# 阶段二开发进度

## 2026-05-13

- 已完成 Task 1：建立阶段二数据模型与配置入口。
- 新增 `ContextSettings`、`ContextWindow`、`ContextConfigRepository`。
- `ChatViewModel` 已接入上下文配置读取入口，但当前发送逻辑还没有切到阶段二上下文管理链路。
- 已补充最小 `JUnit4` 单元测试依赖，并新增 `ContextSettingsTest`。
- 已执行 `.\gradlew.bat :app:testDebugUnitTest`，当前通过。
- 已完成 Task 2-4：实现 `PromptAssembler`、`DefaultContextManager`、`SummaryGenerator`、`NoOpSummaryGenerator`，对应单元测试已通过。
- 已完成 Task 5-7 的第一版主链路：引擎支持 `getCurrentConfig()`、`rebuildConversation()`、`replayMessages()` 接口，发送前可触发上下文压缩判断、Conversation 重建、回放失败降级日志。
- `replayMessages()` 已升级为基于 LiteRT-LM `ConversationConfig.initialMessages` 的原生回放实验实现，不再只是占位降级。
- 已完成 Task 8：设置页新增“上下文窗口大小”入口，可在模型配置页选择保留轮数并持久化。
- 多次执行 `.\gradlew.bat :app:assembleDebug` 成功，当前进入真机部署与长对话验证阶段。
- 真机已验证：应用可启动、模型可进入 `Ready`、可发送至少两轮真实对话、聊天页与设置页可正常切换。
- 真机当前阻塞：ADB 自动输入受到系统拼音输入法组合态影响，连续批量发消息不稳定，导致长对话压缩路径尚未在真机上稳定触发。
- Task 9 当前结论：代码侧已切到原生 `initialMessages` 回放实验方案。
- 2026-05-13 真机阶段二专项验证通过：
- 设置项 `N=5` 生效，`shouldCompress()` 阈值实际为 `20`。
- 真机长对话在 `messageCount=21` 起成功触发压缩。
- 修复后 `Conversation` 重建成功，不再出现 “A session already exists”。
- 最近消息回放成功，日志显示 `initialMessages=10`。
- 追问“我们刚才聊了什么”时，模型成功回忆出关键信息：电竞竞猜、明天早上 9 点、5 人参与。
- 2026-05-13 阶段二补齐版已重新部署到真机，规则摘要器与降级摘要注入代码已生效，单测与编译通过。
- 本轮真机补测结果：`retainedRounds=10` 初始加载后，实际发送前阈值日志为 `threshold=20`，说明运行时设置已切到 `N=5`。
- 本轮日志确认：`summaryEmpty=false`，说明被裁剪历史已生成非空规则摘要。
- 本轮日志确认：`Conversation` 重建成功，且最近消息回放成功，未触发降级摘要注入。
- 本轮人工反馈：上下文管理“没什么问题”，界面可继续操作，未出现明显卡死。
- 但本轮日志只覆盖到 `messageCount=19` 后触发首次压缩，并非严格的 `30+` 条消息专项验收；若要完全对齐阶段二清单，仍需补一轮 `30+` 条真实消息记录。
- 2026-05-13 已补齐 `30+` 条真机长对话专项：日志确认从 `12` 持续到 `30` 的过程中，多次触发压缩。
- 本轮 `30+` 专项中，每次压缩均表现为：
- `summaryEmpty=false`，说明规则摘要持续生成成功。
- `Conversation` 重建完成。
- `最近消息回放成功: initialMessages=10`。
- 最新真机专项说明：在长对话连续推进到 `30` 条后，应用未崩溃、未 ANR，聊天页仍可继续交互。
- 阶段二当前结论：主链路、规则摘要、压缩重建、最近消息回放、`30+` 条长对话稳定性、UI 可操作性均已具备明确证据。
- 2026-05-13 已完成阶段三文档准备：
- 新增 `docs/plans/2026-05-13-stage3-memory-system-design.md`
- 新增 `docs/plans/2026-05-13-stage3-memory-system-plan.md`
- 阶段三方向确定为“完整阶段三覆盖，但按后端主链路先行、UI 随后收口”的实现策略。
