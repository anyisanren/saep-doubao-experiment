# 交接整理核验

核验日期：2026-10-26（Asia/Shanghai）。本次只整理仓库、读取已有产物并离线构建，没有操作实验手机、重新下达豆包任务或重新判定轮次。

| 项目 | 结果 |
| --- | --- |
| 正式实验物料 | 24 个 SLAB APK，包含 22 个初始变体和 DYN 的静态 v7 / v9 包 |
| 历史物料 | 8 个 Agent 实验室 APK；当前 app/ 对应 0.3 系列源码 |
| 冻结 APK | 32 个均通过 Android apksigner 签名验证；使用同一实验签名证书 |
| 归档哈希 | 32 个 APK 的大小、SHA-256、嵌入策略、提取策略文件及 SHA256SUMS 清单一致 |
| 原始策略对应 | 22 个初始 SLAB APK 的策略与 slab/src/ 对应变体逐字节一致；lo / hi 另列历史内容 |
| SLAB 构建 | build-slab.sh 成功构建全部 22 个 Release 变体并运行各自 Lint；22 份报告均 0 errors，每份 11 warnings |
| 早期应用构建 | build.sh 的 4 个基线 Release 和 SECURITY_RESTRICTED=1 的 2 个收紧 Release 均成功；Lint 0 errors，保留现有 warnings |
| 脚本 | Python 语法解析与 Shell 语法检查通过；设备环境变量转发用 mock 核验，不执行 adb |
| 证据保留 | 原 runs/ 的 1357 个文件在整理前后逐字节 SHA-256 一致 |
| 公开文件范围 | 原始 runs/、artifacts/、本机 SDK / 缓存、签名私钥、设备序列号均不在提交内容中 |

新构建输出位于本地 dist/，归档 apks/ 中的既有 APK 未被覆盖。Lint 警告不等于实测轮次判定，本次没有以构建成功替代设备行为验证。

根 README 是新的交接入口；早期 README 保存在 legacy-agent-lab.md。docs/results/ 是从原报告生成的公开副本，只增加来源说明并调整本地证据链接的展示，不修改判定。source-hashes.json 保存原始报告的相对路径与 SHA-256。

复核命令（仓库根目录）：

~~~sh
python3 scripts/verify-apks.py
./scripts/build-slab.sh
./scripts/build.sh
SECURITY_RESTRICTED=1 ./scripts/build.sh
~~~

前三类构建要求 README 所列 JDK / SDK；首次构建可能下载 Gradle 与依赖。冻结 APK 的离线哈希核验只需 Python 标准库。
