use std::{sync::{LazyLock, Once}, time::Duration};

use tokio::runtime::Runtime;
use tracing::info;

pub mod log;

pub static RT: LazyLock<Runtime> = LazyLock::new(|| {
    let mut builder = tokio::runtime::Builder::new_multi_thread();
    builder.enable_all(); // and others
    let rt = builder.build().unwrap();
    rt.block_on(uniffi::deps::async_compat::Compat::new(async {}));
    rt
});

uniffi::setup_scaffolding!();

#[uniffi::export]
fn init_tokio(){
    tracing::info!("Init tokio");
    LazyLock::force(&RT);
}

#[uniffi::export]
fn init_logger(){
    static INIT: Once = Once::new();
    INIT.call_once(|| {
        log::init_logger();
        tracing::info!("Init logger");
    });
}

#[uniffi::export]
async fn init_main(tun_fd: i32){
    info!("TUN fd: {}", tun_fd);

    RT.spawn(async {
        info!("tokio spawn tasks");
        tokio::time::sleep(Duration::from_secs(5)).await;
    });
}

#[uniffi::export]
fn shutdown(){
    info!("clashrs shutdown");
}

