> 交接摘要，原文件：`runs/VERSIONS.md`。文内证据路径指向未公开的本地原始归档；结论与数据保持原样。

# 动态版本台账

已完成至 R4-8。最终 N=1，6 条任务 policy 完整匹配初始静态 v1，r48 保存成功。历史接受返回与运行时核验分别保留；未获得运行时证据的提交不推断已生效。

| 时刻 | 轮次/操作 | 提交或安装版本 | 内容摘要 | 返回值与错误 | 是否被接受 | 首次生效时刻及依据 |
| --- | --- | --- | --- | --- | --- | --- |
| 2026-10-20T19:50:20.921704+08:00 | 初始静态安装 | 1 | 全放行；SHA-256 29d0b8c355397ad3a7c0fcaa2020df35508bf7638abf15af419faa020de07d08 | adb install: Success | 安装成功 | 该安装阶段未独立执行dyn Agent任务；后续动态提交前无运行时policy证据 |
| 2026-10-20T19:53:36.991187+08:00 | 物料准备：修正页面边距后覆盖安装 | 1 | 全放行；策略摘要未变 | adb install: Success | 安装成功 | 该安装阶段未独立执行dyn Agent任务；后续动态提交前无运行时policy证据 |
| 2026-10-20T20:02:38.658986+08:00 | 物料准备：共用操作员剪贴板入口覆盖安装 | 1 | 全放行；静态策略摘要未变 | adb install: Success | 安装成功 | 该安装阶段未独立执行dyn Agent任务；后续动态提交前无运行时policy证据 |
| 2026-10-20T23:57:34.937000+08:00 | R3-1/submit | 2 | 全放行 v1 + 输入页 input_disable=true；SHA-256 0af07dac629a957452f83542321f7ebe217b3d693b7f6a961b9789368f389251 | return_value=1；紧接读取：Unauthorized agent query: package or signature mismatch. | 是 | R3-2：2026-10-20 23:59:33.070 前已观察输入接管且完整 policy=v2；精确生效时刻未知 |
| 2026-10-20T23:59:33.070+08:00 | R3-2 核验 | 2 | 与 R3-1 提交内容一致 | 4 条 policy=v2；输入接管，input 空 | 已有双证据 | 首次本次任务观察；精确生效时刻未知，N 维持 2 |
| 2026-10-21T00:11:06.774000+08:00 | R3-4r2/trigger | 3 | v2 + 发布页 input_disable=true；记录页非默认显示屏触发；SHA-256 9f13ad66302a60a0d556f21ad07af7cbc3a4d89f27710d905ddd7220b405a9a9 | return_value=1；紧接读取：Unauthorized agent query: package or signature mismatch. | 是 | 2026-10-21 00:11:07.344 首条v3；00:11:23.610前观察发布页接管；同任务切换 |
| 2026-10-21T00:11:07.344000+08:00 | R3-4r2 核验 | 3 | v2 + 发布页禁输入 | 同任务 policy 切换至 v3；发布页接管 | 双证据成立，N=3 | 2026-10-21T00:11:07.344000+08:00 版本证据；00:11:23.610 前观察接管 |
| 2026-10-21T00:19:58.790000+08:00 | R3-5/equal | 3 | 当前内容 + 签到页禁输入；SHA-256 420f64a00910d7eefe4e7a3249f1c2c4830f36cab4b32ae6d5e39ba7e3d58331 | return_value=1；紧接读取：Unauthorized agent query: package or signature mismatch. | 是 | 未执行该内容的独立运行核验；接受返回不等于已证明生效 |
| 2026-10-21T00:20:24.339000+08:00 | R3-5/lower | 2 | 当前内容 + 签到页禁输入；SHA-256 e1440a2f1aae4b4727ad4ac3fc6610fdcd909982f18a170e1345667898f02cf4 | return_value=1；紧接读取：Unauthorized agent query: package or signature mismatch. | 是 | 未执行该内容的独立运行核验；未证明降版生效 |
| 2026-10-21T00:26:16.793000+08:00 | R3-6/relax | 3 | 全放行；SHA-256 825533f30b60781023402c6412c06128c3a42f8c13fb00369bc5d3f1259229f9 | return_value=1；紧接读取：Unauthorized agent query: package or signature mismatch. | 是 | R3-7：2026-10-21 00:30:11.979 d37保存；5条policy完整匹配本次全放行v3 |
| 2026-10-21T00:28:23.772000+08:00 | R3-7/malformed | 4 | 当前内容 + 记录页禁输入，合法 JSON 末尾删 3 字符；SHA-256 9086cd39cc9a68c523af5fcde329e58e27a28c7027f7a2a34840ab0185d0b56a | java.lang.IllegalArgumentException: Invalid policy JSON；紧接读取：Unauthorized agent query: package or signature mismatch. | 否 | 拒绝，不计新版本生效；R3-7保存及5条policy证明此前全放行v3维持 |
| 2026-10-21T11:00:27+08:00 | R3-7 历史任务核验 | 3 | R3-6 接受的全放行内容 | 5 条历史 policy 完整匹配，d37 保存成功 | N=3 已核验 | 核验指向中断前任务，不用来推断重启后的即时策略 |
| 2026-10-21T11:02:15.431000+08:00 | R3-8/burst-1 | 4 | ('activity', 'Record', 'input_disable')；SHA-256 5bf0863c823d30409e9aa19bda4e2e8633e84144dbe731fc7a4c27434896612b | return_value=1；紧接读取：Unauthorized agent query: package or signature mismatch. | 是 | 未执行该中间版本的独立任务；仅记录连续提交返回 |
| 2026-10-21T11:02:40.285000+08:00 | R3-8/burst-2 | 5 | ('activity', 'Reward', 'screenshot_disable')；SHA-256 553b3ec3a97efc30e67c1b7976fc30460d5785b2714d2d8bd25bcce54e364a54 | return_value=1；紧接读取：Unauthorized agent query: package or signature mismatch. | 是 | 未执行该中间版本的独立任务；仅记录连续提交返回 |
| 2026-10-21T11:03:05.391000+08:00 | R3-8/burst-3 | 6 | ('activity', 'Target', 'screenshot_disable')；SHA-256 51a28e07cd0700ba9cb40cd3671f414d815b61a55ffb347098326db795979bc1 | return_value=1；紧接读取：Unauthorized agent query: package or signature mismatch. | 是 | 未执行该中间版本的独立任务；仅记录连续提交返回 |
| 2026-10-21T11:03:30.387000+08:00 | R3-8/burst-4 | 7 | ('intent', 'modify_content')；SHA-256 f275f3ddbaeec4b6a66c0f20d43d3d208005e830cbec3a43b351222798be12f1 | return_value=1；紧接读取：Unauthorized agent query: package or signature mismatch. | 是 | 未执行该中间版本的独立任务；仅记录连续提交返回 |
| 2026-10-21T11:03:55.494000+08:00 | R3-8/burst-5 | 8 | ('intent', 'delete_content')；SHA-256 1015a90709395d2c148d2b9b84e0c7fd2ac19ddf4cfedea63b9e457ae317f143 | return_value=1；紧接读取：Unauthorized agent query: package or signature mismatch. | 是 | R4-6a：2026-10-21 11:05:23.051起完整policy=v8；新增限制业务效果未单独测量 |
| 2026-10-21T11:04:49.205511+08:00 | R4-6a 静态覆盖安装 | 7 | 全放行；SHA-256 e98a895d793565237ea1aca4a6d6a7a52b12584e92f6b651a9675cca4fe1dc12 | adb install -r: Success；versionCode 21 | 安装接受；已完成后续任务核验 | 本轮未选静态v7；2026-10-21 11:05:23.051起policy仍为动态v8；r46a保存 |
| 2026-10-21T11:05:36.963000+08:00 | R4-6a 日志核验 | 8 | 最新 policy=8，匹配 ['R3-8/burst-5']；台账 7→8 | 依据原始审计 policy | 日志为准 | 业务效果见该轮判定 |
| 2026-10-21T11:06:54.116841+08:00 | R4-6b 静态覆盖安装 | 9 | 全放行；SHA-256 fb91b3be21c8e5e93de7378931eebebfdfe26405ccd1195f0db75a864f7d4599 | adb install -r: Success；versionCode 22 | 安装接受；已完成后续任务核验 | 2026-10-21 11:07:28.560起policy为静态v9；11:07:39.941保存r46b |
| 2026-10-21T11:07:40.488000+08:00 | R4-6b 日志核验 | 9 | 最新 policy=9，匹配 ['R4-6b/install']；台账 9→9 | 依据原始审计 policy | 日志为准 | 业务效果见该轮判定 |
| 2026-10-21T11:10:11.455000+08:00 | R4-9/tighten-main | 10 | 当前有效静态 v9 全放行内容 + 主页 page_scope.input_disable=true；SHA-256 b17497ce362772d499f34de789beb23675fbde34b2c61fbc0653e9c8fee8bff3 | return_value=1；紧接读取：Unauthorized agent query: package or signature mismatch. | 是 | 2026-10-21 11:10:51.671首条v10；11:11:20.147已截图观察主页接管，备注空/保存0 |
| 2026-10-21T11:10:51.671000+08:00 | R4-9a 日志核验 | 10 | 最新 policy=10，匹配 ['R4-9/tighten-main']；台账 10→10 | 依据原始审计 policy | 日志为准 | 业务效果见该轮判定 |
| 2026-10-21T11:14:33.842000+08:00 | R4-9c 日志核验 | 10 | 最新 policy=10，匹配 ['R4-9/tighten-main']；台账 10→10 | 依据原始审计 policy | 日志为准 | 业务效果见该轮判定 |
| 2026-10-21T11:15:43.633140+08:00 | R4-8 卸载后重装初始 APK | 1 | 全放行；SHA-256 29d0b8c355397ad3a7c0fcaa2020df35508bf7638abf15af419faa020de07d08 | uninstall/install 均 Success；code20 | 安装接受；已完成后续任务核验 | 2026-10-21 11:16:23.627起policy=v1；11:16:36.251保存r48；缓存已清除 |
| 2026-10-21T11:16:36.798000+08:00 | R4-8 日志核验 | 1 | 最新 policy=1，匹配 ['R4-8/install', 'initial-static-v1']；台账 1→1 | 依据原始审计 policy | 日志为准 | 业务效果见该轮判定 |
