此目录只保存 debug/test 构建使用的公开固定签名。

`spotfurry-debug.keystore` 是为了让本地构建和 GitHub Actions 产出的 `debug APK` 使用同一个签名，方便测试时直接覆盖安装，不再因为签名变化需要先卸载旧版本。

不要把 release 签名、真实上架签名或任何私有 keystore 放到这里。正式发布时应使用单独的私有 release 签名，并通过本机配置或 CI secrets 注入。
