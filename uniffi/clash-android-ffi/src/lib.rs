use tracing::info;

pub mod log;

uniffi::setup_scaffolding!();

#[uniffi::export]
fn init_main(tun_fd: i32){
    info!("TUN fd: {}", tun_fd);
}


#[uniffi::export]
fn init_logger(){
    log::init_logger();
    tracing::info!("Hello World");
}