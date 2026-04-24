# Spotfurry

Spotfurry 是一个 `Wear OS` 手表端音乐播放器原型，目标不是照抄官方 Spotify 客户端，而是先做出一个适合圆形小屏的 `Spotify 风格播放体验`。

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

## 项目结构

```text
.
├── app
│   ├── build.gradle.kts
│   └── src/main
│       ├── AndroidManifest.xml
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

## 下一步建议

### 第一阶段

- 把假数据替换成真实的领域层与仓库层
- 增加专辑封面、播放进度条、收藏列表
- 引入持久化，保存最近播放和已固定歌单

### 第二阶段

- 接入 Spotify 账号登录
- 用 `Authorization Code with PKCE` 完成手表端或配对端授权
- 通过 Spotify Web API 获取用户资料、播放列表、当前播放状态和可用设备
- 优先把 Spotfurry 做成 `腕上遥控器 + 轻浏览器`，比一开始就强做完整流媒体更现实

### 第三阶段

- 评估是否支持离线缓存、自有音源或代理播放链路
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

## 关于 Spotify 接入

如果你的目标是“真的连 Spotify 账号”，要注意两件事：

- 鉴权路线应优先走 Spotify 官方的 `Authorization Code with PKCE`
- 真实播放能力、设备切换、目录访问和版权相关限制，需要严格按 Spotify 官方能力边界来设计

更稳的实现顺序是：

1. 先把 Spotfurry 做成一个完整的手表播放器外壳。
2. 再接 Spotify 登录和播放状态同步。
3. 最后评估哪些能力适合放在手表独立完成，哪些更适合走手机或远端设备协同。
