use tracing_error::ErrorLayer;
use tracing_subscriber::EnvFilter;
#[allow(unused_imports)]
use tracing_subscriber::{filter::LevelFilter, fmt::format::FmtSpan, prelude::*};

pub(crate) fn init_logger(level: LevelFilter) {
    let filter = EnvFilter::from_default_env()
        .add_directive(format!("clash={}", level).parse().unwrap())
        .add_directive(format!("clash_lib={}", level).parse().unwrap())
        .add_directive(format!("clash_android_ffi={}", level).parse().unwrap())
        .add_directive("warn".parse().unwrap());

    let layers: Vec<Box<dyn tracing_subscriber::Layer<_> + Send + Sync + 'static>> = Vec::new();


    #[cfg(target_os = "android")]
    {
        let android_layer = paranoid_android::layer("clash-rs")
            .with_ansi(false)
            .with_span_events(FmtSpan::CLOSE)
            .with_thread_names(true)
            .without_time()
            .with_filter(LevelFilter::TRACE)
            .boxed();

        layers.push(android_layer);
    }

    tracing_subscriber::registry()
        .with(filter)
        .with(layers)
        .with(ErrorLayer::default())
        .init();
}
