package com.weifurry.spotfurry.presentation.routes

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberTransformingLazyColumnState
import androidx.wear.compose.material3.EdgeButton
import androidx.wear.compose.material3.EdgeButtonSize
import androidx.wear.compose.material3.ScreenScaffold
import androidx.wear.compose.material3.Text
import com.weifurry.spotfurry.data.applemusic.AppleMusicConfig
import com.weifurry.spotfurry.data.applemusic.AppleMusicKitAvailability
import com.weifurry.spotfurry.data.applemusic.AppleMusicKitBridge
import com.weifurry.spotfurry.data.applemusic.AppleMusicTokenStore
import com.weifurry.spotfurry.presentation.components.ActionRowButton
import com.weifurry.spotfurry.presentation.components.ListSectionHeader
import com.weifurry.spotfurry.presentation.components.TrackCard

@Composable
internal fun AppleMusicRoute(
    onOpenLibrary: () -> Unit,
    onOpenPairingLogin: () -> Unit
) {
    val context = LocalContext.current
    val config = remember(context) { AppleMusicConfig.fromResources(context) }
    val tokenStore = remember { AppleMusicTokenStore(context) }
    val bridge = remember { AppleMusicKitBridge() }
    var availability by remember { mutableStateOf(bridge.checkAvailability()) }
    var hasMusicUserToken by remember { mutableStateOf(tokenStore.hasMusicUserToken) }
    var statusMessage by remember {
        mutableStateOf("先检查 SDK 和本地配置，然后在真机上登录 Apple Music。")
    }

    fun refreshAvailability() {
        availability = bridge.checkAvailability()
        hasMusicUserToken = tokenStore.hasMusicUserToken
    }

    fun setResultMessage(result: Result<Unit>, successMessage: String) {
        statusMessage =
            result.fold(
                onSuccess = { successMessage },
                onFailure = { it.message ?: "Apple Music 操作失败" }
            )
        refreshAvailability()
    }

    val authLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) {
                statusMessage = "Apple Music 登录已取消或未完成。"
                refreshAvailability()
                return@rememberLauncherForActivityResult
            }

            val tokenResult = bridge.handleAuthenticationResult(context, result.data)
            statusMessage =
                tokenResult.fold(
                    onSuccess = { musicUserToken ->
                        tokenStore.musicUserToken = musicUserToken
                        "Apple Music 登录成功，可以尝试播放测试歌曲。"
                    },
                    onFailure = { it.message ?: "无法完成 Apple Music 登录" }
                )
            refreshAvailability()
        }

    DisposableEffect(Unit) {
        onDispose {
            bridge.release()
        }
    }

    val listState = rememberTransformingLazyColumnState()

    ScreenScaffold(
        scrollState = listState,
        edgeButton = {
            EdgeButton(
                onClick = onOpenLibrary,
                buttonSize = EdgeButtonSize.ExtraSmall
            ) {
                Text("返回")
            }
        }
    ) { contentPadding ->
        TransformingLazyColumn(
            state = listState,
            contentPadding = contentPadding
        ) {
            item(key = "apple-music-header") {
                ListSectionHeader("Apple Music")
            }
            item(key = "apple-music-status") {
                TrackCard(
                    title = "实验播放模式",
                    body = statusBody(config, availability, hasMusicUserToken, statusMessage),
                    onClick = {}
                )
            }
            item(key = "check-sdk") {
                ActionRowButton(
                    label = "检查 SDK",
                    detail = availabilityDetail(availability),
                    onClick = {
                        refreshAvailability()
                        statusMessage = availability.label
                    }
                )
            }
            item(key = "pairing-login") {
                ActionRowButton(
                    label = "手机扫码登录",
                    detail =
                        if (config.hasAuthBackend) {
                            "连接 SpotfurryAuth，手机完成 Apple Music 授权"
                        } else {
                            "先配置 spotfurry.appleMusicAuthBaseUrl"
                        },
                    onClick = onOpenPairingLogin
                )
            }
            item(key = "apple-login") {
                ActionRowButton(
                    label = "登录 Apple Music",
                    detail =
                        if (config.hasDeveloperToken) {
                            "打开 MusicKit 登录授权"
                        } else {
                            "先配置 spotfurry.appleMusicDeveloperToken"
                        },
                    onClick = {
                        bridge
                            .createAuthenticationIntent(context, config.developerToken)
                            .fold(
                                onSuccess = authLauncher::launch,
                                onFailure = {
                                    statusMessage = it.message ?: "无法启动 Apple Music 登录"
                                }
                            )
                        refreshAvailability()
                    }
                )
            }
            item(key = "play-test-song") {
                ActionRowButton(
                    label = "播放测试歌曲",
                    detail =
                        if (config.hasTestSongId) {
                            "使用 MusicKit Media Playback 播放 ${config.testSongId}"
                        } else {
                            "先配置 spotfurry.appleMusicTestSongId"
                        },
                    onClick = {
                        setResultMessage(
                            result =
                                bridge.playCatalogSong(
                                    context = context,
                                    developerToken = config.developerToken,
                                    musicUserToken = tokenStore.musicUserToken,
                                    songId = config.testSongId
                                ),
                            successMessage = "已请求 Apple Music 播放测试歌曲。"
                        )
                    }
                )
            }
            item(key = "resume") {
                ActionRowButton(
                    label = "继续播放",
                    detail = "调用 MusicKit 播放控制器",
                    onClick = {
                        setResultMessage(
                            result = bridge.resume(),
                            successMessage = "已请求 Apple Music 继续播放。"
                        )
                    }
                )
            }
            item(key = "pause") {
                ActionRowButton(
                    label = "暂停播放",
                    detail = "调用 MusicKit 播放控制器",
                    onClick = {
                        setResultMessage(
                            result = bridge.pause(),
                            successMessage = "已请求 Apple Music 暂停播放。"
                        )
                    }
                )
            }
            item(key = "next") {
                ActionRowButton(
                    label = "下一首",
                    detail = "测试 MusicKit 队列控制",
                    onClick = {
                        setResultMessage(
                            result = bridge.skipNext(),
                            successMessage = "已请求 Apple Music 下一首。"
                        )
                    }
                )
            }
            item(key = "previous") {
                ActionRowButton(
                    label = "上一首",
                    detail = "测试 MusicKit 队列控制",
                    onClick = {
                        setResultMessage(
                            result = bridge.skipPrevious(),
                            successMessage = "已请求 Apple Music 上一首。"
                        )
                    }
                )
            }
            item(key = "clear-login") {
                ActionRowButton(
                    label = "清除登录",
                    detail = if (hasMusicUserToken) "移除本机 music user token" else "当前没有保存登录 token",
                    onClick = {
                        tokenStore.clear()
                        bridge.release()
                        hasMusicUserToken = false
                        statusMessage = "已清除 Apple Music 登录状态。"
                    }
                )
            }
        }
    }
}

private fun statusBody(
    config: AppleMusicConfig,
    availability: AppleMusicKitAvailability,
    hasMusicUserToken: Boolean,
    statusMessage: String
): String =
    listOf(
        statusMessage,
        "SDK：${availability.label}",
        "Developer token：${if (config.hasDeveloperToken) "已配置" else "未配置"}",
        "Music user token：${if (hasMusicUserToken) "已登录" else "未登录"}",
        "扫码后端：${if (config.hasAuthBackend) "已配置" else "未配置"}",
        "测试歌曲 ID：${if (config.hasTestSongId) config.testSongId else "未配置"}"
    ).joinToString(separator = "\n")

private fun availabilityDetail(availability: AppleMusicKitAvailability): String =
    if (availability.sdkAvailable) {
        "Authentication 与 Media Playback SDK 均已检测到"
    } else {
        "缺少：${availability.missingClasses.joinToString(separator = ", ")}"
    }
