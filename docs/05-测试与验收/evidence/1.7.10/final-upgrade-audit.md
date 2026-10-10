# Gramophone 1.7.10 最终升级兼容与手势审查

结论：本次限定范围的只读静态审查未发现新增 P0、P1 或可确认的 P2 问题。未发现需要阻断当前交付的代码问题；这不替代最终 APK 安装、签名兼容、完整回归及 course 真机验收。

审计时间：2026-10-10 21:53 CST。审计环境：本机文件与 Git，只读检查；未运行 Gradle、ADB 或修改工程。基准 HEAD：`a45284663f929e5161f6b3eff770c9b78184f6d5`；审计对象为该 HEAD 上的未提交 1.7.10／versionCode 43 改动。

## 1. 风险总览

| 审查项 | 结论 | 证据 |
| --- | --- | --- |
| 分类稳定 ID | 使用分类资源 ID，未改为位置 ID；移除 FileSystem 后不再包含其 ID | ViewPager2Adapter.kt:96–114、125、134 |
| 旧配置兼容 | 保留枚举名称；仅映射过滤，启动读取不会写回旧配置 | ViewPager2Adapter.kt:117–147；TabOrderPreference.kt:53–55、108–115 |
| 旧配置只显示 FileSystem | 分隔符变为最前时将 Songs 移到前面，至少一个可见页面 | ViewPager2Adapter.kt:139–143 |
| 控件事件边界 | Button、ImageButton、TimeBar 和 clickable 控件排除；仅 enabled play/pause 允许双击 | VideoGesturePlayerView.kt:57–74 |
| 旧手势候选清理 | 排除控件 DOWN、失焦和 detach 发送 CANCEL；UP/CANCEL 清除当前拦截标记 | VideoGesturePlayerView.kt:41–54、76–93 |
| 简洁日志与隐私 | 新增日志只有播放请求状态、播放器是否绑定及命令可用性；没有路径、媒体名、坐标或 URI | VideoGesturePlayerView.kt:27–35 |

上述路径均位于仓库 `/Users/lizhihang/Documents/Codex/2026-08-03/referenced-chatgpt-conversation-this-is-an/work/Gramophone/app/src/main/java/org/akanework/gramophone/`，XML 位于同仓库 `app/src/main/res/`。

## 2. 审计范围和未覆盖范围

范围：相对基准 HEAD 的三个正式源码／资源变更、版本号变更，以及分页、偏好编辑、媒体库分类、播放调用和日志存储的相关调用链；只读复核 InteractionBehaviorTest 和 HomeTabsRegressionTest 的覆盖内容。

未覆盖：全仓库审计、最终 APK 反编译、签名对比、安装升级、SDK 36 真机再次运行、course 真机、测试执行结果、旧版本 Fragment 保存状态的跨 APK 恢复、控制栏动画过渡的真实手势和辅助功能操作。

## 3. 技术栈和模块概览

Android／Kotlin、Media3 PlayerView／GestureDetector、ViewPager2／FragmentStateAdapter、SharedPreferences；回归代码使用 Robolectric。手势触发仍调用现有 `Player.playOrPause()`；播放权威状态由 Player／服务持有。

## 4. 严重问题清单

无新增可确认 P0／P1 问题。

## 5. 功能逻辑问题清单

审计结论：未发现本次新增的可确认功能问题。

已检查内容：默认四入口，FileSystem 在配置解析及补全两处均排除；可见与隐藏分类顺序；只显示 FileSystem 的回退；非控件区域、enabled play/pause、其他控件与禁用控件；双击调用只在有媒体项和播放命令可用时发生。

未发现问题：未见移除入口后重新补回 FileSystem、生成零页面，或对 disabled play/pause 触发播放切换。`HomeTabsRegressionTest.kt:19–54` 对默认、旧顺序、单入口与实际页面映射存在断言，但本审查未运行该测试。

仍存在的限制：静态分析无法证明每种真实设备布局、动画时点和系统手势均符合预期。

## 6. 安全问题清单

审计结论：新增日志没有新增可确认的敏感信息泄露问题。

已检查内容：两个事件 details 只有布尔／空状态；DiagnosticStore.kt:31–34 设置数量／字节上限，44–68 异步保存，161–162 使用应用私有 filesDir，183–194 裁剪存储。

未发现问题：未新增媒体标题、路径、content URI、触摸坐标或网络上传。

仍存在的限制：未进行 APK／运行日志扫描；此结论限定于新增手势事件与现有存储调用链。

## 7. 性能与稳定性问题清单

审计结论：未发现新增可确认的性能或稳定性问题。

已检查内容：控件命中递归规模受当前 View 层级限制；事件保存使用既有异步执行器；取消事件创建后 recycle。

未发现问题：未新增主线程文件写入或持续触摸轨迹记录。

仍存在的限制：未进行压力测试和设备性能采样。

## 8. 架构与可维护性问题清单

审计结论：当前改动沿用现有分页、播放与诊断责任边界。

已检查内容：未新增播放状态副本；保留旧枚举与偏好序列化协议；外部速度／返回控件仍是 PlayerView 的兄弟节点，原动作分别打开对话框与 finish（VideoPlayerActivity.kt:40–45）。

未发现问题：未发现需要为本次交付扩大重构范围的问题。

仍存在的限制：只审查当前任务涉及的调用链。

## 9. 数据一致性问题清单

审计结论：启动映射没有写回或清除偏好。确认编辑标签顺序时按现有流程写回新顺序是用户明确操作，与自动迁移不同。

已检查内容：`tabs` 读取和映射、TabOrderPreference 的初始化与保存、LibraryTreeLoader.kt:53–57 的可见分类映射。

未发现问题：未发现数据删除、媒体库写入或自动偏好覆盖。

仍存在的限制：未实际做跨 APK 保留数据升级。

## 10. 修复优先级路线图

当前无审查发现需要追加代码修复。继续完成已规划的最终回归与交付核验，不扩展修改范围。

## 11. 上线阻断项

本次限定静态审查未发现代码阻断项。最终回归结果和 APK 交付核验由主任务依据实际结果单独判断，不在本报告预判通过。

## 12. 建议观察项

仅保留三个实际验收边界：保留旧配置的 APK 升级；控制栏显示／隐藏及过渡时点的真机操作；course 包真实安装与播放。无新增无关优化建议。

## 13. 可接受的残余风险

Android 原生 GestureDetector 的双击时距和空间阈值仍适用。生命周期取消已具备实现，但本审查没有单独执行失焦／detach 自动化或真机验证。用户此前对 local 临时 gesture-check 包的确认属于主任务传入事实，不等于最终正式 APK／course 已验收。

## 14. 最终审计结论

未发现本次 1.7.10 改动引入的实质性代码问题。已执行 `git diff --check`，退出码 0。未运行测试，未声称当前每 Flavor 120 项通过，未声称 course 真机通过。该报告可作为最终交付的静态审查证据，需与主任务取得的真实测试及产物证据共同使用。
