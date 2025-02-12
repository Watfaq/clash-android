use std::{
    sync::{LazyLock, Once, OnceLock},
    time::Duration,
};

use clash_lib::Config;
use tokio::{runtime::Runtime, task::JoinHandle};
use tracing::{info, trace};

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

uniffi::setup_scaffolding!();

static VM: OnceLock<jni::JavaVM> = OnceLock::new();

#[uniffi::export]
fn init_logger() {
    std::env::set_var("NO_COLOR", "1");
    static INIT: Once = Once::new();
    INIT.call_once(|| {
        log::init_logger();
        tracing::info!("Init logger");
    });
}

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

#[uniffi::export]
async fn init_main(config_path: String, over: ProfileOverride) -> Result<(), FfiError> {
    info!(
        "Config path: {config_path}\n\tTUN fd: {}\n\tLog file path: {}",
        over.tun_fd, over.log_file_path
    );
    let config = Config::File(config_path).try_parse()?;
    config.tun = TunConfig {
        enable: todo!(),
        device_id: todo!(),
        route_all: todo!(),
        routes: todo!(),
        gateway: todo!(),
        mtu: todo!(),
        so_mark: todo!(),
        route_table: todo!(),
        dns_hijack: todo!(),
    };
    let _: JoinHandle<eyre::Result<()>> = RT.spawn(async { Ok(()) });
    Ok(())
}

#[uniffi::export]
fn shutdown() {
    info!("clashrs shutdown");
}
