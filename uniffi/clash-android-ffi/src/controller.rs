use eyre::Context;
use http_body_util::{BodyExt, Full};
use hyper::Request;
use hyper::body::Bytes;
use hyper_util::client::legacy::Client;
use hyper_util::rt::TokioExecutor;
use serde::{Deserialize, Serialize};
use std::collections::HashMap;
use std::sync::Arc;
use urlencoding::encode;

#[cfg(unix)]
use hyperlocal::{UnixConnector, Uri as UnixUri};

use crate::EyreError;

#[derive(Debug, Clone, Copy, Serialize, Deserialize, uniffi::Enum)]
#[serde(rename_all = "lowercase")]
pub enum Mode {
    Rule,
    Global,
    Direct,
}

#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
pub struct Proxy {
    /// Proxy name
    pub name: String,
    /// Proxy type (e.g., Selector, URLTest, Fallback, Direct, Reject, etc.)
    #[serde(rename = "type")]
    pub proxy_type: String,
    /// All proxy node names contained in the proxy group (only for proxy groups)
    #[serde(default)]
    pub all: Vec<String>,
    /// Currently selected proxy node name (only for proxy groups)
    pub now: Option<String>,
    /// Delay test history records
    #[serde(default)]
    pub history: Vec<DelayHistory>,
}

#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
pub struct DelayHistory {
    pub time: String,
    pub delay: i32,
}

#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
pub struct DelayResponse {
    pub delay: i32,
}

#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
pub struct MemoryResponse {
    pub inuse: i64,
    pub oslimit: i64,
}

#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
pub struct Connection {
    pub id: String,
    pub metadata: Metadata,
    pub upload: i64,
    pub download: i64,
    pub start: String,
    pub chains: Vec<String>,
    pub rule: String,
}

#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
pub struct Metadata {
    pub network: String,
    #[serde(rename = "type")]
    pub metadata_type: String,
    #[serde(rename = "sourceIP")]
    pub source_ip: String,
    #[serde(rename = "destinationIP")]
    pub destination_ip: String,
    #[serde(rename = "destinationPort")]
    pub destination_port: u16,
    pub host: String,
}

#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
pub struct ConnectionsResponse {
    #[serde(rename = "downloadTotal")]
    pub download_total: i64,
    #[serde(rename = "uploadTotal")]
    pub upload_total: i64,
    pub connections: Vec<Connection>,
}

#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
pub struct ConfigResponse {
    #[serde(rename = "external-controller")]
    pub external_controller: Option<String>,
    pub secret: Option<String>,
    pub mode: Option<Mode>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
struct ProxiesResponse {
    pub proxies: HashMap<String, Proxy>,
}

/// Clash HTTP API client using Unix domain socket
#[derive(uniffi::Object)]
pub struct ClashController {
    socket_path: String,
}

#[uniffi::export(async_runtime = "tokio")]
impl ClashController {
    /// Create a new HTTP client that connects via Unix domain socket
    #[uniffi::constructor]
    pub fn new(socket_path: String) -> Arc<Self> {
        Arc::new(Self { socket_path })
    }

    /// Get all proxies
    pub async fn get_proxies(&self) -> Result<Vec<Proxy>, EyreError> {
        let mode = self.get_mode().await?.unwrap_or(Mode::Rule);

        // If in direct mode, return a single DIRECT proxy
        if matches!(mode, Mode::Direct) {
            return Ok(vec![Proxy {
                name: "DIRECT".to_string(),
                proxy_type: "Direct".to_string(),
                all: Vec::new(),
                now: None,
                history: Vec::new(),
            }]);
        }

        let mut response: ProxiesResponse = self.request("GET", "/proxies", None).await?;

        // Get the order from GLOBAL proxy group's 'all' field
        if let Some(global_group) = response.proxies.remove("GLOBAL") {
            let mut sorted_proxies = Vec::new();

            // First add proxies in GLOBAL's 'all' order
            for name in &global_group.all {
                if let Some(proxy) = response.proxies.get(name) {
                    sorted_proxies.push(proxy.clone());
                }
            }

            // Then add any remaining proxies not in GLOBAL's 'all'
            for (name, proxy) in &response.proxies {
                if !global_group.all.contains(name) {
                    sorted_proxies.push(proxy.clone());
                }
            }

            // Add GLOBAL group at the front when GLOBAL mode is active
            if matches!(mode, Mode::Global) {
                sorted_proxies.insert(0, global_group);
            }

            Ok(sorted_proxies)
        } else {
            // If no GLOBAL group, return proxies as a vec
            Ok(response.proxies.values().cloned().collect())
        }
    }

    /// Select a proxy for a group
    pub async fn select_proxy(
        &self,
        group_name: String,
        proxy_name: String,
    ) -> Result<(), EyreError> {
        let body = serde_json::json!(
            {
                "name": proxy_name
            }
        );

        let path = format!("/proxies/{}", encode(&group_name));
        self.request_no_response("PUT", &path, Some(serde_json::to_vec(&body)?))
            .await
    }

    /// Get proxy delay
    pub async fn get_proxy_delay(
        &self,
        name: String,
        url: Option<String>,
        timeout: Option<i32>,
    ) -> Result<DelayResponse, EyreError> {
        let test_url = url.unwrap_or_else(|| "http://www.gstatic.com/generate_204".to_string());
        let timeout_ms = timeout.unwrap_or(5000);

        let path = format!(
            "/proxies/{}/delay?url={}&timeout={}",
            encode(&name),
            encode(&test_url),
            timeout_ms
        );
        self.request("GET", &path, None).await
    }

    /// Get memory statistics
    pub async fn get_memory(&self) -> Result<MemoryResponse, EyreError> {
        #[cfg(feature = "jemallocator")]
        {
            use jemalloc_ctl::{epoch, stats};
            // Advance the epoch to update statistics
            epoch::advance()
                .map_err(|e| eyre::eyre!("Failed to advance jemalloc epoch: {}", e))?;
            
            // Read allocated memory in bytes
            let allocated = stats::allocated::read()
                .map_err(|e| eyre::eyre!("Failed to read allocated memory: {}", e))?;
            
            // Read resident memory in bytes
            let resident = stats::resident::read()
                .map_err(|e| eyre::eyre!("Failed to read resident memory: {}", e))?;
            
            Ok(MemoryResponse {
                inuse: allocated as i64,
                oslimit: resident as i64,
            })
        }
        
        #[cfg(not(feature = "jemallocator"))]
        {
            self.request("GET", "/memory", None).await
        }
    }

    /// Get active connections
    pub async fn get_connections(&self) -> Result<ConnectionsResponse, EyreError> {
        self.request("GET", "/connections", None).await
    }

    /// Get current configuration
    pub async fn get_configs(&self) -> Result<ConfigResponse, EyreError> {
        self.request("GET", "/configs", None).await
    }

    /// Update configuration
    pub async fn update_config(&self, config: HashMap<String, String>) -> Result<(), EyreError> {
        let body_bytes = serde_json::to_vec(&config).wrap_err("Failed to serialize config")?;

        self.request_no_response("PATCH", "/configs", Some(body_bytes))
            .await
    }

    /// Set proxy mode (rule, global, direct)
    pub async fn set_mode(&self, mode: Mode) -> Result<(), EyreError> {
        let mode_str = match mode {
            Mode::Rule => "rule",
            Mode::Global => "global",
            Mode::Direct => "direct",
        };
        let mut config = HashMap::new();
        config.insert("mode".to_string(), mode_str.to_string());
        self.update_config(config).await
    }

    /// Get current proxy mode
    pub async fn get_mode(&self) -> Result<Option<Mode>, EyreError> {
        let config = self.get_configs().await?;
        Ok(config.mode)
    }

    /// Dump memory profiling flamegraph to file
    pub async fn dump(&self, file_path: String) -> Result<(), EyreError> {
        #[cfg(feature = "jemallocator")]
        {
            let mut prof_ctl = jemalloc_pprof::PROF_CTL
                .as_ref()
                .ok_or_else(|| eyre::eyre!("Profiling not initialized"))?
                .lock()
                .await;
            
            if !prof_ctl.activated() {
                return Err(eyre::eyre!("Profiling is not activated").into());
            }
            
            let svg = prof_ctl
                .dump_flamegraph()
                .map_err(|e| eyre::eyre!("Failed to dump flamegraph: {}", e))?;
            
            tokio::fs::write(&file_path, svg).await
                .wrap_err_with(|| format!("Failed to write flamegraph to {}", file_path))?;
        }
        
        #[cfg(not(feature = "jemallocator"))]
        {
            _ = file_path;  // Suppress unused warning
            Err(eyre::eyre!("clash-rs is not compiled with jemallocator feature").into())
        }
    }
}

impl ClashController {
    async fn request_no_response(
        &self,
        method: &str,
        path: &str,
        body: Option<Vec<u8>>,
    ) -> Result<(), EyreError> {
        let client = Client::builder(TokioExecutor::new()).build(UnixConnector);
        let uri: hyper::Uri = UnixUri::new(&self.socket_path, path).into();

        let request_builder = Request::builder()
            .uri(uri)
            .method(method)
            .header("Content-Type", "application/json");

        let request = if let Some(body_data) = body {
            request_builder
                .body(Full::new(Bytes::from(body_data)))
                .wrap_err("Failed to build request with body")?
        } else {
            request_builder
                .body(Full::new(Bytes::new()))
                .wrap_err("Failed to build request")?
        };

        let response = client
            .request(request)
            .await
            .wrap_err("HTTP request failed")?;

        if !response.status().is_success() {
            return Err(eyre::eyre!("HTTP status error: {}", response.status()).into());
        }

        Ok(())
    }
    async fn request<T>(
        &self,
        method: &str,
        path: &str,
        body: Option<Vec<u8>>,
    ) -> Result<T, EyreError>
    where
        T: serde::de::DeserializeOwned,
    {
        let uri: hyper::Uri = UnixUri::new(&self.socket_path, path).into();
        let client = Client::builder(TokioExecutor::new()).build(UnixConnector);

        let request_builder = Request::builder()
            .uri(uri)
            .method(method)
            .header("Content-Type", "application/json");

        let request = if let Some(body_data) = body {
            request_builder
                .body(Full::new(Bytes::from(body_data)))
                .wrap_err("Failed to build request with body")?
        } else {
            request_builder
                .body(Full::new(Bytes::new()))
                .wrap_err("Failed to build request")?
        };

        let response = client
            .request(request)
            .await
            .wrap_err("HTTP request failed")?;

        if !response.status().is_success() {
            return Err(eyre::eyre!("HTTP status error: {}", response.status()).into());
        }

        let body_bytes = response
            .into_body()
            .collect()
            .await
            .wrap_err("Failed to read response body")?
            .to_bytes();

        serde_json::from_slice(&body_bytes)
            .wrap_err_with(|| {
                format!(
                    "Failed to parse JSON response: {}",
                    String::from_utf8_lossy(&body_bytes)
                )
            })
            .map_err(Into::into)
    }
}
