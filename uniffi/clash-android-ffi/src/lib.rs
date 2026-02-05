#[cfg(feature = "jemallocator")]
#[global_allocator]
static GLOBAL: tikv_jemallocator::Jemalloc = tikv_jemallocator::Jemalloc;

#[cfg(feature = "jemallocator")]
#[allow(non_upper_case_globals)]
#[export_name = "malloc_conf"]
pub static malloc_conf: &[u8] = b"prof:true,prof_active:true,lg_prof_sample:19\0";

use async_compat::set_runtime_builder;
use clash_lib::app::dns;

use clash_lib::app::dns::config::{DNSListenAddr, DNSNetMode, NameServer};
use clash_lib::config::def::{DNSMode, Port};
use clash_lib::{
    Config,
    config::{config::Controller, def::LogLevel},
    start,
};
use once_cell::sync::OnceCell;
use std::path::PathBuf;
use std::{
    net::{IpAddr, Ipv4Addr, SocketAddr},
    sync::Once,
};

use log::init_logger;
use tokio::{sync::broadcast, task::JoinHandle};
use tracing::{error, info};

use clash_lib::config::def::Config as ConfigDef;
use clash_lib::config::internal::config::TunConfig;

pub mod controller;
pub mod log;
pub mod util;

type EyreError = eyre::Error;
#[uniffi::remote(Object)]
pub struct EyreError;

#[uniffi::export]
pub fn format_eyre_error(err: &EyreError) -> String {
    format!("{}", err.to_string())
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
    env: jni::JNIEnv,
    _class: jni::objects::JClass,
    app: jni::objects::JObject,
) {
    
    let vm = env.get_java_vm().unwrap();
    static VM: OnceCell<jni::JavaVM> = OnceCell::new();
    _ = VM.set(vm);
    let builder = || {
        let mut builder = tokio::runtime::Builder::new_multi_thread();
        builder
            .on_thread_start(|| {
                let vm = VM.get().expect("init java vm");
                vm.attach_current_thread_permanently().unwrap();
            })
            .enable_all();
        builder
    };
    set_runtime_builder(Box::new(builder));
    #[cfg(target_os = "android")]
    {
        let mut env_mut = env;
        _ = rustls_platform_verifier::android::init_with_env(&mut env_mut, app);
    }
    #[cfg(not(target_os = "android"))]
    {
        // On non-Android platforms, we don't need to initialize the platform verifier with env
        _ = (env, app);
    }
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

        // Install aws-lc-rs as the default crypto provider
        if let Err(e) = rustls::crypto::aws_lc_rs::default_provider().install_default() {
            error!("Failed to install default crypto provider: {:?}", e);
        } else {
            info!("Successfully installed aws-lc-rs crypto provider");
        }
        info!("Init logger and crypto provider initialized");
    });
}

#[uniffi::export]
fn verify_config(config_path: &str) -> Result<String, EyreError> {
    let config = Config::File(config_path.to_string()).try_parse()?;
    Ok(format!("{:#?}", config))
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
        gateway: ipnet::Ipv4Net::new(Ipv4Addr::new(10, 0, 0, 1), 30)?.into(),
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

    let nameserver = if config.dns.nameserver.is_empty() {
        vec![
            NameServer {
                net: DNSNetMode::DoH,
                address: "223.5.5.5:443".to_string(),
                interface: None,
                proxy: None,
            },
            NameServer {
                net: DNSNetMode::DoH,
                address: "223.6.6.6:443".to_string(),
                interface: None,
                proxy: None,
            },
            NameServer {
                net: DNSNetMode::DoH,
                address: "120.53.53.53:443".to_string(),
                interface: None,
                proxy: None,
            },
            NameServer {
                net: DNSNetMode::DoH,
                address: "1.12.12.12:443".to_string(),
                interface: None,
                proxy: None,
            },
        ]
    } else {
        config.dns.nameserver.clone()
    };
    let default_nameserver = if config.dns.default_nameserver.is_empty() {
        vec![
            NameServer {
                net: DNSNetMode::Udp,
                address: "223.6.6.6:53".to_string(),
                interface: None,
                proxy: None,
            },
            NameServer {
                net: DNSNetMode::Udp,
                address: "8.8.8.8:53".to_string(),
                interface: None,
                proxy: None,
            },
        ]
    } else {
        config.dns.default_nameserver.clone()
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
        ..config.dns
    };
    if over.fake_ip {
        config.dns.enhance_mode = DNSMode::FakeIp;
        config.dns.fake_ip_range = over.fake_ip_range.parse()?;
    } else {
        config.dns.enhance_mode = DNSMode::Normal;
    }

    info!("Config path: {config_path}\n\tTUN fd: {}", over.tun_fd);

    let _: JoinHandle<eyre::Result<()>> = tokio::spawn(async {
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
