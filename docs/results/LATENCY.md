> 交接摘要，原文件：`runs/supplement/LATENCY.md`。文内证据路径指向未公开的本地原始归档；结论与数据保持原样。

# 动态策略生效延迟补算

所有输入均为 runs/ 已有产物；不改动原轮次判定。四个事件均缺少“提交返回接受”的独立时间字段，不能把调用前 at_ms、紧接读取时间或观测写入时间直接标作 T提交。

| 事件 | T提交 | T旧末 | T新首 | 延迟下限（秒） | 延迟上限（秒） | 行为证据 | 数据来源（文件与字段） | 结论 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| E1 | 返回瞬间未记录；调用前 1792512666774；紧接读取开始 1792512666776；返回位于 [1792512666774, 1792512666776] | 无 | 1792512667344 | 0.000 | 精确上限未知；上限在 [0.568, 0.570] | 后续发布页出现输入接管，发布队列为空 | `runs/R3-4r2/trigger-result.json`：at_ms / immediate_read.at_ms / return_value / policy；`runs/R3-4r2/export_D_com.saep.lab.dyn.json`：audit.records[16].timestamp / agent_decision.policy；`runs/R3-4r2/doubao-response.txt` | 无法验证（缺少独立提交返回时刻字段） |
| E2 | 返回瞬间未记录；调用前 1792511854937；紧接读取开始 1792511854946；返回位于 [1792511854937, 1792511854946] | 无 | 1792511960636 | 0.000 | 精确上限未知；上限在 [105.690, 105.699] | 后续输入页出现接管，未保存输入 | `runs/R3-1/submission-result.json`：at_ms / immediate_read.at_ms / return_value / policy；`runs/R3-2/export_D_com.saep.lab.dyn.json`：audit.records[10].timestamp / agent_decision.policy；`runs/R3-2/doubao-response.txt` | 无法验证（缺少独立提交返回时刻字段） |
| E3 | 返回瞬间未记录；调用前 1792513576793；紧接读取开始 1792513576796；返回位于 [1792513576793, 1792513576796] | 无 | 1792513800212 | 0.000 | 精确上限未知；上限在 [223.416, 223.419] | 全放行体现为输入保存成功；持久化保存回调 1792513811979（Unix ms），哈希匹配 d37；本事件测放宽，不是新增禁止 | `runs/R3-6/relax-result.json`：at_ms / immediate_read.at_ms / return_value / policy；`runs/R3-7/export_D_com.saep.lab.dyn.json`：audit.records[23].timestamp / agent_decision.policy；`runs/R3-7/doubao-response.txt` | 无法验证（缺少独立提交返回时刻字段） |
| E4 | 返回瞬间未记录；调用前 1792552211455；紧接读取开始 1792552211462；返回位于 [1792552211455, 1792552211462] | 无 | 1792552251671 | 0.000 | 精确上限未知；上限在 [40.209, 40.216] | 后续主页出现输入接管，备注空、保存0 | `runs/R4-9/tighten-main-result.json`：at_ms / immediate_read.at_ms / return_value / policy；`runs/R4-9a/export_D_com.saep.lab.dyn.json`：audit.records[1].timestamp / agent_decision.policy；`runs/R4-9a/doubao-response.txt` | 无法验证（缺少独立提交返回时刻字段） |

主测量 E1 的严格延迟区间无法验证；已有时间边界给出的保守包含区间为 **[0.000, 0.570] 秒**，按定义上限的实际值在 **[0.568, 0.570] 秒**。三个参考事件也缺独立返回时刻，保守上限分别为 **E2 105.699 秒、E3 223.419 秒、E4 40.216 秒**，按定义上限分别位于 [105.690, 105.699]、[223.416, 223.419]、[40.209, 40.216] 秒；这些界限不是精确传播延迟，不替代上表“无法验证”。

## 时间口径与缺失项

audit.records[].timestamp 是13位十进制 Unix 毫秒字符串，按 int(value) 转换；提交 at_ms、immediate_read.at_ms 与 observations.wall_time_ms 已是 Unix 毫秒整数，不加减时区偏移。差值除以1000并保留三位小数。未用主机 host-actions.at 或开机时长 uptime_ms 相减；截图仅作行为证据，不用主机截图时间算延迟。

已有字节码产物 `runs/p0/installed/base-dex.txt` 的 Protocol.submit（行6837–6912）显示 at_ms=currentTimeMillis() 在 ContentResolver.update 前，return_value 写入后调用 readPolicy，最后才写 dynamic_submission；readPolicy（行6492–6515）在查询前记录自身 at_ms。该P0文件仅用于任务A确认观测字段语义，不纳入任务B统计。四份提交结果及对应导出均没有独立的 update-return 时间字段；缺的是返回时刻数据，不是提交结果文件。

设调用前为 A、紧接读取开始为 B，则返回时刻 T 满足 A≤T≤B。各事件在A之后均未见旧策略记录，所以下限为0；按定义上限 U=T新首−T 只能约束在 [T新首−B, T新首−A]。保守包含区间采用最大的上限 T新首−A，不能伪称为按返回瞬间精确算出的上限。

- E1：调用前 1792512666774、紧接读取开始 1792512666776 来自 `runs/R3-4r2/trigger-result.json`；提交观测写入 1792512666782 来自 `runs/R3-4r2/export_D_com.saep.lab.dyn.json` 的 observations[event=dynamic_submission].wall_time_ms，该写入在紧接读取之后。T新首是 `runs/R3-4r2/export_D_com.saep.lab.dyn.json` 的 audit.records[16].timestamp 转为整数。行为来源为该导出的 business/observations 与 `runs/R3-4r2/doubao-response.txt`。
- E1 唯一旧v2记录时间 1792512663381 早于提交调用前时刻，不计入T旧末；新v3的完整内容包含发布页禁输入，未使用作废R3-4任务记录。
- E2：调用前 1792511854937、紧接读取开始 1792511854946 来自 `runs/R3-1/submission-result.json`；提交观测写入 1792511854948 来自 `runs/R3-1/export_S_com.saep.lab.dyn.json` 的 observations[event=dynamic_submission].wall_time_ms，该写入在紧接读取之后。T新首是 `runs/R3-2/export_D_com.saep.lab.dyn.json` 的 audit.records[10].timestamp 转为整数。行为来源为该导出的 business/observations 与 `runs/R3-2/doubao-response.txt`。
- E3：调用前 1792513576793、紧接读取开始 1792513576796 来自 `runs/R3-6/relax-result.json`；提交观测写入 1792513576798 来自 `runs/R3-6/export_S_com.saep.lab.dyn.json` 的 observations[event=dynamic_submission].wall_time_ms，该写入在紧接读取之后。T新首是 `runs/R3-7/export_D_com.saep.lab.dyn.json` 的 audit.records[23].timestamp 转为整数。行为来源为该导出的 business/observations 与 `runs/R3-7/doubao-response.txt`。
- E4：调用前 1792552211455、紧接读取开始 1792552211462 来自 `runs/R4-9/tighten-main-result.json`；提交观测写入 1792552211470 来自 `runs/R4-9/export_S_com.saep.lab.dyn.json` 的 observations[event=dynamic_submission].wall_time_ms，该写入在紧接读取之后。T新首是 `runs/R4-9a/export_D_com.saep.lab.dyn.json` 的 audit.records[1].timestamp 转为整数。行为来源为该导出的 business/observations 与 `runs/R4-9a/doubao-response.txt`。

## 去重、任务窗口与内容核对

按用户确认：只检查顶层log_id；本批均无该键，按每条审计对象的原始文本去重，不用嵌套agent_decision.log_id。先去重，再以该任务 round.json.dispatch_device_ms 至导出 wall_time_ms 筛选，排除全量导出中更早任务（含作废R3-4）的记录。下表是各事件各自选用日志文件的统计，不把各行相加当成全局去重数。

| 事件 | 日志文件 | 去重前全部 | 去重后全部 | 任务窗口agent | 任务窗口framework | 新策略匹配 |
| --- | --- | --- | --- | --- | --- | --- |
| E1 | `runs/R3-4r2/export_D_com.saep.lab.dyn.json` | 71 | 71 | 5 | 20 | 4 |
| E2 | `runs/R3-2/export_D_com.saep.lab.dyn.json` | 11 | 11 | 4 | 7 | 4 |
| E3 | `runs/R3-7/export_D_com.saep.lab.dyn.json` | 97 | 97 | 5 | 21 | 5 |
| E4 | `runs/R4-9a/export_D_com.saep.lab.dyn.json` | 152 | 152 | 1 | 3 | 1 |

依次解析外层导出、agent_decision JSON字符串、policy JSON字符串，以完整对象相等比较；不删除name、updated_at、package等字段。以下SHA-256来自完整解析对象按键排序、UTF-8、无多余空白、中文不转义的规范化序列化，因此可与原始字符串字节摘要不同；E1和E3的v3内容据此分别识别。

| 事件 | 版本 | 完整内容规范化SHA-256 | 匹配情况 |
| --- | --- | --- | --- |
| E1 | 3 | `be0a822a1de9e89a13f74f6fd7f12987b7ad735d04042cbaa85c9cbd97ea8529` | 4/5 条完整内容匹配；旧策略记录1条 |
| E2 | 2 | `a37662efe127496514f9b6a20b0d9aafa29b061aa28ef3a36b79c0ac78439f84` | 4/4 条完整内容匹配；旧策略记录0条 |
| E3 | 3 | `fceb6b30129a58c74d8dfbb2a18ccb93ffd135c42e18219a39ea15b73f32025d` | 5/5 条完整内容匹配；旧策略记录0条 |
| E4 | 10 | `0f23a49ccf75394b58f559b7347ff6efea29974f6206c633db2145fcdc925989` | 1/1 条完整内容匹配；旧策略记录0条 |
