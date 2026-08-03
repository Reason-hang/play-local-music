# 本地听歌的发布自动化说明

本目录保存 Android 发布任务的自动化配置。产品名称统一为“本地听歌”；上游仓库名称和应用技术包名仍保留在脚本中，用于兼容既有发布流程。

## 常用任务

```sh
[bundle exec] fastlane android test
```

运行自动化测试。

```sh
[bundle exec] fastlane android buildrel
```

构建发布版本。

```sh
[bundle exec] fastlane android preprel
```

准备发布所需的元数据。

```sh
[bundle exec] fastlane android googleplay
```

发布到 Google Play；执行前必须确认签名、账号和发布权限。

```sh
[bundle exec] fastlane android gitrel
```

构建并推送 Git 发布版本；执行前必须确认远程仓库和发布范围。

更多 fastlane 用法请参阅 [fastlane 官方文档](https://docs.fastlane.tools/)。
