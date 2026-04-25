# Spotfurry

Spotfurry 是一个 `Wear OS` 手表端音乐播放器原型，目标不是照抄手机端播放器，而是先做出一个适合圆形小屏的 `流媒体播放体验`。

当前仓库已经初始化为一个可继续扩展的首版工程骨架，重点先放在三个问题：

1. 手表上怎么快速浏览内容。
2. 手表上怎么快速回到“正在播放”。
3. 小屏幕里怎么把队列、资料库、播放控制串成一条顺手的交互链。

## 当前已实现

- `Wear OS + Kotlin + Compose for Wear OS` 项目结构
- 首页 `Home`，展示当前播放和推荐曲目
- 播放页 `Now Playing`，包含播放/暂停、上一首、下一首、喜欢、随机、重复、音量调节
- 资料库页 `Library`，可切换不同的预置歌单队列
- 队列页 `Queue`，可直接点选队列中的歌曲
- 本地假数据与播放器状态模型，已经形成一个可交互的闭环
- Apple Music 实验页，可检测本地 MusicKit SDK、发起登录并测试 catalog 歌曲播放
- Apple Music 扫码登录原型，手表显示二维码和短码，手机端后续可通过配对后端完成授权
- Spotify WebView 实验页，可用 Web Playback SDK 尝试把手表注册为 Spotify Connect 播放设备

## 项目结构

```text
.
├── app
│   ├── build.gradle.kts
│   ├── libs
│   │   └── README.md
│   └── src/main
│       ├── AndroidManifest.xml
│       ├── java/com/weifurry/spotfurry/data/applemusic
│       ├── java/com/weifurry/spotfurry/presentation
│       │   ├── components
│       │   ├── model
│       │   ├── navigation
│       │   ├── player
│       │   ├── routes
│       │   ├── MainActivity.kt
│       │   └── theme
│       └── res
├── gradle
│   └── libs.versions.toml
├── build.gradle.kts
└── settings.gradle.kts
```

## 设计取向

- 以手表场景优先，不按手机思路硬缩小
- 把“正在播放”做成最高优先级入口
- 页面结构尽量少层级，避免用户在小屏幕里迷路
- 先把本地状态模型跑通，再接真实鉴权和在线播放控制
- 真实平台播放优先验证 Apple Music，因为 MusicKit for Android 更接近“App 内直接播放完整曲库”的目标

## 下一步建议

### 第一阶段

- 在真机手表上验证 Apple MusicKit SDK 是否能登录和在线播放
- 把假数据替换成真实的领域层与仓库层
- 增加专辑封面、真实播放进度、收藏列表

### 第二阶段

- 接入 Apple Music catalog / library 数据
- 将首页播放按钮切到真实 MusicKit 播放状态
- 处理未登录、未订阅、SDK 不可用、播放失败等状态

### 第三阶段

- 评估 Spotify Web Playback SDK WebView 实验模式
- 评估本地音频 / 自有音源的 Media3 离线播放模式
- 增加 Tile、Complication、通知控制和语音入口
- 针对圆屏和方屏分别优化布局

## 开发说明

当前工程已经包含 Gradle Wrapper，可直接用仓库内的 `./gradlew` 构建。当前本机已验证 `debug APK` 可以编译通过。

你本地继续推进时建议：

1. 用 Android Studio 打开本项目。
2. 安装 `JDK 17+` 与最新的 Wear OS SDK。
3. 执行 `./gradlew assembleDebug --stacktrace` 验证构建。
4. 先跑 Wear OS 模拟器确认 UI 与导航，再开始接真实播放能力。

## 自动构建

仓库已经添加 GitHub Actions 工作流：

- 文件位置：`.github/workflows/android-build.yml`
- 触发方式：`push 到 main`、`pull request`、手动 `workflow_dispatch`
- 构建内容：`debug APK`
- 产物位置：Actions 页面中的 `spotfurry-debug-apk`

当前工作流安装 `JDK 17` 和 `Android SDK 36`，然后使用仓库内的 Gradle Wrapper 执行构建。

## Apple Music 实验模式

当前仓库已经预留 Apple Music 实验播放入口：

- 页面入口：`音乐库` -> `Apple Music 实验`
- 扫码入口：`音乐库` -> `Apple Music 实验` -> `手机扫码登录`
- 默认扫码后端：`https://spotfurry-auth.weifurry-c80.workers.dev`
- SDK 位置：`app/libs/`
- 配置方式：使用本机 Gradle 属性，不要写入仓库内的 `gradle.properties`
- 构建策略：没有 Apple MusicKit SDK 文件时仍可正常编译，实验页会显示 SDK 未安装
- 扫码策略：没有配置真实配对后端时，手表端不会生成二维码，避免扫到无法解析的占位域名

本机测试时建议把配置放到 `~/.gradle/gradle.properties`：

```properties
spotfurry.appleMusicDeveloperToken=你的短期 Apple Music developer token
spotfurry.appleMusicTestSongId=用于测试播放的 Apple Music catalog song ID
spotfurry.appleMusicAuthBaseUrl=https://你的 SpotfurryAuth Worker
```

也可以临时用命令行传入：

```sh
./gradlew :app:assembleDebug \
  -Pspotfurry.appleMusicDeveloperToken=... \
  -Pspotfurry.appleMusicTestSongId=... \
  -Pspotfurry.appleMusicAuthBaseUrl=...
```

注意事项：

- 不要提交 Apple `.p8` 私钥、developer token、music user token 或本地 SDK AAR。
- Apple MusicKit for Android SDK 需要从 Apple Developer 下载，放入 `app/libs/` 后由 App 运行时检测。
- 扫码登录会调用 SpotfurryAuth Worker 创建配对会话，并使用后端返回的 `pairUrl` 生成二维码。
- `spotfurry.appleMusicAuthBaseUrl` 必须是手机能访问到的真实 `https://` 地址，不能使用 `.invalid`、内网不可达地址或未部署的域名。
- 当前 Apple Music 集成是实验骨架，真正能否在 Wear OS 真机上播放，需要放入 SDK、配置 token 后实测。
- Apple Music 可以通过 MusicKit 在线播放，但不应把 Apple Music 歌曲下载或缓存成本地文件。

## Spotify Web Playback SDK WebView 实验模式

当前仓库已经加入 Spotify WebView 实验入口：

- 页面入口：`音乐库` -> `Spotify WebView 实验`
- 技术路线：在 Wear OS WebView 内加载 Spotify Web Playback SDK，把手表尝试注册为 `Spotfurry WebView` 播放设备
- 配置方式：使用本机 Gradle 属性，不要写入仓库内的 `gradle.properties`
- 账号要求：需要 Spotify Premium
- 权限要求：测试 token 至少需要 `streaming`、`user-read-email`、`user-read-private`、`user-modify-playback-state`、`user-read-playback-state`
- 风险说明：Wear OS WebView 对 DRM、后台播放、电量和音频焦点的支持不一定稳定，所以这仍然是实验模式

本机测试时建议把配置放到 `~/.gradle/gradle.properties`：

```properties
spotfurry.spotifyWebPlaybackAccessToken=你的短期 Spotify OAuth access token
spotfurry.spotifyWebPlaybackUri=spotify:track:你的测试曲目ID
```

也可以临时用命令行传入：

```sh
./gradlew :app:assembleDebug \
  -Pspotfurry.spotifyWebPlaybackAccessToken=... \
  -Pspotfurry.spotifyWebPlaybackUri=spotify:track:...
```

注意事项：

- 不要提交 Spotify access token、refresh token、client secret 或任何个人账号配置。
- `spotifyWebPlaybackAccessToken` 是短期 OAuth token，过期后需要重新生成并重新安装或改成后端下发。
- `spotifyWebPlaybackUri` 可以留空；留空时页面只把手表注册为播放设备，需要你在 Spotify 客户端里手动选择 `Spotfurry WebView`。
- 如果 WebView 里显示账号限制、鉴权失败或无法发声，优先确认账号是否为 Premium、token scope 是否完整、Spotify 当前播放是否已转移到手表设备。
- 公开 API 下更稳的产品形态仍然是 `Web API 数据读取 + Spotify Connect 控制`；在 Spotfurry 内直接响 Spotify 音乐只适合继续做 WebView 实验。
