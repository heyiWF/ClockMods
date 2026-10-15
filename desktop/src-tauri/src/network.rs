use futures_util::StreamExt;
use serde::Serialize;
use std::time::Duration;
const MAX_RESPONSE_BYTES: usize = 1048576;

fn http_client_builder() -> reqwest::ClientBuilder {
    reqwest::Client::builder()
        .timeout(Duration::from_secs(15))
        .redirect(reqwest::redirect::Policy::none())
        // QWeather returns gzip-compressed JSON. Decode it before UTF-8 parsing
        // and apply the streaming size limit to the decompressed bytes.
        .gzip(true)
}

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
    let client = http_client_builder()
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
    read_response(response).await
}

async fn read_response(response: reqwest::Response) -> Result<HttpReply, String> {
    let status = response.status().as_u16();
    let date = response
        .headers()
        .get("date")
        .and_then(|v| v.to_str().ok())
        .map(String::from);
    if response.content_length().unwrap_or(0) > MAX_RESPONSE_BYTES as u64 {
        return Err("Response too large".into());
    }
    let mut stream = response.bytes_stream();
    let mut bytes = Vec::new();
    while let Some(chunk) = stream.next().await {
        let chunk = chunk.map_err(|_| "Failed to read response")?;
        if bytes.len() + chunk.len() > MAX_RESPONSE_BYTES {
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
    use flate2::{write::GzEncoder, Compression};
    use std::{
        io::{Read, Write},
        net::TcpListener,
        thread,
    };

    fn gzip(body: &[u8]) -> Vec<u8> {
        let mut encoder = GzEncoder::new(Vec::new(), Compression::default());
        encoder.write_all(body).unwrap();
        encoder.finish().unwrap()
    }

    // Use the production client and response reader against a local HTTP fixture.
    // The public command still requires HTTPS and validates credential hosts.
    fn mock_reply(
        body: Vec<u8>,
        compressed: bool,
        status: u16,
        head: bool,
    ) -> Result<HttpReply, String> {
        let listener = TcpListener::bind("127.0.0.1:0").unwrap();
        let address = listener.local_addr().unwrap();
        let server = thread::spawn(move || {
            let (mut socket, _) = listener.accept().unwrap();
            socket
                .set_read_timeout(Some(Duration::from_secs(5)))
                .unwrap();
            let mut request = Vec::new();
            let mut buffer = [0; 1024];
            while !request.windows(4).any(|bytes| bytes == b"\r\n\r\n") {
                let count = socket.read(&mut buffer).unwrap();
                assert!(count > 0);
                request.extend_from_slice(&buffer[..count]);
            }
            let encoding = if compressed {
                "Content-Encoding: gzip\r\n"
            } else {
                ""
            };
            write!(socket,
                "HTTP/1.1 {status} Test\r\nContent-Type: application/json\r\n{encoding}Content-Length: {}\r\nDate: Thu, 15 Oct 2026 15:53:22 GMT\r\nConnection: close\r\n\r\n",
                body.len()
            ).unwrap();
            if !head {
                // The client may stop reading as soon as the size limit is hit.
                let _ = socket.write_all(&body);
            }
        });
        let result = tauri::async_runtime::block_on(async {
            let client = http_client_builder().no_proxy().build().unwrap();
            let url = format!("http://{address}");
            let request = if head {
                client.head(url)
            } else {
                client.get(url)
            };
            read_response(request.send().await.unwrap()).await
        });
        server.join().unwrap();
        result
    }

    #[test]
    fn reads_gzip_weather_json() {
        let body = r#"{"code":"200","now":{"text":"晴","temp":"23"}}"#;
        let reply = mock_reply(gzip(body.as_bytes()), true, 200, false).unwrap();
        assert_eq!(reply.body, body);
        assert_eq!(reply.status, 200);
        assert_eq!(reply.date.as_deref(), Some("Thu, 15 Oct 2026 15:53:22 GMT"));
    }

    #[test]
    fn preserves_plain_and_gzip_error_responses() {
        let body = br#"{"code":"401"}"#;
        for compressed in [false, true] {
            let bytes = if compressed {
                gzip(body)
            } else {
                body.to_vec()
            };
            let reply = mock_reply(bytes, compressed, 401, false).unwrap();
            assert_eq!(reply.status, 401);
            assert_eq!(reply.body.as_bytes(), body);
        }
    }

    #[test]
    fn limits_decompressed_response_size() {
        let body = vec![b'a'; MAX_RESPONSE_BYTES + 1];
        let compressed = gzip(&body);
        assert!(compressed.len() < MAX_RESPONSE_BYTES);
        assert_eq!(
            mock_reply(compressed, true, 200, false).err().as_deref(),
            Some("Response too large")
        );
    }

    #[test]
    fn rejects_invalid_utf8_and_corrupt_gzip() {
        assert_eq!(
            mock_reply(vec![0xff], false, 200, false).err().as_deref(),
            Some("Invalid response encoding")
        );
        assert_eq!(
            mock_reply(b"not gzip".to_vec(), true, 200, false)
                .err()
                .as_deref(),
            Some("Failed to read response")
        );
    }

    #[test]
    fn head_response_preserves_date_without_reading_gzip_body() {
        let reply = mock_reply(gzip(b"{}"), true, 200, true).unwrap();
        assert!(reply.body.is_empty());
        assert_eq!(reply.date.as_deref(), Some("Thu, 15 Oct 2026 15:53:22 GMT"));
    }

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
