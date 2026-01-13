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

#[derive(Debug, Clone, Serialize, Deserialize, uniffi::Record)]
pub struct Proxy {
    pub name: String,
    #[serde(rename = "type")]
    pub proxy_type: String,
    #[serde(default)]
    pub all: Vec<String>,
    pub now: Option<String>,
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
    pub mode: Option<String>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
struct ProxiesResponse {
    pub proxies: HashMap<String, Proxy>,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
struct ProxySelect {
    pub name: String,
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
    pub async fn get_proxies(&self) -> Result<HashMap<String, Proxy>, HttpError> {
        let response: ProxiesResponse = self.request("GET", "/proxies", None).await?;
        Ok(response.proxies)
    }

    /// Select a proxy for a group
    pub async fn select_proxy(
        &self,
        group_name: String,
        proxy_name: String,
    ) -> Result<(), HttpError> {
        let body = ProxySelect { name: proxy_name };
        let body_bytes =
            serde_json::to_vec(&body).map_err(|e| HttpError::ClientError(e.to_string()))?;

        let path = format!("/proxies/{}", encode(&group_name));
        self.request_no_response("PUT", &path, Some(body_bytes))
            .await
    }

    /// Get proxy delay
    pub async fn get_proxy_delay(
        &self,
        name: String,
        url: Option<String>,
        timeout: Option<i32>,
    ) -> Result<DelayResponse, HttpError> {
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
    pub async fn get_memory(&self) -> Result<MemoryResponse, HttpError> {
        self.request("GET", "/memory", None).await
    }

    /// Get active connections
    pub async fn get_connections(&self) -> Result<ConnectionsResponse, HttpError> {
        self.request("GET", "/connections", None).await
    }

    /// Get current configuration
    pub async fn get_configs(&self) -> Result<ConfigResponse, HttpError> {
        self.request("GET", "/configs", None).await
    }

    /// Update configuration
    pub async fn update_config(&self, config: HashMap<String, String>) -> Result<(), HttpError> {
        let body_bytes =
            serde_json::to_vec(&config).map_err(|e| HttpError::ClientError(e.to_string()))?;

        self.request_no_response("PATCH", "/configs", Some(body_bytes))
            .await
    }
}

impl ClashController {
    async fn request_no_response(
        &self,
        method: &str,
        path: &str,
        body: Option<Vec<u8>>,
    ) -> Result<(), HttpError> {
        let client = Client::builder(TokioExecutor::new()).build(UnixConnector);
        let uri: hyper::Uri = UnixUri::new(&self.socket_path, path).into();

        let request_builder = Request::builder()
            .uri(uri)
            .method(method)
            .header("Content-Type", "application/json");

        let request = if let Some(body_data) = body {
            request_builder
                .body(Full::new(Bytes::from(body_data)))
                .map_err(|e| HttpError::RequestFailed(e.to_string()))?
        } else {
            request_builder
                .body(Full::new(Bytes::new()))
                .map_err(|e| HttpError::RequestFailed(e.to_string()))?
        };

        let response = client
            .request(request)
            .await
            .map_err(|e| HttpError::RequestFailed(e.to_string()))?;

        if !response.status().is_success() {
            return Err(HttpError::StatusError(response.status().as_u16() as i32));
        }

        Ok(())
    }
    async fn request<T>(
        &self,
        method: &str,
        path: &str,
        body: Option<Vec<u8>>,
    ) -> Result<T, HttpError>
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
                .map_err(|e| HttpError::RequestFailed(e.to_string()))?
        } else {
            request_builder
                .body(Full::new(Bytes::new()))
                .map_err(|e| HttpError::RequestFailed(e.to_string()))?
        };

        let response = client
            .request(request)
            .await
            .map_err(|e| HttpError::RequestFailed(e.to_string()))?;

        if !response.status().is_success() {
            return Err(HttpError::StatusError(response.status().as_u16() as i32));
        }

        let body_bytes = response
            .into_body()
            .collect()
            .await
            .map_err(|e| HttpError::RequestFailed(e.to_string()))?
            .to_bytes();

        serde_json::from_slice(&body_bytes).map_err(|e| {
            HttpError::ParseError(format!("{e}{}", String::from_utf8_lossy(&body_bytes)))
        })
    }
}

#[derive(Debug, thiserror::Error, uniffi::Error)]
pub enum HttpError {
    #[error("Request failed: {0}")]
    RequestFailed(String),

    #[error("HTTP status error: {0}")]
    StatusError(i32),

    #[error("Failed to parse response: {0}")]
    ParseError(String),

    #[error("Client error: {0}")]
    ClientError(String),

    #[error("Not supported: {0}")]
    NotSupported(String),
}
