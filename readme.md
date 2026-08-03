# 本地听歌：安卓本地音乐播放器 MVP

本地听歌是基于上游开源项目 `FoedusProgramme/Gramophone` `beta` 分支定制的本地音乐播放器。当前版本面向红米 K80 Pro、HyperOS 和 Android 16，重点验收本地 MP3 播放。

本项目保留上游播放器架构、技术包名和类名，以确保安装更新兼容；用户可见产品名称、应用内名称、APK文件名和本项目文档统一使用“本地听歌”。

## 产品范围

- 使用系统媒体库扫描本地歌曲，首版重点支持 MP3。
- 支持立即播放、顺序播放、循环播放和随机播放。
- 保留源码已有的队列、歌单、后台播放、锁屏媒体控制和蓝牙媒体按键能力。
- 界面沿用原有布局，用户可见文案统一为简体中文。
- 不新增应用内年份标签、永久忽略或定制队列恢复功能。

## 离线处理

本版本采用方案 B：

- 删除 `INTERNET`、`ACCESS_NETWORK_STATE` 和图片读取权限。
- 关闭在线歌词、在线封面、外链更新和无用功能入口。
- 不全面删除底层网络、歌词、封面和均衡器代码及其依赖。
- 核心扫描、播放和队列流程不以网络为前提；飞行模式下的运行结果以测试记录为准。

## 构建

环境要求：JDK 21、Android SDK Platform 36.1、Build Tools 36，以及已初始化的 `media3` 和 `hificore` 子模块。

```bash
JAVA_HOME=/path/to/jdk-21 \
ANDROID_HOME=/path/to/android-sdk \
./gradlew --no-configuration-cache --no-parallel :app:assembleNonMinifiedRelease
```

构建输出的文件名前缀为 `本地听歌-`。本地交付使用 Android 默认 debug 签名，仅用于安装试用；正式发布时必须替换为自己的 release keystore。

## 文档

- [文档总索引](./docs/00-文档总索引.md)
- [MVP范围与验收](./docs/01-产品文档/01-MVP范围与验收.md)
- [源码基线与离线边界](./docs/02-架构文档/01-源码基线与离线边界.md)
- [构建与交付](./docs/04-开发文档/01-构建与交付.md)
- [自主决策记录](./docs/04-开发文档/02-自主决策记录.md)
- [交付说明](./docs/04-开发文档/03-交付说明.md)
- [验证记录](./docs/05-测试与验收/01-验证记录.md)
- [静态离线检查](./docs/05-测试与验收/02-静态离线检查.md)

## 当前验证口径

已完成本地构建、APK签名检查、权限静态检查和自动化测试记录。当前没有连接红米 K80 Pro，因此不能把 HyperOS 后台、电量策略、锁屏、蓝牙和飞行模式结果写成真机通过；详细证据和未覆盖风险见测试文档。

## 许可证

上游项目及本次修改遵循 GNU General Public License v3.0，详见仓库中的 [LICENSE](./LICENSE)。上游项目地址为 [FoedusProgramme/Gramophone](https://github.com/FoedusProgramme/Gramophone)。
