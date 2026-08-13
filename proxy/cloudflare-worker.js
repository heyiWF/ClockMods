/**
 * Optional QWeather signing proxy for ClockMods Web.
 *
 * Deploy this if you would rather not put the Ed25519 private key in a browser,
 * or if the QWeather host does not send CORS headers for your account. The
 * browser then calls `<worker-url>/v7/weather/now?...` with no credentials and
 * the worker signs the request server-side.
 *
 * Deploy (Cloudflare Workers):
 *   1. npx wrangler init clockmods-weather --no-git
 *   2. replace src/index.js with this file
 *   3. npx wrangler secret put QWEATHER_PRIVATE_KEY     (PKCS#8 Base64, one line)
 *      npx wrangler secret put QWEATHER_CREDENTIAL_ID
 *      npx wrangler secret put QWEATHER_PROJECT_ID
 *      (optional) set QWEATHER_API_HOST and ALLOWED_ORIGIN as plain vars
 *   4. npx wrangler deploy
 *   5. paste the worker URL into ClockMods → 设置 → 天气 → 代理地址
 *
 * The same handler runs unmodified on Vercel/Netlify edge functions and Deno
 * Deploy; only the export wrapper differs.
 */

const DEFAULT_API_HOST = 'devapi.qweather.com';

/** Paths the app is allowed to reach, so the proxy is not an open relay. */
const ALLOWED_PREFIXES = [
  '/v7/weather/',
  '/geo/v2/city/',
  '/airquality/v1/current/',
  '/weatheralert/v1/current/',
];

export default {
  async fetch(request, env) {
    const origin = env.ALLOWED_ORIGIN || '*';
    if (request.method === 'OPTIONS') {
      return new Response(null, { status: 204, headers: corsHeaders(origin) });
    }
    if (request.method !== 'GET' && request.method !== 'HEAD') {
      return json({ code: '405' }, 405, origin);
    }

    const url = new URL(request.url);
    const path = url.pathname + url.search;
    if (!ALLOWED_PREFIXES.some((prefix) => url.pathname.startsWith(prefix))) {
      return json({ code: '404' }, 404, origin);
    }

    // A HEAD request with no path is the time-source probe: answer it directly so
    // the Date header comes from the edge rather than from QWeather.
    if (request.method === 'HEAD') {
      return new Response(null, { status: 204, headers: corsHeaders(origin) });
    }

    let token;
    try {
      token = await createToken(
        env.QWEATHER_CREDENTIAL_ID,
        env.QWEATHER_PROJECT_ID,
        env.QWEATHER_PRIVATE_KEY,
        Math.floor(Date.now() / 1000)
      );
    } catch (error) {
      return json({ code: '401', error: String(error) }, 500, origin);
    }

    const upstream = await fetch(`https://${env.QWEATHER_API_HOST || DEFAULT_API_HOST}${path}`, {
      headers: { Authorization: `Bearer ${token}` },
    });
    const body = await upstream.text();
    return new Response(body, {
      status: upstream.status,
      headers: {
        ...corsHeaders(origin),
        'content-type': upstream.headers.get('content-type') || 'application/json',
        // Let the browser reuse a response for a minute; the app schedules its own
        // refreshes well above that.
        'cache-control': 'public, max-age=60',
      },
    });
  },
};

function corsHeaders(origin) {
  return {
    'access-control-allow-origin': origin,
    'access-control-allow-methods': 'GET, HEAD, OPTIONS',
    'access-control-allow-headers': 'content-type',
    'access-control-max-age': '86400',
  };
}

function json(payload, status, origin) {
  return new Response(JSON.stringify(payload), {
    status,
    headers: { ...corsHeaders(origin), 'content-type': 'application/json' },
  });
}

// ---- JWT signing (same claims as the in-browser signer) ----

async function createToken(credentialId, projectId, privateKeyBase64, nowSeconds) {
  const header = `{"alg":"EdDSA","kid":"${credentialId}"}`;
  const issuedAt = nowSeconds - 30;
  const payload = `{"sub":"${projectId}","iat":${issuedAt},"exp":${issuedAt + 900}}`;
  const encoder = new TextEncoder();
  const signingInput = `${base64Url(encoder.encode(header))}.${base64Url(encoder.encode(payload))}`;
  const key = await crypto.subtle.importKey(
    'pkcs8',
    decodeBase64(privateKeyBase64),
    { name: 'Ed25519' },
    false,
    ['sign']
  );
  const signature = await crypto.subtle.sign('Ed25519', key, encoder.encode(signingInput));
  return `${signingInput}.${base64Url(new Uint8Array(signature))}`;
}

function base64Url(bytes) {
  let binary = '';
  for (const byte of bytes) binary += String.fromCharCode(byte);
  return btoa(binary).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

function decodeBase64(value) {
  const cleaned = value
    .replace(/-----[A-Z ]+-----/g, '')
    .replace(/\s+/g, '')
    .replace(/-/g, '+')
    .replace(/_/g, '/');
  const binary = atob(cleaned.padEnd(Math.ceil(cleaned.length / 4) * 4, '='));
  const bytes = new Uint8Array(binary.length);
  for (let index = 0; index < binary.length; index++) bytes[index] = binary.charCodeAt(index);
  return bytes;
}
