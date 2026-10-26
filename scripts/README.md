# 脚本与交接入口

## 离线构建与校验

| 脚本 | 输入与结果 |
| --- | --- |
| build-env.sh | 构建共享环境；使用 JDK / SDK，缺少本地签名时生成实验密钥 |
| build-slab.sh | 构建 22 个 SLAB Release、逐变体 Lint、签名核验；输出 dist/slab-rebuilt/ |
| build.sh | 构建早期 app/；SECURITY_RESTRICTED=1 选择收紧条件 |
| verify-apks.py | 只读验证 apks/ 的数量、大小、SHA-256、嵌入策略和校验清单 |
| serve-apk.sh | 将本地 dist/ 发布为 8765 端口 HTTP 下载目录，不是实验执行器 |

新机器先按根 README 配置 JAVA_HOME、ANDROID_SDK_ROOT；SDK 内需要 platforms/android-35 和 build-tools/34.0.0。Gradle Wrapper 已随仓库提供。构建输入使用保存的策略，不需要先执行 slab-policies.py。

## 正式 SLAB 实验工具

以下脚本是原实验执行工具，运行会访问设备或写入 runs/。本次交接整理没有执行它们。读代码或使用 --help 前也应注意：部分单轮脚本在模块顶层执行，没有统一的空运行模式。

| 脚本 | 作用 |
| --- | --- |
| slab-device.py | 公共设备工具与轮前准备、截图、持续日志；从 SAEP_SERIAL 读取设备编号 |
| slab-agent.py | compose 验证精确任务文本；send 单次下达并启动范围监视 |
| slab-scope-guard.py | 监测前台应用范围，越界时终止任务 |
| slab-watch.py / slab-ocr.swift | 截图 + macOS Vision OCR；watch 可点击指定测试应用的操作授权 |
| slab-cancel.py | 观察并取消接管任务，不代填业务结果 |
| slab-tools-ui.py / slab-submit.py | 操作主页协议工具，输入并提交完整策略 |
| slab-submission-round.py / slab-immediate.py | 特定动态提交轮次与即时检查衔接 |
| slab-dyn-task.py / slab-query-round.py | 动态组任务准备、查询轮次 |
| slab-collect.py / slab-log-stream.py | 收集导出、持续系统日志 |
| slab-policies.py | 重新生成 22 个条件的静态策略及 runs/build/policies.json；会改变 updated_at |
| slab-install-verify.py | 构建包覆盖安装、拉取已安装包、核对 Manifest / 资源 / DEX；会写 runs/p0/ |
| slab-static-install.py | R4-6 的单个静态策略构建与覆盖安装；会修改 dyn 源策略及台账 |
| slab-final-install.py | R4-8 的卸载、重装；锁定原始冻结 APK 摘要，不适用于自行重签名的新包 |
| slab-final-read.py | R4-8 收尾的单次策略读取 |
| slab-evidence.py / slab-dynamic-check.py | 从已有产物提取观测、策略及双证据 |
| slab-ledger.py / slab-reconcile.py | 依据提交与任务策略内容维护版本台账 |
| slab-record.py / slab-archive.py / slab-finalize.py | 生成或更新归档与判定；finalize 包含本次实验的固定断言，不是通用分析入口 |
| security-device.py / analyze-security.py | 早期 Agent 实验室的执行与汇总工具 |

设备变量：

~~~sh
export SAEP_SERIAL="<测试机的 adb 序列号>"
export ANDROID_SDK_ROOT="/path/to/android-sdk"
# 可选：单独覆盖 adb 路径
export SAEP_ADB="/path/to/adb"
~~~

slab-device.py、slab-install-verify.py 和 security-device.py 不再内置原设备序列号。共用模块会向依赖它的脚本传递设备选择。

## 重开实验时的输入

按 [v2 指令书](../docs/experiment-instructions-v2.md) 准备全新的本地 runs/。该目录的原始数据不在公开仓库；已有结论摘要在 docs/results/，不要把摘要当成可恢复全部执行状态的原始目录。

每轮至少按对应步骤准备 round.json、prompt.txt 及策略输入；动态组还依赖 runs/dynamic-ledger.json、runs/build/policies.json 和原始 DYN 策略。原指令的静态条件生成器能创建 policies.json，但不会自动生成所有单轮输入或动态台账。操作卡是流程记录，不是可直接运行的命令集合。

用于原 R4-8 的冻结包在 apks/slab/dyn-static-v1.apk，对应策略在 apks/policies/slab/dyn-static-v1.json。原单轮脚本仍按 runs/build/apks/ 和 runs/build/dyn-original-policy.json 读取，复用前应按原指令在新的实验归档中准备对应文件。APK-lo / APK-hi 的历史内容位于同一 apks/ 目录；新实验应根据当时有效 N 构建，不照搬历史 v7/v9。

当前自动化面向原设备与豆包 UI。OCR 使用 macOS Vision；部分按钮识别还包含屏幕坐标阈值。协议查询本身走应用中的公开反射入口与指定 ContentProvider；不要把主机 UI 采集脚本的无障碍读取计入豆包步骤的测量。

## 原始统计口径

- 先按顶层 log_id 去重；不存在时按整条记录原文去重。嵌套 agent_decision.log_id 不是单条记录主键。
- 导出查询全包日志，必须再按任务时间窗口区分；不同导出文件中出现同一记录不表示新事件。
- 逐层解析 agent_decision、policy 等 JSON 字符串，以完整策略内容识别两次 v3。
- submission-result.at_ms 记录在调用前，immediate_read.at_ms 在提交返回之后、查询之前；没有单独提交返回时刻，不能将两者直接当作精确接受时间。
- 一次性归档脚本反映原执行过程；离线补算的最终口径以 docs/results/LATENCY.md 和 FIELDS.md 为准。
