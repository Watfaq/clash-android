#[cfg(target_os = "android")]
#[allow(dead_code)]
pub(crate) fn init_logger() {
    use tracing_subscriber::{filter::LevelFilter, fmt::format::FmtSpan, prelude::*};
    let android_layer = paranoid_android::layer("clash-rs")
        .with_span_events(FmtSpan::CLOSE)
        .with_thread_names(true)
        .with_filter(LevelFilter::DEBUG);

    tracing_subscriber::registry().with(android_layer).init();
}
#[cfg(not(target_os = "android"))]
#[allow(dead_code)]
pub(crate) fn init_logger() {}
