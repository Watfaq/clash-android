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
    info!("Starting download from: {}", url);

    let ua = user_agent.unwrap_or_else(|| "clash-android/1.0".to_string());
    info!("Using User-Agent: {}", ua);

    // Build reqwest client
    let mut client_builder = reqwest::Client::builder()
        .user_agent(&ua)
        .redirect(reqwest::redirect::Policy::limited(10));

    // Add proxy if provided
    if let Some(proxy) = proxy_url {
        info!("Using proxy: {}", proxy);
        let proxy = reqwest::Proxy::all(&proxy)
            .map_err(|e| eyre::eyre!("Invalid proxy URL: {}", e))?;
        client_builder = client_builder.proxy(proxy);
    }

    let client = client_builder
        .build()
        .map_err(|e| eyre::eyre!("Failed to build HTTP client: {}", e))?;

    // Send request
    info!("Sending request to: {}", url);
    let response = client
        .get(&url)
        .send()
        .await
        .map_err(|e| eyre::eyre!("Failed to send request: {}", e))?;

    let status = response.status();
    if !status.is_success() {
        error!("HTTP request failed with status: {} for URL: {}", status, url);
        return Ok(DownloadResult {
            success: false,
            file_size: 0,
            error_message: Some(format!("HTTP {} - {}", status.as_u16(), status.canonical_reason().unwrap_or("Unknown"))),
        });
    }

    // Download response body
    let bytes = response
        .bytes()
        .await
        .map_err(|e| eyre::eyre!("Failed to read response body: {}", e))?;

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
