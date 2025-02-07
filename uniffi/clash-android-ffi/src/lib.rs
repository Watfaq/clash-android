pub mod log;

uniffi::setup_scaffolding!();

#[uniffi::export]
fn init_logger(){
    log::init_logger();
    tracing::info!("Hello World");
}