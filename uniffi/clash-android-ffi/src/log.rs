use std::fs::OpenOptions;
use tracing_error::ErrorLayer;
use tracing_subscriber::{EnvFilter, fmt::time::LocalTime};
#[allow(unused_imports)]
use tracing_subscriber::{filter::LevelFilter, fmt::format::FmtSpan, prelude::*};

pub(crate) fn init_logger(level: LevelFilter, log_file_path: Option<String>) {
    let filter = EnvFilter::from_default_env()
        .add_directive(format!("clash={}", level).parse().unwrap())
        .add_directive(format!("clash_lib={}", level).parse().unwrap())
        .add_directive(format!("clash_android_ffi={}", level).parse().unwrap())
        .add_directive("warn".parse().unwrap());

    let mut layers = Vec::new();

    if let Some(file_path) = log_file_path {
        let file = OpenOptions::new()
            .create(true)
            .append(true)
            .open(file_path)
            .unwrap_or_else(|e| {
                eprintln!("Failed to open log file: {}", e);
                OpenOptions::new()
                    .write(true)
                    .open(if cfg!(target_os = "windows") {
                        "NUL"
                    } else {
                        "/dev/null"
                    })
                    .unwrap()
            });

        let file_layer = tracing_subscriber::fmt::layer()
            .with_writer(std::sync::Mutex::new(file))
            .with_ansi(false)
            .with_thread_names(true)
            .with_span_events(FmtSpan::CLOSE)
            .with_timer(LocalTime::new(time::macros::format_description!(
                "[year repr:last_two]-[month]-[day] [hour]:[minute]:[second]"
            )))
            .boxed();

        layers.push(file_layer);
    }

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
