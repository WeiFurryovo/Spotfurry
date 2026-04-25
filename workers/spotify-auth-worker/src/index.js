const PAIRING_TTL_SECONDS = 600;
const POLL_AFTER_MS = 1500;
const SPOTIFY_AUTHORIZE_URL = "https://accounts.spotify.com/authorize";
const SPOTIFY_TOKEN_URL = "https://accounts.spotify.com/api/token";
const SPOTIFY_SCOPES = [
  "streaming",
  "user-read-email",
  "user-read-private",
  "user-modify-playback-state",
  "user-read-playback-state",
];

export default {
  async fetch(request, env, context) {
    if (request.method === "OPTIONS") {
      return corsResponse(null, 204);
    }

    try {
      return await routeRequest(request, env);
    } catch (error) {
      return jsonResponse(
        {
          error: error instanceof Error ? error.message : "Unknown error",
        },
        500,
      );
    }
  },
};

async function routeRequest(request, env) {
  assertEnv(env);

  const url = new URL(request.url);
  if (request.method === "POST" && url.pathname === "/api/spotify/pairing/start") {
    return startPairing(request, env);
  }
  if (request.method === "GET" && url.pathname === "/api/spotify/pairing/status") {
    return getPairingStatus(request, env);
  }
  if (request.method === "GET" && url.pathname === "/spotify/pair") {
    return showPairingPage(request, env);
  }
  if (request.method === "GET" && url.pathname === "/spotify/login") {
    return redirectToSpotify(request, env);
  }
  if (request.method === "GET" && url.pathname === "/spotify/callback") {
    return handleSpotifyCallback(request, env);
  }

  return jsonResponse({ error: "Not found" }, 404);
}

async function startPairing(request, env) {
  const origin = new URL(request.url).origin;
  const session = {
    sessionId: randomToken(18),
    watchSecret: randomToken(32),
    stateSecret: randomToken(24),
    code: randomCode(),
    status: "pending",
    createdAt: Date.now(),
    expiresAt: Date.now() + PAIRING_TTL_SECONDS * 1000,
  };

  await putSession(env, session);

  const pairUrl = new URL("/spotify/pair", origin);
  pairUrl.searchParams.set("sessionId", session.sessionId);
  pairUrl.searchParams.set("code", session.code);

  return jsonResponse({
    sessionId: session.sessionId,
    watchSecret: session.watchSecret,
    code: session.code,
    pairUrl: pairUrl.toString(),
    expiresAt: session.expiresAt,
    pollAfterMs: POLL_AFTER_MS,
  });
}

async function getPairingStatus(request, env) {
  const url = new URL(request.url);
  const sessionId = url.searchParams.get("sessionId") || "";
  const session = await getSession(env, sessionId);
  if (!session) {
    return jsonResponse({ status: "expired", error: "配对会话不存在或已过期" }, 404);
  }

  const bearerToken = getBearerToken(request);
  if (bearerToken !== session.watchSecret) {
    return jsonResponse({ status: "error", error: "watchSecret 无效" }, 401);
  }

  if (isExpired(session)) {
    await deleteSession(env, session.sessionId);
    return jsonResponse({ status: "expired", error: "二维码已过期" });
  }

  if (session.status === "authorized") {
    return jsonResponse({
      status: "authorized",
      expiresAt: session.tokenExpiresAt,
      accessToken: session.accessToken,
      expiresIn: Math.max(1, Math.floor((session.tokenExpiresAt - Date.now()) / 1000)),
      tokenType: session.tokenType || "Bearer",
      scope: session.scope || "",
    });
  }

  if (session.status === "error") {
    return jsonResponse({
      status: "error",
      expiresAt: session.expiresAt,
      error: session.error || "Spotify 授权失败",
    });
  }

  return jsonResponse({
    status: "pending",
    expiresAt: session.expiresAt,
  });
}

async function showPairingPage(request, env) {
  const url = new URL(request.url);
  const sessionId = url.searchParams.get("sessionId") || "";
  const code = url.searchParams.get("code") || "";
  const session = await getSession(env, sessionId);
  if (!session || session.code !== code || isExpired(session)) {
    return htmlResponse(pairingHtml("二维码已过期", "请回到手表刷新 Spotify 配对二维码。"));
  }

  const loginUrl = new URL("/spotify/login", url.origin);
  loginUrl.searchParams.set("sessionId", sessionId);
  loginUrl.searchParams.set("code", code);

  return htmlResponse(pairingHtml("连接 Spotify", `配对码 ${escapeHtml(code)}`, loginUrl.toString()));
}

async function redirectToSpotify(request, env) {
  const url = new URL(request.url);
  const sessionId = url.searchParams.get("sessionId") || "";
  const code = url.searchParams.get("code") || "";
  const session = await getSession(env, sessionId);
  if (!session || session.code !== code || isExpired(session)) {
    return htmlResponse(pairingHtml("二维码已过期", "请回到手表刷新 Spotify 配对二维码。"));
  }

  const state = `${session.sessionId}.${session.stateSecret}`;
  const authorizeUrl = new URL(SPOTIFY_AUTHORIZE_URL);
  authorizeUrl.searchParams.set("client_id", env.SPOTIFY_CLIENT_ID);
  authorizeUrl.searchParams.set("response_type", "code");
  authorizeUrl.searchParams.set("redirect_uri", env.SPOTIFY_REDIRECT_URI);
  authorizeUrl.searchParams.set("scope", SPOTIFY_SCOPES.join(" "));
  authorizeUrl.searchParams.set("state", state);

  return Response.redirect(authorizeUrl.toString(), 302);
}

async function handleSpotifyCallback(request, env) {
  const url = new URL(request.url);
  const state = url.searchParams.get("state") || "";
  const [sessionId, stateSecret] = state.split(".");
  const session = await getSession(env, sessionId);
  if (!session || session.stateSecret !== stateSecret || isExpired(session)) {
    return htmlResponse(pairingHtml("授权已失效", "请回到手表刷新 Spotify 配对二维码。"));
  }

  const spotifyError = url.searchParams.get("error");
  if (spotifyError) {
    await putSession(env, {
      ...session,
      status: "error",
      error: spotifyError,
    });
    return htmlResponse(pairingHtml("Spotify 授权取消", "你可以回到手表刷新二维码后重试。"));
  }

  const authorizationCode = url.searchParams.get("code") || "";
  if (!authorizationCode) {
    await putSession(env, {
      ...session,
      status: "error",
      error: "Spotify 没有返回授权 code",
    });
    return htmlResponse(pairingHtml("授权失败", "Spotify 没有返回授权 code。"));
  }

  const token = await exchangeSpotifyToken(env, authorizationCode);
  const tokenExpiresAt = Date.now() + Math.max(1, token.expires_in || 3600) * 1000;
  await putSession(env, {
    ...session,
    status: "authorized",
    accessToken: token.access_token,
    tokenType: token.token_type || "Bearer",
    scope: token.scope || "",
    tokenExpiresAt,
  });

  return htmlResponse(pairingHtml("授权成功", "Spotify token 已发送给手表，可以回到 Spotfurry 继续测试。"));
}

async function exchangeSpotifyToken(env, authorizationCode) {
  const body = new URLSearchParams();
  body.set("grant_type", "authorization_code");
  body.set("code", authorizationCode);
  body.set("redirect_uri", env.SPOTIFY_REDIRECT_URI);

  const response = await fetch(SPOTIFY_TOKEN_URL, {
    method: "POST",
    headers: {
      "Authorization": `Basic ${btoa(`${env.SPOTIFY_CLIENT_ID}:${env.SPOTIFY_CLIENT_SECRET}`)}`,
      "Content-Type": "application/x-www-form-urlencoded",
    },
    body,
  });

  const payload = await response.json().catch(() => ({}));
  if (!response.ok) {
    throw new Error(payload.error_description || payload.error || `Spotify token HTTP ${response.status}`);
  }
  if (!payload.access_token) {
    throw new Error("Spotify token response missing access_token");
  }
  return payload;
}

async function putSession(env, session) {
  await env.SPOTIFY_PAIRING_KV.put(sessionKey(session.sessionId), JSON.stringify(session), {
    expirationTtl: PAIRING_TTL_SECONDS,
  });
}

async function getSession(env, sessionId) {
  if (!sessionId) {
    return null;
  }
  const stored = await env.SPOTIFY_PAIRING_KV.get(sessionKey(sessionId), "json");
  return stored || null;
}

async function deleteSession(env, sessionId) {
  if (sessionId) {
    await env.SPOTIFY_PAIRING_KV.delete(sessionKey(sessionId));
  }
}

function sessionKey(sessionId) {
  return `spotify:pairing:${sessionId}`;
}

function isExpired(session) {
  return Date.now() > session.expiresAt;
}

function getBearerToken(request) {
  const authorization = request.headers.get("authorization") || "";
  const match = authorization.match(/^Bearer\s+(.+)$/i);
  return match ? match[1] : "";
}

function randomCode() {
  const bytes = new Uint8Array(3);
  crypto.getRandomValues(bytes);
  const value = ((bytes[0] << 16) | (bytes[1] << 8) | bytes[2]) % 1000000;
  return value.toString().padStart(6, "0");
}

function randomToken(byteLength) {
  const bytes = new Uint8Array(byteLength);
  crypto.getRandomValues(bytes);
  return btoa(String.fromCharCode(...bytes))
    .replaceAll("+", "-")
    .replaceAll("/", "_")
    .replaceAll("=", "");
}

function assertEnv(env) {
  const missing = [
    "SPOTIFY_CLIENT_ID",
    "SPOTIFY_CLIENT_SECRET",
    "SPOTIFY_REDIRECT_URI",
    "SPOTIFY_PAIRING_KV",
  ].filter((key) => !env[key]);
  if (missing.length > 0) {
    throw new Error(`Missing Worker env: ${missing.join(", ")}`);
  }
}

function jsonResponse(payload, status = 200) {
  return corsResponse(JSON.stringify(payload), status, {
    "content-type": "application/json; charset=utf-8",
  });
}

function htmlResponse(html, status = 200) {
  return new Response(html, {
    status,
    headers: {
      "content-type": "text/html; charset=utf-8",
      "cache-control": "no-store",
    },
  });
}

function corsResponse(body, status = 200, headers = {}) {
  return new Response(body, {
    status,
    headers: {
      "access-control-allow-origin": "*",
      "access-control-allow-methods": "GET, POST, OPTIONS",
      "access-control-allow-headers": "authorization, content-type",
      "cache-control": "no-store",
      ...headers,
    },
  });
}

function pairingHtml(title, message, loginUrl = "") {
  const action = loginUrl
    ? `<a class="button" href="${escapeHtml(loginUrl)}">登录 Spotify</a>`
    : "";
  return `<!doctype html>
<html lang="zh-CN">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>${escapeHtml(title)} - Spotfurry</title>
  <style>
    :root {
      color-scheme: dark;
      font-family: system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
      background: #050806;
      color: #f4fff7;
    }
    body {
      min-height: 100vh;
      margin: 0;
      display: grid;
      place-items: center;
      background: radial-gradient(circle at 50% 20%, #1db954 0%, #0f3d25 38%, #050806 80%);
    }
    main {
      width: min(88vw, 420px);
      padding: 28px;
      border: 1px solid rgb(255 255 255 / 14%);
      border-radius: 28px;
      background: rgb(0 0 0 / 48%);
      box-shadow: 0 24px 80px rgb(0 0 0 / 40%);
      text-align: center;
    }
    h1 {
      margin: 0 0 10px;
      font-size: 28px;
    }
    p {
      margin: 0;
      color: #d7ffe2;
      line-height: 1.55;
    }
    .button {
      display: inline-flex;
      margin-top: 24px;
      padding: 13px 18px;
      border-radius: 999px;
      background: #1ed760;
      color: #06110a;
      font-weight: 800;
      text-decoration: none;
    }
  </style>
</head>
<body>
  <main>
    <h1>${escapeHtml(title)}</h1>
    <p>${escapeHtml(message)}</p>
    ${action}
  </main>
</body>
</html>`;
}

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}
