# 本地听歌

本地听歌是基于 `FoedusProgramme/Gramophone` 定制的离线 Android 本地媒体播放器，面向红米 K80 Pro、HyperOS 和 Android 16。它沿用成熟的 Media3、MediaSession 和前台媒体服务架构，重点支持本地 MP3，以及 MP4 容器中的 AAC 音轨与完整视频画面。

当前交付版本为 `1.3.3`（`versionCode 27`）的 SelfBuilt APK。它仍使用可更新旧包的 `.debug` 包名与默认 debug 签名，但已关闭 `debuggable`、StrictMode 弹窗、LeakCanary 和开发者启动入口。`nonMinifiedRelease` 已在本机构建通过；商店级正式签名仍需持有者提供私钥，详见[验证记录](./docs/05-测试与验收/01-验证记录.md)。

## 目录

- [当前能力](#当前能力)
- [离线与隐私](#离线与隐私)
- [构建](#构建)
- [文档](#文档)
- [签名说明](#签名说明)

## 当前能力

- MediaStore 扫描本地 MP3 和 MP4+A​​AC；有视频轨的 MP4 在完整播放器显示画面。
- MP3/MP4 共用播放队列、歌单、后台播放、锁屏通知和蓝牙媒体控制。
- 队列存在上一项时，任意入口的“上一首”均直接切换上一项；只有队列第一项才重播本项。
- 播放速度使用 `0.75x`、`1x`、`1.3x`、`1.5x`、`1.7x`、`2x` 六档单选预设；默认不锁定速度和音高。
- “设置 → 诊断与日志”可查看私有崩溃摘要、主动导出 ZIP、清除记录；没有强制邮件弹窗。
- 安装包仅保留一个“本地听歌”启动图标，并使用主应用图标；不包含 `Reflections` 或 `Leaks` 开发工具入口。
- 用户可见中文语言环境使用简体中文；不重做上游页面结构。

## 离线与隐私

- 不声明 `INTERNET` 或 `ACCESS_NETWORK_STATE`，核心播放不依赖网络。
- 不申请图片读取权限；仅请求读取本地音乐和视频的最小权限。
- 不自动上传日志；诊断数据只保存在应用私有目录，只有用户点击导出才会调用系统分享。
- 为控制改动风险，歌词、封面和均衡器的底层上游实现暂未全量清除，但可见入口已关闭。

## 构建

```bash
JAVA_HOME=/path/to/jdk \
ANDROID_HOME=/path/to/android-sdk \
ANDROID_SDK_ROOT=/path/to/android-sdk \
./gradlew --console=plain :app:assembleDebug
```

详细环境、签名和校验方法见[构建与签名](./docs/04-开发文档/01-构建与签名.md)。

## 文档

完整交付、架构、决策和验证资料见[文档总索引](./docs/00-文档总索引.md)。

## 签名说明

当前内部交付变体使用 Android 默认 debug 签名，适合个人安装与诊断，且已显式关闭 `debuggable`；它仍不等同于商店正式发布签名。正式发布必须由持有者提供并保管 release keystore，并验证从已安装包到正式签名包的升级路径。

## 许可证

本项目及上游修改遵循 [GNU General Public License v3.0](./LICENSE)。上游项目地址：[FoedusProgramme/Gramophone](https://github.com/FoedusProgramme/Gramophone)。
