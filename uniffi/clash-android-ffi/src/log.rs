#[allow(unused_imports)]
use tracing_subscriber::{filter::LevelFilter, fmt::format::FmtSpan, prelude::*};
use tracing_subscriber::EnvFilter;

pub(crate) fn init_logger(level: LevelFilter) {
    let filter = EnvFilter::from_default_env()
        .add_directive(format!("clash={}", level).parse().unwrap())
        .add_directive(format!("clash_lib={}", level).parse().unwrap())
        .add_directive(format!("clash_android_ffi={}", level).parse().unwrap())
        .add_directive("trace".parse().unwrap());

    let registry = tracing_subscriber::registry().with(filter);

    #[cfg(target_os = "android")]
    let registry = registry.with(
        paranoid_android::layer("clash-rs")
            .with_ansi(false)
            .with_span_events(FmtSpan::CLOSE)
            .with_thread_names(true)
            .without_time()
            .with_filter(LevelFilter::TRACE),
    );
    registry.init();
}
