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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.weifurry.spotfurry.data.applemusic.AppleMusicPairingSession
import com.weifurry.spotfurry.data.applemusic.buildAppleMusicPairingUrl
import com.weifurry.spotfurry.presentation.components.QrCodeImage
import com.weifurry.spotfurry.presentation.components.SmallIconBubble
import com.weifurry.spotfurry.presentation.components.StatusPill

@Composable
internal fun AppleMusicPairingRoute(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val config = remember(context) { AppleMusicConfig.fromResources(context) }
    var sessionKey by remember { mutableIntStateOf(0) }
    val session = remember(sessionKey) { AppleMusicPairingSession.create() }
    val pairingUrl =
        remember(config.pairingBaseUrl, session) {
            buildAppleMusicPairingUrl(config.pairingBaseUrl, session)
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
                    text = "手机扫码",
                    active = config.hasCustomPairingBridge
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
                        if (config.hasCustomPairingBridge) {
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

            if (pairingUrl != null) {
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
                        content = pairingUrl,
                        contentDescription = "Apple Music 配对二维码",
                        modifier = Modifier.size(qrSize)
                    )
                }
            } else {
                MissingPairingBackendCard(
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
                    text = "配对码 ${session.code}",
                    color = Color(0xFFEDEDED),
                    fontSize = if (compact) 11.sp else 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center
                )
                Text(
                    text =
                        if (pairingUrl != null) {
                            "二维码 5 分钟内有效"
                        } else {
                            "不会生成无效二维码"
                        },
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
private fun MissingPairingBackendCard(
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
            text = "未配置后端",
            color = Color(0xFFF1F1F1),
            fontSize = if (compact) 13.sp else 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
        Text(
            text = "设置 PairingBaseUrl 后再生成二维码",
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
