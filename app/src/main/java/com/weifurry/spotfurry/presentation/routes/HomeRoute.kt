package com.weifurry.spotfurry.presentation.routes

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Text
import com.weifurry.spotfurry.presentation.components.PrimaryPlayerButton
import com.weifurry.spotfurry.presentation.components.ProgressRing
import com.weifurry.spotfurry.presentation.components.SecondaryPlayerButton
import com.weifurry.spotfurry.presentation.components.SmallIconBubble
import com.weifurry.spotfurry.presentation.components.StatusPill
import com.weifurry.spotfurry.presentation.player.SpotfurryState

@Composable
internal fun HomeRoute(
    state: SpotfurryState,
    onOpenNowPlaying: () -> Unit,
    onOpenLibrary: () -> Unit,
    onOpenQueue: () -> Unit
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
                                    Color(0xFF171717),
                                    Color(0xFF060606),
                                    Color(0xFF000000)
                                )
                        )
                    )
        ) {
            val compact = maxWidth < 220.dp
            val mainSize = if (compact) 58.dp else 66.dp
            val sideSize = if (compact) 34.dp else 38.dp
            val utilitySize = if (compact) 22.dp else 24.dp
            val progressRingSize = if (compact) 72.dp else 80.dp
            val sideOffset = if (compact) 62.dp else 74.dp
            val transportOffsetY = if (compact) 24.dp else 34.dp
            val utilityRowOffsetY = if (compact) 46.dp else 48.dp
            val utilitySpacing = if (compact) 10.dp else 14.dp
            val topPadding = if (compact) 18.dp else 24.dp

            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
            ) {
                SmallIconBubble(
                    icon = Icons.Filled.LibraryMusic,
                    onClick = onOpenLibrary,
                    contentDescription = "打开音乐库",
                    size = 28.dp,
                    iconSize = 12.dp,
                    modifier =
                        Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = topPadding + 10.dp, end = 32.dp),
                    bubbleColor = Color(0xFF1D1D1D),
                    borderColor = Color(0xFF2A2A2A),
                    iconTint = Color(0xFFDCDCDC)
                )

                Column(
                    modifier =
                        Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth(0.72f)
                            .padding(top = topPadding)
                            .clickable(onClick = onOpenNowPlaying),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    StatusPill(
                        text = if (state.isPlaying) "正在播放" else "已暂停",
                        active = state.isPlaying
                    )
                    Text(
                        text = state.currentTrack.title,
                        modifier = Modifier.padding(top = 7.dp),
                        fontSize = if (compact) 17.sp else 20.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        color = Color(0xFFF2F2F2)
                    )
                    Text(
                        text = state.currentTrack.artist,
                        modifier = Modifier.padding(top = 3.dp),
                        fontSize = if (compact) 11.sp else 12.sp,
                        color = Color(0xFFB5B5B5),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }

                Box(
                    modifier =
                        Modifier
                            .align(Alignment.Center)
                            .fillMaxSize()
                            .offset(y = transportOffsetY)
                ) {
                    ProgressRing(
                        progress = state.progress,
                        modifier =
                            Modifier
                                .align(Alignment.Center)
                                .size(progressRingSize)
                    )

                    PrimaryPlayerButton(
                        icon = if (state.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        onClick = state::togglePlayPause,
                        contentDescription = if (state.isPlaying) "暂停" else "播放",
                        modifier = Modifier.align(Alignment.Center),
                        size = mainSize
                    )

                    SecondaryPlayerButton(
                        icon = Icons.Filled.SkipPrevious,
                        onClick = state::skipPrevious,
                        contentDescription = "上一首",
                        size = sideSize,
                        modifier =
                            Modifier
                                .align(Alignment.Center)
                                .offset(x = -sideOffset)
                    )

                    SecondaryPlayerButton(
                        icon = Icons.Filled.SkipNext,
                        onClick = state::skipNext,
                        contentDescription = "下一首",
                        size = sideSize,
                        modifier =
                            Modifier
                                .align(Alignment.Center)
                                .offset(x = sideOffset)
                    )

                    Row(
                        modifier =
                            Modifier
                                .align(Alignment.Center)
                                .offset(y = utilityRowOffsetY)
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(utilitySpacing),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SmallIconBubble(
                            icon = Icons.AutoMirrored.Filled.VolumeUp,
                            onClick = { state.changeVolume(5) },
                            contentDescription = "音量加大",
                            size = utilitySize,
                            iconSize = if (compact) 14.dp else 15.dp,
                            bubbleColor = Color(0xFF1D1D1D),
                            borderColor = Color(0xFF2A2A2A),
                            iconTint = Color(0xFFD0D0D0)
                        )
                        SmallIconBubble(
                            icon = Icons.Filled.Shuffle,
                            onClick = state::toggleShuffle,
                            contentDescription = if (state.shuffleEnabled) "关闭随机播放" else "开启随机播放",
                            size = utilitySize,
                            iconSize = if (compact) 14.dp else 15.dp,
                            highlighted = state.shuffleEnabled,
                            bubbleColor = Color(0xFF1D1D1D),
                            borderColor = Color(0xFF2A2A2A),
                            iconTint = if (state.shuffleEnabled) Color.White else Color(0xFFD0D0D0)
                        )
                        SmallIconBubble(
                            icon = Icons.AutoMirrored.Filled.QueueMusic,
                            onClick = onOpenQueue,
                            contentDescription = "打开播放队列",
                            size = utilitySize,
                            iconSize = if (compact) 14.dp else 15.dp,
                            bubbleColor = Color(0xFF1D1D1D),
                            borderColor = Color(0xFF2A2A2A),
                            iconTint = Color(0xFFD0D0D0)
                        )
                    }
                }
            }
        }
    }
}
