use std::{
    net::{IpAddr, Ipv4Addr, SocketAddr},
    sync::{Arc, LazyLock, Once, OnceLock},
};

use clash_lib::{
    start, Config,
};

use log::init_logger;
use tokio::{runtime::Runtime, sync::broadcast, task::JoinHandle};
use tracing::{error, info};

use clash_lib::config::internal::config::TunConfig;

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

#[export_name = "Java_rs_clash_android_ffi_JNI_setup"]
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
pub trait SocketProtector: Send + Sync {
    fn protect(&self, fd: i32);
}

#[uniffi::export]
async fn init_main(
    config_path: String,
    work_dir: String,
    over: ProfileOverride,
    socket_protector: Arc<dyn SocketProtector>,
) -> Result<(), FfiError> {
    let mut config = Config::File(config_path.clone()).try_parse()?;
    config.tun = TunConfig {
        enable: true,
        device_id: format!("fd://{}", over.tun_fd),
        route_all: false,
        routes: Vec::new(),
        gateway: ipnet::Ipv4Net::new(Ipv4Addr::new(10, 0, 0, 1), 30)?.into(),
        gateway_v6: None,
        mtu: None,
        so_mark: 0,
        route_table: 0,
        dns_hijack: true,
    };
    // Note: DNS and general config would need to be updated based on the actual API
    // For now, keeping minimal changes to allow compilation
    std::env::set_var("RUST_BACKTRACE", "1");
    std::env::set_var("NO_COLOR", "1");
    static INIT: Once = Once::new();
    INIT.call_once(|| {
        init_logger(config.general.log_level.into());
        // color_eyre::install().unwrap();
        tracing::info!("Init logger");
    });
    info!(
        "Config path: {config_path}\n\tTUN fd: {}\n\tLog file path: {}",
        over.tun_fd, over.log_file_path
    );

    // Socket protector functionality removed for now due to API changes
    // TODO: Re-implement socket protector based on new API

    let _: JoinHandle<eyre::Result<()>> = RT.spawn(async {
        let (log_tx, _) = broadcast::channel(100);
        info!("Starting clash-rs");
        // start(config, work_dir, log_tx).await.unwrap();
        if let Err(err) = start(config, work_dir, log_tx).await {
            error!("{:#}", eyre::eyre!(err));
        }
        Ok(())
    });
    Ok(())
}

#[uniffi::export]
fn shutdown() {
    info!("clashrs shutdown");
}

uniffi::include_scaffolding!("clash");
