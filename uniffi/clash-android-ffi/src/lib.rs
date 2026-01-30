use async_compat::set_runtime_builder;
use clash_lib::app::dns;

use clash_lib::app::dns::config::{DNSListenAddr, DNSNetMode, NameServer};
use clash_lib::config::def::DNSMode;
use clash_lib::{
    Config,
    config::{config::Controller, def::LogLevel},
    start,
};
use once_cell::sync::OnceCell;
use std::{
    net::{IpAddr, Ipv4Addr, SocketAddr},
    sync::Once,
};

use log::init_logger;
use tokio::{sync::broadcast, task::JoinHandle};
use tracing::{error, info};

use clash_lib::config::internal::config::TunConfig;

pub mod controller;
pub mod log;

type EyreError = eyre::Error;
#[uniffi::remote(Object)]
pub struct EyreError;

#[uniffi::export]
pub fn format_eyre_error(err: &EyreError) -> String {
    format!("{:#}", err)
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
    #[uniffi(default = false)]
    pub fake_ip: bool,

    #[uniffi(default = "198.18.0.2/16")]
    pub fake_ip_range: String,

    #[uniffi(default = true)]
    pub ipv6: bool,

    #[uniffi(default = true)]
    pub some_flag: bool,
}



#[unsafe(export_name = "Java_rs_clash_android_MainActivity_javaInit")]
pub extern "system" fn java_init(
    env: jni::JNIEnv,
    _class: jni::objects::JClass,
    _app: jni::objects::JObject,
) {
    let vm = env.get_java_vm().unwrap();
    static VM: OnceCell<jni::JavaVM> = OnceCell::new();
    _ = VM.set(vm);
    let builder = || {
        let mut builder = tokio::runtime::Builder::new_multi_thread();
        builder
            .on_thread_start(|| {
                let vm = VM.get().expect("init java vm");
                vm.attach_current_thread_permanently().unwrap();
            })
            .enable_all();
        builder
    };
    set_runtime_builder(Box::new(builder));
}

#[uniffi::export]
fn verify_config(config_path: &str) -> Result<String, EyreError> {
    let config = Config::File(config_path.to_string()).try_parse()?;
    Ok(format!("{:#?}", config))
}

#[uniffi::export(async_runtime = "tokio")]
async fn init_main(
    config_path: String,
    work_dir: String,
    over: ProfileOverride,
) -> Result<(), EyreError> {
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

    config.general.ipv6 = over.ipv6;

    let nameserver = if config.dns.nameserver.is_empty() {
        vec![
            NameServer {
                net: DNSNetMode::DoH,
                address: "223.5.5.5:443".to_string(),
                interface: None,
                proxy: None,
            },
            NameServer {
                net: DNSNetMode::DoH,
                address: "223.6.6.6:443".to_string(),
                interface: None,
                proxy: None,
            },
            NameServer {
                net: DNSNetMode::DoH,
                address: "120.53.53.53:443".to_string(),
                interface: None,
                proxy: None,
            },
            NameServer {
                net: DNSNetMode::DoH,
                address: "1.12.12.12:443".to_string(),
                interface: None,
                proxy: None,
            },
        ]
    } else {
        config.dns.nameserver.clone()
    };
    let default_nameserver = if config.dns.default_nameserver.is_empty() {
        vec![
            NameServer {
                net: DNSNetMode::Udp,
                address: "223.6.6.6:53".to_string(),
                interface: None,
                proxy: None,
            },
            NameServer {
                net: DNSNetMode::Udp,
                address: "8.8.8.8:53".to_string(),
                interface: None,
                proxy: None,
            },
        ]
    } else {
        config.dns.default_nameserver.clone()
    };

    config.dns = dns::Config {
        enable: true,
        ipv6: over.ipv6,
        listen: DNSListenAddr {
            udp: Some(SocketAddr::new(
                IpAddr::V4(Ipv4Addr::new(127, 0, 0, 1)),
                53553,
            )),
            ..config.dns.listen
        },
        nameserver,
        default_nameserver,
        ..config.dns
    };
    if over.fake_ip {
        config.dns.enhance_mode = DNSMode::FakeIp;
        config.dns.fake_ip_range = over.fake_ip_range.parse()?;
    } else {
        config.dns.enhance_mode = DNSMode::Normal;
    }

    #[cfg(debug_assertions)]
    {
        config.general.log_level = LogLevel::Debug;
    }

    unsafe {
        std::env::set_var("RUST_BACKTRACE", "1");
        // std::env::set_var("NO_COLOR", "1");
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
        color_eyre::install().unwrap();
        tracing::info!("Init logger and panic hook");
    });
    info!(
        "Config path: {config_path}\n\tTUN fd: {}\n\tLog file path: {}",
        over.tun_fd, over.log_file_path
    );

    let _: JoinHandle<eyre::Result<()>> = tokio::spawn(async {
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
    clash_lib::shutdown();
    info!("clashrs shutdown");
}

#[derive(uniffi::Record)]
pub struct DownloadResult {
    pub success: bool,
    pub file_size: u64,
    pub error_message: Option<String>,
}

#[uniffi::export(async_runtime = "tokio")]
async fn download_config_from_url(
    url: String,
    output_path: String,
    user_agent: Option<String>,
    proxy_url: Option<String>,
) -> Result<DownloadResult, EyreError> {
    use http_body_util::BodyExt;
    use std::io::Write;
    use hyper_util::client::legacy::Client;
    use hyper_util::client::legacy::connect::HttpConnector;
    use hyper_util::rt::TokioExecutor;

    info!("Starting download from: {}", url);

    let uri: hyper::Uri = url.parse()
        .map_err(|e| eyre::eyre!("Invalid URL: {}", e))?;
    
    let ua = user_agent.unwrap_or_else(|| "clash-android/1.0".to_string());
    info!("Using User-Agent: {}", ua);

    // Build client with or without proxy
    let response = if let Some(proxy) = proxy_url {
        use hyper_http_proxy::{Proxy, ProxyConnector, Intercept};
        
        info!("Using proxy: {}", proxy);
        let proxy_uri: hyper::Uri = proxy.parse()
            .map_err(|e| eyre::eyre!("Invalid proxy URL: {}", e))?;
        
        let proxy = Proxy::new(Intercept::All, proxy_uri);
        let connector = HttpConnector::new();
        let proxy_connector = ProxyConnector::from_proxy_unsecured(connector, proxy);
        
        let client = Client::builder(TokioExecutor::new()).build(proxy_connector);
        
        // Build request
        let req = hyper::Request::builder()
            .uri(&uri)
            .header("User-Agent", &ua)
            .body(http_body_util::Empty::<bytes::Bytes>::new())
            .map_err(|e| eyre::eyre!("Failed to build request: {}", e))?;
        
        client.request(req)
            .await
            .map_err(|e| eyre::eyre!("Failed to download via proxy: {}", e))?
    } else {
        let client: Client<_, http_body_util::Full<bytes::Bytes>> = 
            Client::builder(TokioExecutor::new()).build_http();
        
        let req = hyper::Request::builder()
            .uri(&uri)
            .header("User-Agent", &ua)
            .body(http_body_util::Full::new(bytes::Bytes::new()))
            .map_err(|e| eyre::eyre!("Failed to build request: {}", e))?;

        client.request(req)
            .await
            .map_err(|e| eyre::eyre!("Failed to download: {}", e))?
    };

    if !response.status().is_success() {
        return Ok(DownloadResult {
            success: false,
            file_size: 0,
            error_message: Some(format!("HTTP error: {}", response.status())),
        });
    }

    // Download response body
    let body = response.into_body();
    let bytes = body
        .collect()
        .await
        .map_err(|e| eyre::eyre!("Failed to read response body: {}", e))?
        .to_bytes();

    // Create output file and write
    let mut file = std::fs::File::create(&output_path)
        .map_err(|e| eyre::eyre!("Failed to create file: {}", e))?;
    
    file.write_all(&bytes)
        .map_err(|e| eyre::eyre!("Failed to write to file: {}", e))?;

    let file_size = bytes.len() as u64;
    info!("Download completed: {} bytes written to {}", file_size, output_path);

    Ok(DownloadResult {
        success: true,
        file_size,
        error_message: None,
    })
}

uniffi::setup_scaffolding!("clash_android_ffi");
