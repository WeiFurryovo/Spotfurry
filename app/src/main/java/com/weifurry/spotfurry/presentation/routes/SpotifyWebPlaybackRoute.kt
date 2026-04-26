package com.weifurry.spotfurry.presentation.routes

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.wear.compose.material3.Text
import com.weifurry.spotfurry.data.spotify.SpotifyPairingClient
import com.weifurry.spotfurry.data.spotify.SpotifyPairingStartResponse
import com.weifurry.spotfurry.data.spotify.SpotifyTokenStore
import com.weifurry.spotfurry.data.spotify.SpotifyWebPlaybackConfig
import com.weifurry.spotfurry.presentation.components.QrCodeImage
import com.weifurry.spotfurry.presentation.components.SmallIconBubble
import com.weifurry.spotfurry.presentation.components.StatusPill
import kotlinx.coroutines.delay
import org.json.JSONObject

@Composable
internal fun SpotifyWebPlaybackRoute(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val config = remember(context) { SpotifyWebPlaybackConfig.fromResources(context) }
    val tokenStore = remember(context) { SpotifyTokenStore(context) }
    var storedAccessToken by remember { mutableStateOf(tokenStore.validAccessToken()) }
    val accessToken = config.accessToken.ifBlank { storedAccessToken }

    if (accessToken.isBlank()) {
        SpotifyPairingRoute(
            config = config,
            tokenStore = tokenStore,
            onAuthorized = {
                storedAccessToken = tokenStore.validAccessToken()
            },
            onBack = onBack
        )
        return
    }

    SpotifyWebPlaybackPlayer(
        config = config.withAccessToken(accessToken),
        onClearLogin = {
            tokenStore.clear()
            storedAccessToken = ""
        },
        onBack = onBack
    )
}

@Composable
private fun SpotifyPairingRoute(
    config: SpotifyWebPlaybackConfig,
    tokenStore: SpotifyTokenStore,
    onAuthorized: () -> Unit,
    onBack: () -> Unit
) {
    val pairingClient = remember { SpotifyPairingClient() }
    var sessionKey by remember { mutableIntStateOf(0) }
    var pairingSession by remember {
        mutableStateOf<SpotifyPairingStartResponse?>(null)
    }
    var pairingMessage by remember {
        mutableStateOf(
            if (config.hasAuthBackend) {
                "正在创建二维码"
            } else {
                "先配置 Spotify 授权后端"
            }
        )
    }
    var isLoading by remember { mutableStateOf(false) }
    var isAuthorized by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(config.authBaseUrl, sessionKey) {
        pairingSession = null
        isAuthorized = false
        errorMessage = null

        if (!config.hasAuthBackend) {
            isLoading = false
            pairingMessage = "先配置 spotfurry.spotifyAuthBaseUrl"
            return@LaunchedEffect
        }

        isLoading = true
        pairingMessage = "正在创建二维码"
        pairingClient
            .startPairing(config.authBaseUrl)
            .fold(
                onSuccess = { session ->
                    pairingSession = session
                    pairingMessage = "等待手机扫码授权"
                },
                onFailure = { error ->
                    errorMessage = error.message ?: "二维码创建失败"
                    pairingMessage = "二维码创建失败"
                }
            )
        isLoading = false
    }

    LaunchedEffect(pairingSession?.sessionId) {
        val session = pairingSession ?: return@LaunchedEffect

        while (!isAuthorized && errorMessage == null) {
            delay(session.pollAfterMs.coerceAtLeast(MIN_SPOTIFY_POLL_INTERVAL_MS).toLong())

            pairingClient
                .checkStatus(
                    authBaseUrl = config.authBaseUrl,
                    sessionId = session.sessionId,
                    watchSecret = session.watchSecret
                )
                .fold(
                    onSuccess = { status ->
                        when (status.status) {
                            "pending" -> {
                                pairingMessage = "等待手机授权"
                            }
                            "authorized" -> {
                                val accessToken = status.accessToken.orEmpty()
                                if (accessToken.isBlank()) {
                                    errorMessage = "后端没有返回 Spotify access token"
                                    pairingMessage = "登录结果无效"
                                } else {
                                    tokenStore.saveAccessToken(
                                        accessToken = accessToken,
                                        expiresInSeconds =
                                            status.expiresIn ?: DEFAULT_SPOTIFY_EXPIRES_IN_SECONDS
                                    )
                                    isAuthorized = true
                                    pairingMessage = "登录成功，正在打开播放器"
                                    onAuthorized()
                                }
                            }
                            "expired" -> {
                                errorMessage = "二维码已过期，请刷新"
                                pairingMessage = "二维码已过期"
                            }
                            else -> {
                                errorMessage = status.error ?: "未知登录状态：${status.status}"
                                pairingMessage = "登录状态异常"
                            }
                        }
                    },
                    onFailure = { error ->
                        errorMessage = error.message ?: "无法读取 Spotify 登录状态"
                        pairingMessage = "轮询失败"
                    }
                )
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier =
                Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors =
                                listOf(
                                    Color(0xFF123F28),
                                    Color(0xFF07120D),
                                    Color.Black
                                )
                        )
                    )
        ) {
            val compact = maxWidth < 220.dp
            val topPadding = if (compact) 18.dp else 24.dp
            val qrSize = if (compact) 88.dp else 118.dp
            val qrPadding = if (compact) 7.dp else 9.dp
            val bottomPadding = if (compact) 12.dp else 18.dp
            val titleWidth = if (compact) 0.58f else 0.68f
            val pillText =
                when {
                    isAuthorized -> "已登录"
                    errorMessage != null -> "需要处理"
                    isLoading -> "创建中"
                    config.hasAuthBackend -> "手机扫码"
                    else -> "未配置"
                }

            SmallIconBubble(
                icon = Icons.Filled.Refresh,
                onClick = { sessionKey += 1 },
                contentDescription = "刷新 Spotify 二维码",
                size = if (compact) 30.dp else 34.dp,
                iconSize = if (compact) 15.dp else 17.dp,
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = topPadding + 12.dp, end = 30.dp),
                bubbleColor = Color(0xFF173B28),
                borderColor = Color(0xFF24543A),
                iconTint = Color(0xFFE8FFF0)
            )

            Column(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth(titleWidth)
                        .padding(top = topPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StatusPill(
                    text = pillText,
                    active = config.hasAuthBackend && errorMessage == null
                )
                Text(
                    text = "Spotify 登录",
                    modifier = Modifier.padding(top = if (compact) 5.dp else 7.dp),
                    color = Color(0xFFF2FFF6),
                    fontSize = if (compact) 14.sp else 17.sp,
                    lineHeight = if (compact) 16.sp else 19.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                if (!compact) {
                    Text(
                        text =
                            if (config.hasAuthBackend) {
                                "用手机完成授权"
                            } else {
                                "先配置配对后端"
                            },
                        modifier = Modifier.padding(top = 2.dp),
                        color = Color(0xFFC6D8CC),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (pairingSession != null) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.Center)
                            .offset(y = if (compact) 2.dp else 8.dp)
                            .clip(RoundedCornerShape(if (compact) 18.dp else 22.dp))
                            .background(Color.White)
                            .padding(qrPadding),
                    contentAlignment = Alignment.Center
                ) {
                    QrCodeImage(
                        content = pairingSession!!.pairUrl,
                        contentDescription = "Spotify 配对二维码",
                        modifier = Modifier.size(qrSize)
                    )
                }
            } else {
                SpotifyPairingInfoCard(
                    title =
                        when {
                            isLoading -> "创建二维码"
                            errorMessage != null -> "连接失败"
                            else -> "未配置后端"
                        },
                    body =
                        when {
                            isLoading -> "正在连接 SpotifyAuth"
                            errorMessage != null -> errorMessage!!
                            else -> "设置 AuthBaseUrl 后再生成二维码"
                        },
                    compact = compact,
                    modifier =
                        Modifier
                            .align(Alignment.Center)
                            .offset(y = if (compact) 14.dp else 16.dp)
                )
            }

            Column(
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth(0.78f)
                        .padding(bottom = bottomPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (pairingSession != null) {
                    Text(
                        text = "配对码 ${pairingSession!!.code}",
                        color = Color(0xFFEDEDED),
                        fontSize = if (compact) 10.sp else 12.sp,
                        lineHeight = if (compact) 12.sp else 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = pairingMessage,
                        modifier = Modifier.padding(top = 1.dp),
                        color = Color(0xFFBFE7CA),
                        fontSize = if (compact) 8.sp else 9.sp,
                        lineHeight = if (compact) 10.sp else 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
                Row(
                    modifier = Modifier.padding(top = if (pairingSession != null) 5.dp else 0.dp),
                    horizontalArrangement = Arrangement.spacedBy(if (compact) 10.dp else 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SmallIconBubble(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        onClick = onBack,
                        contentDescription = "返回音乐库",
                        size = if (compact) 28.dp else 32.dp,
                        iconSize = if (compact) 14.dp else 16.dp,
                        bubbleColor = Color(0xFF173B28),
                        borderColor = Color(0xFF24543A),
                        iconTint = Color(0xFFE8FFF0)
                    )
                    SmallIconBubble(
                        icon = Icons.Filled.Refresh,
                        onClick = { sessionKey += 1 },
                        contentDescription = "刷新 Spotify 二维码",
                        size = if (compact) 28.dp else 32.dp,
                        iconSize = if (compact) 14.dp else 16.dp,
                        bubbleColor = Color(0xFF173B28),
                        borderColor = Color(0xFF24543A),
                        iconTint = Color(0xFFE8FFF0)
                    )
                }
            }
        }
    }
}

@Composable
private fun SpotifyPairingInfoCard(
    title: String,
    body: String,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier =
            modifier
                .fillMaxWidth(if (compact) 0.62f else 0.66f)
                .clip(RoundedCornerShape(if (compact) 18.dp else 22.dp))
                .background(Color(0xE6121B16))
                .border(
                    width = 1.dp,
                    color = Color(0xFF24543A),
                    shape = RoundedCornerShape(if (compact) 18.dp else 22.dp)
                )
                .padding(
                    horizontal = if (compact) 12.dp else 16.dp,
                    vertical = if (compact) 10.dp else 14.dp
                ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = Color(0xFFF1F1F1),
            fontSize = if (compact) 13.sp else 15.sp,
            lineHeight = if (compact) 15.sp else 17.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Text(
            text = body,
            modifier = Modifier.padding(top = if (compact) 4.dp else 6.dp),
            color = Color(0xFFC6D8CC),
            fontSize = if (compact) 8.sp else 9.sp,
            lineHeight = if (compact) 11.sp else 12.sp,
            maxLines = if (compact) 3 else 4,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun SpotifyWebPlaybackPlayer(
    config: SpotifyWebPlaybackConfig,
    onClearLogin: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val webViewResult = remember(context) { runCatching { createSpotifyWebView(context) } }
    val webView = webViewResult.getOrNull()

    if (webView == null) {
        SpotifyWebViewUnavailableRoute(
            errorMessage =
                webViewResult.exceptionOrNull()?.message
                    ?: "当前系统没有可用的 WebView Provider",
            onBack = onBack,
            onClearLogin = onClearLogin
        )
        return
    }

    val pageHtml = remember(config) { spotifyWebPlaybackHtml(config) }

    LaunchedEffect(webView, pageHtml) {
        webView.loadDataWithBaseURL(
            SPOTIFY_WEBVIEW_BASE_URL,
            pageHtml,
            "text/html",
            "UTF-8",
            null
        )
    }

    DisposableEffect(webView) {
        onDispose {
            webView.stopLoading()
            webView.destroy()
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { webView },
            modifier =
                Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
        )
        SmallIconBubble(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            onClick = onBack,
            contentDescription = "返回音乐库",
            size = 30.dp,
            iconSize = 15.dp,
            modifier =
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 32.dp, end = 30.dp),
            bubbleColor = Color(0xB3121B16),
            borderColor = Color(0x99333333),
            iconTint = Color.White
        )
    }
}

@Composable
private fun SpotifyWebViewUnavailableRoute(
    errorMessage: String,
    onBack: () -> Unit,
    onClearLogin: () -> Unit
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(
            modifier =
                Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors =
                                listOf(
                                    Color(0xFF153321),
                                    Color(0xFF07120D),
                                    Color.Black
                                )
                        )
                    )
        ) {
            val compact = maxWidth < 220.dp

            Column(
                modifier =
                    Modifier
                        .align(Alignment.Center)
                        .fillMaxWidth(if (compact) 0.74f else 0.70f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StatusPill(
                    text = "WebView 不可用",
                    active = false
                )
                Text(
                    text = "无法打开播放器",
                    modifier = Modifier.padding(top = if (compact) 7.dp else 9.dp),
                    color = Color(0xFFF2FFF6),
                    fontSize = if (compact) 16.sp else 18.sp,
                    lineHeight = if (compact) 18.sp else 20.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Text(
                    text =
                        "Spotify 登录已成功，但当前手表系统没有可用 WebView，无法运行 Web Playback SDK。",
                    modifier = Modifier.padding(top = if (compact) 7.dp else 9.dp),
                    color = Color(0xFFC6D8CC),
                    fontSize = if (compact) 9.sp else 10.sp,
                    lineHeight = if (compact) 12.sp else 14.sp,
                    maxLines = 5,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "系统返回：$errorMessage",
                    modifier = Modifier.padding(top = if (compact) 5.dp else 7.dp),
                    color = Color(0xFF8FA898),
                    fontSize = if (compact) 8.sp else 9.sp,
                    lineHeight = if (compact) 10.sp else 11.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Row(
                    modifier = Modifier.padding(top = if (compact) 10.dp else 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(if (compact) 8.dp else 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SpotifyTextButton(
                        text = "返回",
                        onClick = onBack
                    )
                    SpotifyTextButton(
                        text = "清除登录",
                        onClick = onClearLogin
                    )
                }
            }
        }
    }
}

private fun createSpotifyWebView(context: Context): WebView =
    WebView(context).apply {
        layoutParams =
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        setBackgroundColor(android.graphics.Color.BLACK)
        webChromeClient = WebChromeClient()
        webViewClient = WebViewClient()
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.mediaPlaybackRequiresUserGesture = false
        settings.mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
        settings.cacheMode = WebSettings.LOAD_DEFAULT
    }

private fun spotifyWebPlaybackHtml(config: SpotifyWebPlaybackConfig): String {
    val accessToken = JSONObject.quote(config.accessToken)
    val playbackUri = JSONObject.quote(config.playbackUri)
    val configuredUriLabel =
        escapeHtml(
            if (config.hasPlaybackUri) {
                config.playbackUri
            } else {
                "未配置，先把手表设为 Spotify 设备"
            }
        )

    return """
        <!doctype html>
        <html lang="zh-CN">
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">
          <script src="https://sdk.scdn.co/spotify-player.js"></script>
          <style>
            :root {
              color-scheme: dark;
              font-family: system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", sans-serif;
              background: #000;
              color: #f7fff9;
            }
            * {
              box-sizing: border-box;
              -webkit-tap-highlight-color: transparent;
            }
            html,
            body {
              width: 100%;
              height: 100%;
              margin: 0;
              overflow: hidden;
              background: radial-gradient(circle at 50% 35%, #1db954 0%, #0f3d25 34%, #050806 72%, #000 100%);
            }
            body {
              display: grid;
              place-items: center;
            }
            main {
              width: 100vw;
              height: 100vh;
              display: flex;
              flex-direction: column;
              align-items: center;
              justify-content: center;
              gap: 8px;
              padding: 28px 24px 24px;
              text-align: center;
            }
            .pill {
              border: 1px solid rgba(255, 255, 255, 0.2);
              border-radius: 999px;
              padding: 3px 9px;
              background: rgba(0, 0, 0, 0.34);
              color: #d7ffe2;
              font-size: 10px;
              font-weight: 700;
              letter-spacing: 0.02em;
            }
            h1 {
              margin: 0;
              max-width: 72vw;
              color: #ffffff;
              font-size: 18px;
              line-height: 1.1;
            }
            .track {
              max-width: 72vw;
              min-height: 30px;
              color: #ccefd6;
              font-size: 10px;
              line-height: 1.35;
              word-break: break-word;
            }
            .status {
              width: min(72vw, 180px);
              min-height: 36px;
              border: 1px solid rgba(255, 255, 255, 0.14);
              border-radius: 18px;
              padding: 8px 10px;
              background: rgba(0, 0, 0, 0.38);
              color: #e7ffee;
              font-size: 10px;
              line-height: 1.28;
            }
            .controls {
              display: grid;
              grid-template-columns: repeat(3, 42px);
              gap: 8px;
              align-items: center;
              justify-content: center;
            }
            button {
              width: 42px;
              height: 42px;
              border: 1px solid rgba(255, 255, 255, 0.16);
              border-radius: 50%;
              background: rgba(0, 0, 0, 0.52);
              color: #fff;
              font-size: 16px;
              font-weight: 800;
            }
            button.primary {
              background: #1ed760;
              color: #06110a;
              border-color: rgba(255, 255, 255, 0.32);
            }
            button:disabled {
              opacity: 0.42;
            }
            .hint {
              max-width: 68vw;
              color: rgba(236, 255, 242, 0.7);
              font-size: 8px;
              line-height: 1.25;
            }
            @media (max-width: 210px) {
              main {
                gap: 6px;
                padding: 24px 20px 18px;
              }
              h1 {
                font-size: 16px;
              }
              .controls {
                grid-template-columns: repeat(3, 38px);
                gap: 7px;
              }
              button {
                width: 38px;
                height: 38px;
                font-size: 14px;
              }
            }
          </style>
        </head>
        <body>
          <main>
            <div id="pill" class="pill">加载 SDK</div>
            <h1>Spotify WebView</h1>
            <div id="track" class="track">$configuredUriLabel</div>
            <div id="status" class="status">正在载入 Spotify Web Playback SDK</div>
            <div class="controls">
              <button id="prev" type="button" disabled>‹</button>
              <button id="play" class="primary" type="button" disabled>▶</button>
              <button id="next" type="button" disabled>›</button>
            </div>
            <div class="controls">
              <button id="device" type="button" disabled>⌁</button>
              <button id="pause" type="button" disabled>Ⅱ</button>
              <button id="refresh" type="button">↻</button>
            </div>
            <div class="hint">Premium + streaming 权限。若不能发声，先在手机 Spotify 中选择 Spotfurry WebView 设备。</div>
          </main>
          <script>
            const config = {
              token: $accessToken,
              playbackUri: $playbackUri
            };
            let player = null;
            let deviceId = "";
            let ready = false;

            const pill = document.getElementById("pill");
            const status = document.getElementById("status");
            const track = document.getElementById("track");
            const play = document.getElementById("play");
            const pause = document.getElementById("pause");
            const prev = document.getElementById("prev");
            const next = document.getElementById("next");
            const device = document.getElementById("device");
            const refresh = document.getElementById("refresh");

            function setStatus(message, label = null) {
              status.textContent = message;
              if (label) {
                pill.textContent = label;
              }
            }

            function setReady(value) {
              ready = value;
              play.disabled = !value;
              pause.disabled = !value;
              prev.disabled = !value;
              next.disabled = !value;
              device.disabled = !value;
            }

            async function spotifyFetch(path, options = {}) {
              const response = await fetch("https://api.spotify.com/v1" + path, {
                ...options,
                headers: {
                  "Authorization": "Bearer " + config.token,
                  "Content-Type": "application/json",
                  ...(options.headers || {})
                }
              });
              if (response.status === 204) {
                return null;
              }
              const text = await response.text();
              if (!response.ok) {
                throw new Error(text || ("HTTP " + response.status));
              }
              return text ? JSON.parse(text) : null;
            }

            function playbackBody(shouldPlay = true) {
              const body = {
                device_ids: [deviceId],
                play: shouldPlay
              };
              return JSON.stringify(body);
            }

            function playBody() {
              const uri = config.playbackUri.trim();
              if (!uri) {
                return "{}";
              }
              if (uri.startsWith("spotify:track:")) {
                return JSON.stringify({ uris: [uri] });
              }
              return JSON.stringify({ context_uri: uri });
            }

            async function activatePlaybackElement() {
              if (player && typeof player.activateElement === "function") {
                await player.activateElement();
              }
            }

            async function transferPlayback(shouldPlay = false) {
              if (!ready) {
                return;
              }
              if (shouldPlay) {
                await activatePlaybackElement();
              }
              await spotifyFetch("/me/player", {
                method: "PUT",
                body: playbackBody(shouldPlay)
              });
              setStatus("已把播放设备切到手表 WebView", "设备就绪");
            }

            async function playConfiguredUri() {
              if (!ready) {
                return;
              }
              await activatePlaybackElement();
              if (!config.playbackUri.trim()) {
                await transferPlayback(true);
                setStatus("未配置 URI，已尝试继续当前 Spotify 队列", "播放中");
                return;
              }
              await spotifyFetch("/me/player/play?device_id=" + encodeURIComponent(deviceId), {
                method: "PUT",
                body: playBody()
              });
              setStatus("已请求播放配置的 Spotify URI", "播放中");
            }

            function run(action, loadingText) {
              setStatus(loadingText);
              action().catch((error) => {
                setStatus(error.message || "Spotify 操作失败", "失败");
              });
            }

            play.addEventListener("click", () => run(playConfiguredUri, "正在请求播放"));
            pause.addEventListener("click", () => run(() => player.pause(), "正在暂停"));
            prev.addEventListener("click", () => run(() => player.previousTrack(), "正在上一首"));
            next.addEventListener("click", () => run(() => player.nextTrack(), "正在下一首"));
            device.addEventListener("click", () => run(() => transferPlayback(false), "正在设为播放设备"));
            refresh.addEventListener("click", () => location.reload());

            window.onSpotifyWebPlaybackSDKReady = () => {
              player = new Spotify.Player({
                name: "Spotfurry WebView",
                getOAuthToken: callback => callback(config.token),
                volume: 0.72
              });

              player.addListener("ready", ({ device_id }) => {
                deviceId = device_id;
                setReady(true);
                setStatus("设备已注册。点 ⌁ 设为设备，或点 ▶ 播放 URI。", "设备就绪");
              });

              player.addListener("not_ready", ({ device_id }) => {
                setReady(false);
                setStatus("设备离线：" + device_id, "离线");
              });

              player.addListener("initialization_error", ({ message }) => setStatus(message, "初始化失败"));
              player.addListener("authentication_error", ({ message }) => setStatus(message, "鉴权失败"));
              player.addListener("account_error", ({ message }) => setStatus(message, "账号限制"));
              player.addListener("playback_error", ({ message }) => setStatus(message, "播放失败"));

              player.addListener("player_state_changed", (state) => {
                if (!state) {
                  return;
                }
                const current = state.track_window.current_track;
                const artist = current.artists.map(item => item.name).join(" / ");
                track.textContent = current.name + " · " + artist;
                pill.textContent = state.paused ? "已暂停" : "播放中";
              });

              player.connect().then((success) => {
                if (!success) {
                  setStatus("Web Playback SDK 连接失败", "失败");
                }
              });
            };

            window.addEventListener("error", (event) => {
              setStatus(event.message || "页面脚本错误", "错误");
            });
          </script>
        </body>
        </html>
    """.trimIndent()
}

private fun escapeHtml(value: String): String =
    buildString(value.length) {
        value.forEach { character ->
            when (character) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&#39;")
                else -> append(character)
            }
        }
    }

@Composable
private fun SpotifyTextButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier =
            Modifier
                .clip(RoundedCornerShape(50))
                .background(Color(0xE6173B28))
                .border(
                    width = 1.dp,
                    color = Color(0xFF24543A),
                    shape = RoundedCornerShape(50)
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 10.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color(0xFFE8FFF0),
            fontSize = 9.sp,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

private const val MIN_SPOTIFY_POLL_INTERVAL_MS = 1_000
private const val DEFAULT_SPOTIFY_EXPIRES_IN_SECONDS = 3_600
private const val SPOTIFY_WEBVIEW_BASE_URL = "https://spotfurry.local/spotify/"
