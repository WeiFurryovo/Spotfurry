package com.weifurry.spotfurry.presentation.routes

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
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
import androidx.wear.compose.material3.Text
import com.weifurry.spotfurry.data.applemusic.AppleMusicConfig
import com.weifurry.spotfurry.data.applemusic.AppleMusicPairingClient
import com.weifurry.spotfurry.data.applemusic.AppleMusicPairingStartResponse
import com.weifurry.spotfurry.data.applemusic.AppleMusicTokenStore
import com.weifurry.spotfurry.presentation.components.QrCodeImage
import com.weifurry.spotfurry.presentation.components.SmallIconBubble
import com.weifurry.spotfurry.presentation.components.StatusPill
import kotlinx.coroutines.delay

@Composable
internal fun AppleMusicPairingRoute(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val config = remember(context) { AppleMusicConfig.fromResources(context) }
    val tokenStore = remember(context) { AppleMusicTokenStore(context) }
    val pairingClient = remember { AppleMusicPairingClient() }
    var sessionKey by remember { mutableIntStateOf(0) }
    var pairingSession by remember {
        mutableStateOf<AppleMusicPairingStartResponse?>(null)
    }
    var pairingMessage by remember {
        mutableStateOf(
            if (config.hasAuthBackend) {
                "正在创建二维码"
            } else {
                "先配置扫码登录后端"
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
            pairingMessage = "先配置 spotfurry.appleMusicAuthBaseUrl"
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
            delay(session.pollAfterMs.coerceAtLeast(MIN_POLL_INTERVAL_MS).toLong())

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
                                val musicUserToken = status.musicUserToken.orEmpty()
                                if (musicUserToken.isBlank()) {
                                    errorMessage = "后端没有返回 Apple Music user token"
                                    pairingMessage = "登录结果无效"
                                } else {
                                    tokenStore.musicUserToken = musicUserToken
                                    isAuthorized = true
                                    pairingMessage = "登录成功，已保存到手表"
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
                        errorMessage = error.message ?: "无法读取扫码登录状态"
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
                                    Color(0xFF111111),
                                    Color(0xFF050505),
                                    Color.Black
                                )
                        )
                    )
        ) {
            val compact = maxWidth < 220.dp
            val topPadding = if (compact) 20.dp else 28.dp
            val qrSize = if (compact) 108.dp else 130.dp
            val qrPadding = if (compact) 8.dp else 10.dp
            val bottomPadding = if (compact) 14.dp else 20.dp
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
                contentDescription = "刷新二维码",
                size = if (compact) 30.dp else 34.dp,
                iconSize = if (compact) 15.dp else 17.dp,
                modifier =
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = topPadding + 10.dp, end = 30.dp),
                bubbleColor = Color(0xFF202020),
                borderColor = Color(0xFF343434),
                iconTint = Color(0xFFECECEC)
            )

            Column(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth(0.72f)
                        .padding(top = topPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                StatusPill(
                    text = pillText,
                    active = config.hasAuthBackend && errorMessage == null
                )
                Text(
                    text = "Apple Music 登录",
                    modifier = Modifier.padding(top = 7.dp),
                    color = Color(0xFFF5F5F5),
                    fontSize = if (compact) 15.sp else 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Text(
                    text =
                        if (config.hasAuthBackend) {
                            "用手机完成授权"
                        } else {
                            "先配置配对后端"
                        },
                    modifier = Modifier.padding(top = 2.dp),
                    color = Color(0xFFB6B6B6),
                    fontSize = if (compact) 9.sp else 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
            }

            if (pairingSession != null) {
                Box(
                    modifier =
                        Modifier
                            .align(Alignment.Center)
                            .offset(y = if (compact) 8.dp else 10.dp)
                            .clip(RoundedCornerShape(if (compact) 18.dp else 22.dp))
                            .background(Color.White)
                            .padding(qrPadding),
                    contentAlignment = Alignment.Center
                ) {
                    QrCodeImage(
                        content = pairingSession!!.pairUrl,
                        contentDescription = "Apple Music 配对二维码",
                        modifier = Modifier.size(qrSize)
                    )
                }
            } else if (isLoading) {
                PairingInfoCard(
                    title = "创建二维码",
                    body = "正在连接 SpotfurryAuth",
                    compact = compact,
                    modifier =
                        Modifier
                            .align(Alignment.Center)
                            .offset(y = if (compact) 8.dp else 10.dp)
                )
            } else if (errorMessage != null) {
                PairingInfoCard(
                    title = "连接失败",
                    body = errorMessage!!,
                    compact = compact,
                    modifier =
                        Modifier
                            .align(Alignment.Center)
                            .offset(y = if (compact) 8.dp else 10.dp)
                )
            } else {
                PairingInfoCard(
                    title = "未配置后端",
                    body = "设置 AuthBaseUrl 后再生成二维码",
                    compact = compact,
                    modifier =
                        Modifier
                            .align(Alignment.Center)
                            .offset(y = if (compact) 8.dp else 10.dp)
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
                Text(
                    text = pairingSession?.let { "配对码 ${it.code}" } ?: "等待配对",
                    color = Color(0xFFEDEDED),
                    fontSize = if (compact) 11.sp else 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = pairingMessage,
                    modifier = Modifier.padding(top = 2.dp),
                    color = Color(0xFF9E9E9E),
                    fontSize = if (compact) 8.sp else 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Row(
                    modifier = Modifier.padding(top = if (compact) 5.dp else 7.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PairingTextButton(
                        text = "返回",
                        onClick = onBack
                    )
                    PairingTextButton(
                        text = "刷新",
                        onClick = { sessionKey += 1 }
                    )
                }
            }
        }
    }
}

@Composable
private fun PairingInfoCard(
    title: String,
    body: String,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier =
            modifier
                .widthIn(max = if (compact) 128.dp else 152.dp)
                .clip(RoundedCornerShape(if (compact) 18.dp else 22.dp))
                .background(Color(0xFF141414))
                .border(
                    width = 1.dp,
                    color = Color(0xFF313131),
                    shape = RoundedCornerShape(if (compact) 18.dp else 22.dp)
                )
                .padding(horizontal = if (compact) 14.dp else 16.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            color = Color(0xFFF1F1F1),
            fontSize = if (compact) 13.sp else 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
        Text(
            text = body,
            modifier = Modifier.padding(top = 6.dp),
            color = Color(0xFFA8A8A8),
            fontSize = if (compact) 8.sp else 9.sp,
            lineHeight = if (compact) 11.sp else 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PairingTextButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier =
            Modifier
                .clip(RoundedCornerShape(50))
                .background(Color(0xFF171717))
                .border(
                    width = 1.dp,
                    color = Color(0xFF2D2D2D),
                    shape = RoundedCornerShape(50)
                )
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color(0xFFDCDCDC),
            fontSize = 9.sp,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

private const val MIN_POLL_INTERVAL_MS = 1_000
