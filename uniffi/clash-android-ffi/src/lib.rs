use clash_lib::app::dns;

use clash_lib::app::dns::config::{DNSListenAddr, DNSNetMode, NameServer};
use clash_lib::{
    Config,
    config::{config::Controller, def::LogLevel},
    start,
};
use std::{
    net::{IpAddr, Ipv4Addr, SocketAddr},
    sync::{LazyLock, Once, OnceLock},
};

use log::init_logger;
use tokio::{runtime::Runtime, sync::broadcast, task::JoinHandle};
use tracing::{error, info};

use clash_lib::config::internal::config::TunConfig;

pub mod controller;
pub mod log;

pub static RT: LazyLock<Runtime> = LazyLock::new(|| {
    let mut builder = tokio::runtime::Builder::new_multi_thread();
    let rt = builder
        .on_thread_start(|| {
            let vm = VM.get().expect("init java vm");
            vm.attach_current_thread_permanently().unwrap();
        })
        .enable_all()
        .build()
        .unwrap();
    rt.block_on(uniffi::deps::async_compat::Compat::new(async {}));
    rt
});

static VM: OnceLock<jni::JavaVM> = OnceLock::new();

#[unsafe(export_name = "Java_rs_clash_android_ffi_JNI_setup")]
pub extern "system" fn setup_tokio(env: jni::JNIEnv, _class: jni::objects::JClass) {
    let vm = env.get_java_vm().unwrap();
    _ = VM.set(vm);
    LazyLock::force(&RT);
}

#[derive(uniffi::Record)]
pub struct ProfileOverride {
    pub tun_fd: i32,
    pub log_file_path: String,

    #[uniffi(default = false)]
    pub allow_lan: bool,

    #[uniffi(default = 7890)]
    pub mixed_port: u16,
    #[uniffi(default = 7891)]
    pub http_port: u16,
    #[uniffi(default = 7892)]
    pub socks_port: u16,

    #[uniffi(default = true)]
    pub some_flag: bool,
}

#[derive(uniffi::Error, Debug)]
#[uniffi(flat_error)]
pub enum FfiError {
    Common(eyre::Report),
}

impl<E> From<E> for FfiError
where
    E: std::error::Error + Send + Sync + 'static,
{
    fn from(value: E) -> Self {
        Self::Common(eyre::Report::new(value))
    }
}

impl std::fmt::Display for FfiError {
    fn fmt(&self, f: &mut std::fmt::Formatter<'_>) -> std::fmt::Result {
        match self {
            FfiError::Common(report) => report.fmt(f),
        }
    }
}

#[uniffi::export(async_runtime = "tokio")]
async fn init_main(
    config_path: String,
    work_dir: String,
    over: ProfileOverride,
) -> Result<(), FfiError> {
    std::env::set_current_dir(&work_dir)?;
    let mut config = Config::File(config_path.clone()).try_parse()?;
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

    let nameserver = if config.dns.nameserver.is_empty() {
        vec![NameServer {
            net: DNSNetMode::DoT,
            address: "dns.alidns.com:853".to_string(),
            interface: None,
            proxy: None,
        }]
    } else {
        config.dns.nameserver.clone()
    };
    config.dns = dns::Config {
        enable: true,

        listen: DNSListenAddr {
            udp: Some(SocketAddr::new(
                IpAddr::V4(Ipv4Addr::new(10, 0, 0, 2)),
                53553,
            )),

            ..config.dns.listen
        },
        nameserver,

        ..config.dns
    };

    #[cfg(debug_assertions)]
    {
        config.general.log_level = LogLevel::Debug;
    }

    unsafe {
        std::env::set_var("RUST_BACKTRACE", "1");
        std::env::set_var("NO_COLOR", "1");
    }
    static INIT: Once = Once::new();
    INIT.call_once(|| {
        // Setup panic hook to capture panics on Android
        std::panic::set_hook(Box::new(|panic_info| {
            let payload = panic_info.payload();
            let message = if let Some(s) = payload.downcast_ref::<&str>() {
                s.to_string()
            } else if let Some(s) = payload.downcast_ref::<String>() {
                s.clone()
            } else {
                "Unknown panic payload".to_string()
            };

            let location = if let Some(loc) = panic_info.location() {
                format!("{}:{}:{}", loc.file(), loc.line(), loc.column())
            } else {
                "Unknown location".to_string()
            };

            error!("PANIC caught: {} at {}", message, location);
            error!("Backtrace:\n{}", std::backtrace::Backtrace::force_capture());
        }));

        init_logger(config.general.log_level.into(), None);
        // color_eyre::install().unwrap();
        tracing::info!("Init logger and panic hook");
    });
    info!(
        "Config path: {config_path}\n\tTUN fd: {}\n\tLog file path: {}",
        over.tun_fd, over.log_file_path
    );

    let _: JoinHandle<eyre::Result<()>> = RT.spawn(async {
        let (log_tx, _) = broadcast::channel(100);
        info!("Starting clash-rs");
        if let Err(err) = start(config, work_dir, log_tx).await {
            error!("clash-rs start error: {:#}", eyre::eyre!(err));
        }

        info!("Quitting clash-rs");
        Ok(())
    });
    Ok(())
}

#[uniffi::export]
fn shutdown() {
    info!("clashrs shutdown");
}
uniffi::setup_scaffolding!("clash_android_ffi");
