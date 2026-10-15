use futures_util::StreamExt;
use serde::Serialize;
use std::time::Duration;
#[derive(Serialize)]
pub struct HttpReply {
    pub status: u16,
    pub body: String,
    pub date: Option<String>,
}
// No arbitrary methods, redirects, local hosts or caller-supplied headers. The
// credential is accepted only for QWeather hosts and never persisted or logged.
pub fn validate_url(raw: &str, authorized: bool) -> Result<reqwest::Url, String> {
    let url = reqwest::Url::parse(raw).map_err(|_| "Invalid HTTPS URL")?;
    let host = url.host_str().ok_or("Missing host")?;
    if url.scheme() != "https"
        || url.port_or_known_default() != Some(443)
        || !url.username().is_empty()
        || url.password().is_some()
        || !host.contains('.')
        || host.parse::<std::net::IpAddr>().is_ok()
        || host.ends_with(".localhost")
        || host.ends_with(".local")
    {
        return Err("A public HTTPS hostname is required".into());
    }
    if authorized
        && !(host == "qweather.com"
            || host.ends_with(".qweather.com")
            || host.ends_with(".qweatherapi.com"))
    {
        return Err("Weather tokens may only be sent to QWeather".into());
    }
    Ok(url)
}
#[tauri::command]
pub async fn desktop_http(
    url: String,
    token: Option<String>,
    head: bool,
) -> Result<HttpReply, String> {
    let url = validate_url(&url, token.is_some())?;
    let client = reqwest::Client::builder()
        .timeout(Duration::from_secs(15))
        .redirect(reqwest::redirect::Policy::none())
        .build()
        .map_err(|_| "Unable to create HTTPS client")?;
    let mut request = if head {
        client.head(url)
    } else {
        client.get(url)
    };
    if let Some(token) = token {
        if token.len() > 8192 {
            return Err("Invalid token".into());
        }
        request = request.bearer_auth(token);
    }
    let response = request.send().await.map_err(|_| "HTTPS request failed")?;
    let status = response.status().as_u16();
    let date = response
        .headers()
        .get("date")
        .and_then(|v| v.to_str().ok())
        .map(String::from);
    if response.content_length().unwrap_or(0) > 1048576 {
        return Err("Response too large".into());
    }
    let mut stream = response.bytes_stream();
    let mut bytes = Vec::new();
    while let Some(chunk) = stream.next().await {
        let chunk = chunk.map_err(|_| "Failed to read response")?;
        if bytes.len() + chunk.len() > 1048576 {
            return Err("Response too large".into());
        }
        bytes.extend_from_slice(&chunk);
    }
    Ok(HttpReply {
        status,
        date,
        body: String::from_utf8(bytes).map_err(|_| "Invalid response encoding")?,
    })
}
#[cfg(test)]
mod tests {
    use super::*;
    #[test]
    fn restrict_token_destinations() {
        assert!(validate_url("https://abc.qweatherapi.com/v7/weather/now", true).is_ok());
        for url in [
            "https://evil.test",
            "https://qweather.com.evil.test",
            "http://devapi.qweather.com",
            "https://127.0.0.1",
            "https://user:pass@devapi.qweather.com",
        ] {
            assert!(validate_url(url, true).is_err());
        }
    }
}
