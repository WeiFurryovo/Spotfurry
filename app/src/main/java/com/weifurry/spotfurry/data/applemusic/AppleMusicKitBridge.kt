package com.weifurry.spotfurry.data.applemusic

import android.content.Context
import android.content.Intent
import java.lang.reflect.Proxy

internal class AppleMusicKitBridge {
    private var mediaPlayerController: Any? = null
    private var controllerTokenKey: String? = null

    fun checkAvailability(): AppleMusicKitAvailability {
        val missingClasses =
            listOf(
                AUTHENTICATION_FACTORY_CLASS,
                TOKEN_PROVIDER_CLASS,
                MEDIA_PLAYER_CONTROLLER_FACTORY_CLASS,
                CATALOG_QUEUE_BUILDER_CLASS,
                PLAYBACK_QUEUE_ITEM_PROVIDER_CLASS,
                MEDIA_ITEM_TYPE_CLASS
            ).filterNot(::isClassAvailable)

        return AppleMusicKitAvailability(
            authenticationAvailable =
                AUTHENTICATION_FACTORY_CLASS !in missingClasses &&
                    TOKEN_PROVIDER_CLASS !in missingClasses,
            playbackAvailable =
                MEDIA_PLAYER_CONTROLLER_FACTORY_CLASS !in missingClasses &&
                    CATALOG_QUEUE_BUILDER_CLASS !in missingClasses &&
                    PLAYBACK_QUEUE_ITEM_PROVIDER_CLASS !in missingClasses &&
                    MEDIA_ITEM_TYPE_CLASS !in missingClasses,
            missingClasses = missingClasses
        )
    }

    fun createAuthenticationIntent(
        context: Context,
        developerToken: String
    ): Result<Intent> =
        runCatching {
            require(developerToken.isNotBlank()) { "缺少 Apple Music developer token" }

            val authManager = createAuthenticationManager(context)
            val builder =
                authManager.javaClass
                    .getMethod("createIntentBuilder", String::class.java)
                    .invoke(authManager, developerToken)

            builder.javaClass
                .getMethod("setStartScreenMessage", String::class.java)
                .invoke(builder, "连接 Apple Music，在 Spotfurry 中测试在线播放")

            builder.javaClass.getMethod("build").invoke(builder) as Intent
        }.mapFailure("无法创建 Apple Music 登录 Intent")

    fun handleAuthenticationResult(
        context: Context,
        intent: Intent?
    ): Result<String> =
        runCatching {
            requireNotNull(intent) { "Apple Music 登录没有返回数据" }

            val authManager = createAuthenticationManager(context)
            val tokenResult =
                authManager.javaClass
                    .getMethod("handleTokenResult", Intent::class.java)
                    .invoke(authManager, intent)

            val isError = tokenResult.javaClass.getMethod("isError").invoke(tokenResult) as Boolean
            if (isError) {
                val error = tokenResult.javaClass.getMethod("getError").invoke(tokenResult)
                error("Apple Music 登录失败：$error")
            }

            val musicUserToken =
                tokenResult.javaClass
                    .getMethod("getMusicUserToken")
                    .invoke(tokenResult) as String
            require(musicUserToken.isNotBlank()) { "Apple Music 没有返回 music user token" }
            musicUserToken
        }.mapFailure("无法处理 Apple Music 登录结果")

    fun playCatalogSong(
        context: Context,
        developerToken: String,
        musicUserToken: String,
        songId: String
    ): Result<Unit> =
        runCatching {
            require(developerToken.isNotBlank()) { "缺少 Apple Music developer token" }
            require(musicUserToken.isNotBlank()) { "缺少 Apple Music music user token，请先登录" }
            require(songId.isNotBlank()) { "缺少 Apple Music 测试歌曲 ID" }

            val controller =
                ensureController(
                    context = context,
                    developerToken = developerToken,
                    musicUserToken = musicUserToken
                )
            val queueProvider = createCatalogSongQueueProvider(songId)
            val queueProviderClass = Class.forName(PLAYBACK_QUEUE_ITEM_PROVIDER_CLASS)

            controller.javaClass
                .getMethod("prepare", queueProviderClass, Boolean::class.javaPrimitiveType)
                .invoke(controller, queueProvider, true)
            Unit
        }.mapFailure("无法播放 Apple Music 测试歌曲")

    fun resume(): Result<Unit> =
        runControllerCommand("play", "无法继续 Apple Music 播放")

    fun pause(): Result<Unit> =
        runControllerCommand("pause", "无法暂停 Apple Music 播放")

    fun skipNext(): Result<Unit> =
        runControllerCommand("skipToNextItem", "无法跳到下一首 Apple Music")

    fun skipPrevious(): Result<Unit> =
        runControllerCommand("skipToPreviousItem", "无法回到上一首 Apple Music")

    fun release() {
        mediaPlayerController?.runCatchingUnit("release")
        mediaPlayerController = null
        controllerTokenKey = null
    }

    private fun createAuthenticationManager(context: Context): Any {
        val authFactory = Class.forName(AUTHENTICATION_FACTORY_CLASS)
        return requireNotNull(
            authFactory
                .getMethod("createAuthenticationManager", Context::class.java)
                .invoke(null, context.applicationContext)
        ) {
            "Apple Music authentication manager 创建失败"
        }
    }

    private fun ensureController(
        context: Context,
        developerToken: String,
        musicUserToken: String
    ): Any {
        val tokenKey = "$developerToken::$musicUserToken"
        if (mediaPlayerController != null && controllerTokenKey == tokenKey) {
            return mediaPlayerController!!
        }

        release()

        val tokenProviderClass = Class.forName(TOKEN_PROVIDER_CLASS)
        val tokenProvider =
            Proxy.newProxyInstance(
                tokenProviderClass.classLoader,
                arrayOf(tokenProviderClass)
            ) { proxy, method, args ->
                when (method.name) {
                    "getDeveloperToken" -> developerToken
                    "getUserToken" -> musicUserToken
                    "toString" -> "SpotfurryAppleMusicTokenProvider"
                    "hashCode" -> System.identityHashCode(proxy)
                    "equals" -> proxy === args?.firstOrNull()
                    else -> null
                }
            }

        val controllerFactory = Class.forName(MEDIA_PLAYER_CONTROLLER_FACTORY_CLASS)
        mediaPlayerController =
            controllerFactory
                .getMethod("createLocalController", Context::class.java, tokenProviderClass)
                .invoke(null, context.applicationContext, tokenProvider)
        controllerTokenKey = tokenKey
        return mediaPlayerController!!
    }

    private fun createCatalogSongQueueProvider(songId: String): Any {
        val builderClass = Class.forName(CATALOG_QUEUE_BUILDER_CLASS)
        val builder = builderClass.getConstructor().newInstance()
        val songItemType = Class.forName(MEDIA_ITEM_TYPE_CLASS).getField("SONG").getInt(null)

        val configuredBuilder =
            builderClass
                .getMethod(
                    "items",
                    Int::class.javaPrimitiveType,
                    Array<String>::class.java
                )
                .invoke(builder, songItemType, arrayOf(songId))

        val queueProvider = configuredBuilder.javaClass.getMethod("build").invoke(configuredBuilder)
        return requireNotNull(queueProvider) { "Apple Music 队列创建失败" }
    }

    private fun runControllerCommand(
        methodName: String,
        errorPrefix: String
    ): Result<Unit> =
        runCatching {
            val controller = requireNotNull(mediaPlayerController) { "Apple Music 播放控制器尚未创建" }
            controller.javaClass.getMethod(methodName).invoke(controller)
            Unit
        }.mapFailure(errorPrefix)

    private fun isClassAvailable(className: String): Boolean =
        runCatching { Class.forName(className) }.isSuccess

    private fun Any.runCatchingUnit(methodName: String) {
        runCatching {
            javaClass.getMethod(methodName).invoke(this)
        }
    }

    private fun <T> Result<T>.mapFailure(prefix: String): Result<T> =
        fold(
            onSuccess = { Result.success(it) },
            onFailure = { Result.failure(IllegalStateException("$prefix：${it.message}", it)) }
        )

    private companion object {
        const val AUTHENTICATION_FACTORY_CLASS =
            "com.apple.android.sdk.authentication.AuthenticationFactory"
        const val TOKEN_PROVIDER_CLASS =
            "com.apple.android.sdk.authentication.TokenProvider"
        const val MEDIA_PLAYER_CONTROLLER_FACTORY_CLASS =
            "com.apple.android.music.playback.controller.MediaPlayerControllerFactory"
        const val CATALOG_QUEUE_BUILDER_CLASS =
            "com.apple.android.music.playback.queue.CatalogPlaybackQueueItemProvider\$Builder"
        const val PLAYBACK_QUEUE_ITEM_PROVIDER_CLASS =
            "com.apple.android.music.playback.queue.PlaybackQueueItemProvider"
        const val MEDIA_ITEM_TYPE_CLASS =
            "com.apple.android.music.playback.model.MediaItemType"
    }
}
