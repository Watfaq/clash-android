#[cfg(feature = "jemallocator")]
#[global_allocator]
static GLOBAL: tikv_jemallocator::Jemalloc = tikv_jemallocator::Jemalloc;

#[cfg(feature = "mimalloc")]
#[global_allocator]
static GLOBAL: mimalloc::MiMalloc = mimalloc::MiMalloc;

#[cfg(feature = "mimalloc")]
unsafe extern "C" {
    /// https://microsoft.github.io/mimalloc/group__extended.html#ga1ea89844e2494f81b30d49b4cec80fb2
    fn mi_process_info(
        elapsed_msecs: *mut usize,
        user_msecs: *mut usize,
        system_msecs: *mut usize,
        current_rss: *mut usize,
        peak_rss: *mut usize,
        current_commit: *mut usize,
        peak_commit: *mut usize,
        page_faults: *mut usize,
    );
}

#[cfg(feature = "mimalloc")]
#[derive(uniffi::Record)]
pub struct MimallocStats {
    pub elapsed_msecs: u64,
    pub user_msecs: u64,
    pub system_msecs: u64,
    /// Current resident set size (bytes)
    pub current_rss: u64,
    /// Peak resident set size (bytes)
    pub peak_rss: u64,
    /// Current committed memory (bytes)
    pub current_commit: u64,
    /// Peak committed memory (bytes)
    pub peak_commit: u64,
    pub page_faults: u64,
}

#[cfg(feature = "mimalloc")]
#[uniffi::export]
pub fn get_mimalloc_stats() -> MimallocStats {
    let mut elapsed_msecs: usize = 0;
    let mut user_msecs: usize = 0;
    let mut system_msecs: usize = 0;
    let mut current_rss: usize = 0;
    let mut peak_rss: usize = 0;
    let mut current_commit: usize = 0;
    let mut peak_commit: usize = 0;
    let mut page_faults: usize = 0;
    unsafe {
        mi_process_info(
            &raw mut elapsed_msecs,
            &raw mut user_msecs,
            &raw mut system_msecs,
            &raw mut current_rss,
            &raw mut peak_rss,
            &raw mut current_commit,
            &raw mut peak_commit,
            &raw mut page_faults,
        );
    }
    MimallocStats {
        elapsed_msecs: elapsed_msecs as u64,
        user_msecs: user_msecs as u64,
        system_msecs: system_msecs as u64,
        current_rss: current_rss as u64,
        peak_rss: peak_rss as u64,
        current_commit: current_commit as u64,
        peak_commit: peak_commit as u64,
        page_faults: page_faults as u64,
    }
}

use std::net::{IpAddr, Ipv4Addr, SocketAddr};
use std::path::PathBuf;
use std::sync::Once;

use async_compat::set_runtime_builder;
use clash_lib::{
    Config,
    app::{
        dns,
        dns::config::{DNSListenAddr, DNSNetMode, NameServer},
    },
    config::{
        config::Controller,
        def::{Config as ConfigDef, DNSMode, LogLevel, Port},
        internal::config::TunConfig,
    },
    start,
};
use log::init_logger;
use once_cell::sync::OnceCell;
use tokio::{sync::broadcast, task::JoinHandle};
use tracing::{error, info};
use url::Host;

pub mod controller;
pub mod log;
pub mod util;

type EyreError = eyre::Error;
#[uniffi::remote(Object)]
pub struct EyreError;

#[uniffi::export]
pub fn format_eyre_error(err: &EyreError) -> String {
    err.to_string()
}

#[derive(uniffi::Record)]
pub struct ProfileOverride {
    pub tun_fd: i32,

    #[uniffi(default = false)]
    pub allow_lan: bool,

    #[uniffi(default = 7890)]
    pub mixed_port: u16,
    #[uniffi(default = None)]
    pub http_port: Option<u16>,
    #[uniffi(default = None)]
    pub socks_port: Option<u16>,
    #[uniffi(default = false)]
    pub fake_ip: bool,

    #[uniffi(default = "198.18.0.2/16")]
    pub fake_ip_range: String,

    #[uniffi(default = true)]
    pub ipv6: bool,
}

#[derive(uniffi::Record, Default)]
pub struct FinalProfile {
    #[uniffi(default = 7890)]
    pub mixed_port: u16,
}

#[unsafe(export_name = "Java_rs_clash_android_MainActivity_javaInit")]
pub extern "system" fn java_init(
    mut env: jni::EnvUnowned,
    _class: jni::objects::JClass,
    context: jni::objects::JObject,
) {
    use jni::{Outcome, errors::Result as JniResult};

    let vm: jni::JavaVM = {
        let outcome = env
            .with_env(|env| -> jni::errors::Result<_> { Ok(env.get_java_vm()?.clone()) })
            .into_outcome();
        match outcome {
            Outcome::Ok(v) => v,
            Outcome::Err(e) => panic!("JNI error: {}", e),
            Outcome::Panic(p) => std::panic::resume_unwind(p),
        }
    };
    static VM: OnceCell<jni::JavaVM> = OnceCell::new();
    _ = VM.set(vm);
    let builder = || {
        let mut builder = tokio::runtime::Builder::new_multi_thread();
        builder
            .on_thread_start(|| {
                let vm = VM.get().expect("init java vm");
                let _ = vm.attach_current_thread(|_| -> JniResult<()> { Ok(()) });
            })
            .enable_all();
        builder
    };
    set_runtime_builder(Box::new(builder));

    static INIT: Once = Once::new();
    INIT.call_once(|| {
        let level = if cfg!(debug_assertions) {
            LogLevel::Debug
        } else {
            LogLevel::Info
        };

        unsafe {
            std::env::set_var("RUST_BACKTRACE", "1");
            // std::env::set_var("NO_COLOR", "1");
        }
        init_logger(level.into());
        color_eyre::install().unwrap();

        // Re-route panic backtraces through `tracing` so they reach
        // logcat. color_eyre's default hook prints to stderr, which on
        // Android is /dev/null — without this the tombstone only shows
        // the abort message and raw `pc` offsets.
        let prev = std::panic::take_hook();
        std::panic::set_hook(Box::new(move |info| {
            let bt = std::backtrace::Backtrace::force_capture();
            error!(target: "panic", "thread panicked: {info}\n{bt}");
            prev(info);
        }));

        // Install aws-lc-rs as the default crypto provider
        if let Err(e) = rustls::crypto::aws_lc_rs::default_provider().install_default() {
            error!("Failed to install default crypto provider: {:?}", e);
        } else {
            info!("Successfully installed aws-lc-rs crypto provider");
        }
        info!("Init logger and crypto provider initialized");
    });

    #[cfg(target_os = "android")]
    {
        use jni::Outcome;
        match env
            .with_env(|env| -> Result<(), jni::errors::Error> {
                rustls_platform_verifier::android::init_with_env(env, context)?;
                Ok(())
            })
            .into_outcome()
        {
            Outcome::Ok(_) => info!("Initialized rustls_platform_verifier"),
            Outcome::Err(e) => error!("rustls_platform_verifier init JNI error: {e:?}"),
            Outcome::Panic(p) => std::panic::resume_unwind(p),
        };
    }
}

#[uniffi::export]
fn verify_config(config_path: &str) -> Result<String, EyreError> {
    let _config = Config::File(config_path.to_string()).try_parse()?;
    Ok("config verified successfully".to_string())
}

#[uniffi::export(async_runtime = "tokio")]
async fn run_clash(
    config_path: String,
    work_dir: String,
    over: ProfileOverride,
) -> Result<FinalProfile, EyreError> {
    std::env::set_current_dir(&work_dir)?;
    let mut final_profile = FinalProfile::default();
    let mut config_def = ConfigDef::try_from(PathBuf::from(config_path.clone()))?;
    final_profile.mixed_port = config_def.mixed_port.get_or_insert(Port(over.mixed_port)).0;
    config_def.port = config_def.port.or_else(|| over.http_port.map(Port));
    config_def.socks_port = config_def.socks_port.or_else(|| over.socks_port.map(Port));

    let mut config = Config::Def(config_def).try_parse()?;
    config.tun = TunConfig {
        enable: true,
        device_id: format!("fd://{}", over.tun_fd),
        route_all: false,
        routes: Vec::new(),
        gateway: ipnet::Ipv4Net::new(Ipv4Addr::new(10, 0, 0, 1), 30)?,
        gateway_v6: None,
        mtu: None,
        so_mark: None,
        route_table: 0,
        dns_hijack: true,
    };

    config.general.geosite = Some("geosite.dat".to_string());
    config.general.mmdb = Some("Country.mmdb".to_string());
    config.general.asn_mmdb = None;

    config.general.controller = Controller {
        external_controller_ipc: Some(format!("{work_dir}/clash.sock")),
        ..Default::default()
    };

    config.general.ipv6 = over.ipv6;
    let default_nameserver = if config.dns.default_nameserver.is_empty() {
        vec![
            NameServer {
                net: DNSNetMode::Udp,
                host: Host::Domain("223.5.5.5".to_string()),
                port: 53,
                interface: None,
                proxy: None,
            },
            NameServer {
                net: DNSNetMode::Udp,
                host: Host::Domain("223.6.6.6".to_string()),
                port: 53,
                interface: None,
                proxy: None,
            },
            NameServer {
                net: DNSNetMode::Udp,
                host: Host::Domain("8.8.8.8".to_string()),
                port: 53,
                interface: None,
                proxy: None,
            },
        ]
    } else {
        config.dns.default_nameserver.clone()
    };
    // 需要 clash-rs 实现 respect-rules
    // let proxy_server_nameserver = if config.dns.proxy_server_nameserver.is_none() {
    //     vec![
    //         NameServer {
    //             net: DNSNetMode::DoT,
    //             host: Host::Domain("dns.alidns.com".to_string()),
    //             port: 853,
    //             interface: None,
    //             proxy: None,
    //         },
    //         NameServer {
    //             net: DNSNetMode::DoT,
    //             host: Host::Domain("dot.pub".to_string()),
    //             port: 853,
    //             interface: None,
    //             proxy: None,
    //         },
    //     ]
    // } else {
    //     config
    //         .dns
    //         .proxy_server_nameserver
    //         .clone()
    //         .unwrap_or_default()
    // };
    // 需要 clash-rs 实现 dns 路由
    // let nameserver = if config.dns.nameserver.is_empty() {
    //     vec![
    //         NameServer {
    //             net: DNSNetMode::DoT,
    //             host: Host::Domain("one.one.one.one".to_string()),
    //             port: 853,
    //             interface: None,
    //             proxy: None,
    //         },
    //         NameServer {
    //             net: DNSNetMode::DoT,
    //             host: Host::Domain("dns.google".to_string()),
    //             port: 853,
    //             interface: None,
    //             proxy: None,
    //         },
    //     ]
    // } else {
    //     config.dns.nameserver.clone()
    // };

    let nameserver = if config.dns.nameserver.is_empty() {
        vec![
            NameServer {
                net: DNSNetMode::DoT,
                host: Host::Domain("dns.alidns.com".to_string()),
                port: 853,
                interface: None,
                proxy: None,
            },
            NameServer {
                net: DNSNetMode::DoT,
                host: Host::Domain("dot.pub".to_string()),
                port: 853,
                interface: None,
                proxy: None,
            },
        ]
    } else {
        config.dns.nameserver.clone()
    };

    config.dns = dns::Config {
        enable: true,
        ipv6: over.ipv6,
        listen: DNSListenAddr {
            udp: Some(SocketAddr::new(
                IpAddr::V4(Ipv4Addr::new(127, 0, 0, 1)),
                53553,
            )),
            ..config.dns.listen
        },
        nameserver,
        default_nameserver,
        // proxy_server_nameserver: Some(proxy_server_nameserver),
        ..config.dns
    };
    if over.fake_ip {
        config.dns.enhance_mode = DNSMode::FakeIp;
        config.dns.fake_ip_range = over.fake_ip_range.parse()?;
        config.dns.store_fake_ip = true;
    } else {
        config.dns.enhance_mode = DNSMode::Normal;
    }

    info!("Config path: {config_path}\n\tTUN fd: {}", over.tun_fd);

    let _handle: JoinHandle<eyre::Result<()>> = tokio::spawn(async {
        let (log_tx, _) = broadcast::channel(100);
        info!("Starting clash-rs");
        if let Err(err) = start(config, work_dir, log_tx).await {
            error!("clash-rs start error: {:#}", eyre::eyre!(err));
        }

        info!("Quitting clash-rs");
        Ok(())
    });
    Ok(final_profile)
}

#[uniffi::export]
fn shutdown() {
    clash_lib::shutdown();
    info!("clashrs shutdown");
}

uniffi::setup_scaffolding!("clash_android_ffi");
