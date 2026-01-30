use eyre::Context;
use tracing::{error, info};

use crate::EyreError;

#[derive(uniffi::Record)]
pub struct DownloadResult {
    pub success: bool,
    pub file_size: u64,
    pub error_message: Option<String>,
}

#[uniffi::export(async_runtime = "tokio")]
pub async fn download_config(
    url: String,
    output_path: String,
    user_agent: Option<String>,
    proxy_url: Option<String>,
) -> Result<DownloadResult, EyreError> {
    use http_body_util::BodyExt;
    use hyper_util::client::legacy::Client;
    use hyper_util::rt::TokioExecutor;

    info!("Starting download from: {}", url);

    let uri: hyper::Uri = url.parse().map_err(|e| eyre::eyre!("Invalid URL: {}", e))?;

    let ua = user_agent.unwrap_or_else(|| "clash-android/1.0".to_string());
    info!("Using User-Agent: {}", ua);

    // Build client with or without proxy
    let response = if let Some(proxy) = proxy_url {
        use hyper_http_proxy::{Intercept, Proxy, ProxyConnector};
        use hyper_rustls::HttpsConnectorBuilder;

        info!("Using proxy: {}", proxy);
        let proxy_uri: hyper::Uri = proxy
            .parse()
            .map_err(|e| eyre::eyre!("Invalid proxy URL: {}", e))?;

        let proxy = Proxy::new(Intercept::All, proxy_uri);
        let https_connector = HttpsConnectorBuilder::new()
            .with_webpki_roots()
            .https_or_http()
            .enable_http1()
            .build();
        let proxy_connector = ProxyConnector::from_proxy(https_connector, proxy)
            .map_err(|e| eyre::eyre!("Failed to create proxy connector: {}", e))?;

        let client = Client::builder(TokioExecutor::new()).build(proxy_connector);

        // Build request with proper headers
        let req = hyper::Request::builder()
            .method("GET")
            .uri(&uri)
            .header("User-Agent", &ua)
            .header("Accept", "*/*")
            .body(http_body_util::Empty::<bytes::Bytes>::new())
            .map_err(|e| eyre::eyre!("Failed to build request: {}", e))?;

        info!("Sending request with proxy to: {}", uri);
        client.request(req).await?
    } else {
        use hyper_rustls::HttpsConnectorBuilder;

        let https_connector = HttpsConnectorBuilder::new()
            .with_webpki_roots()
            .https_or_http()
            .enable_http1()
            .build();

        let client = Client::builder(TokioExecutor::new()).build(https_connector);

        // Build request with proper headers
        let req = hyper::Request::builder()
            .method("GET")
            .uri(&uri)
            .header("User-Agent", &ua)
            .header("Accept", "*/*")
            .body(http_body_util::Empty::<bytes::Bytes>::new())
            .context("Failed to build request")?;

        info!("Sending request to: {}", uri);
        client.request(req).await?
    };

    if !response.status().is_success() {
        let status = response.status();

        error!("HTTP request failed with status: {} for URL: {}", status, url);
        return Ok(DownloadResult {
            success: false,
            file_size: 0,
            error_message: Some(format!("HTTP {} - {}", status.as_u16(), status.canonical_reason().unwrap_or("Unknown"))),
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
    _ = tokio::fs::File::create(&output_path)
        .await
        .context(format!("Failed to create file: {output_path}"))?;
    tokio::fs::write(&output_path, &bytes)
        .await
        .context(format!("Failed to write to file: {output_path}"))?;

    let file_size = bytes.len() as u64;
    info!(
        "Download completed: {} bytes written to {}",
        file_size, output_path
    );

    Ok(DownloadResult {
        success: true,
        file_size,
        error_message: None,
    })
}
