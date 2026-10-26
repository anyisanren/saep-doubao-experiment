> 交接摘要，原文件：`runs/supplement/FIELDS.md`。文内证据路径指向未公开的本地原始归档；结论与数据保持原样。

# 日志字段全貌补算

## 1. 去重说明

按用户确认，只检查审计记录顶层log_id；本批2547条记录顶层均没有该键，因此从导出JSON原文截取每条audit.records[]对象的完整文本作为去重键，保留键顺序、值及内部空白，不以重新序列化结果替代原文。嵌套agent_decision.log_id不参与去重；每条去重记录保留全部导出来源。

| 来源 | 去重前 | 去重后 | 去除重复 |
| --- | --- | --- | --- |
| agent | 566 | 149 | 417 |
| framework | 1981 | 423 | 1558 |
| 合计 | 2547 | 572 | 1975 |

纳入以下41份正式轮次导出，含r2补测；排除作废R3-4的导出和P0产物，不把task-audit、查询结果或系统日志再计成独立记录。R2-10原轮判定为无法验证，导出为空，保留在清单。正式导出所包含的历史记录照常计入，包括作废R3-4任务时间窗内的7条agent与28条framework记录，不按其原任务有效性再删除（时间窗取该轮round.json的dispatch_device_ms至task_end_device_ms）。`runs/R4-2/field-inventory.json`仅交叉核对键名，不增加记录计数。

| 纳入文件 | agent去重前 | framework去重前 |
| --- | --- | --- |
| `runs/R1A/export_D_com.saep.lab.base.json` | 41 | 162 |
| `runs/R1A/export_H_com.saep.lab.base.json` | 36 | 141 |
| `runs/R1A-4/export_com.saep.lab.base.json` | 0 | 0 |
| `runs/R1B/export_D_com.saep.lab.a.json` | 6 | 9 |
| `runs/R1B/export_D_com.saep.lab.b.json` | 6 | 21 |
| `runs/R2-1/export_D_com.saep.lab.g21.json` | 0 | 0 |
| `runs/R2-10/export_D_com.saep.lab.g2a.json` | 0 | 0 |
| `runs/R2-10r2/export_D_com.saep.lab.g2a.json` | 3 | 2 |
| `runs/R2-11/export_D_com.saep.lab.g2b.json` | 0 | 0 |
| `runs/R2-11c/export_D_com.saep.lab.g2f.json` | 4 | 7 |
| `runs/R2-12/export_D_com.saep.lab.g2c.json` | 0 | 0 |
| `runs/R2-13/export_D_com.saep.lab.g2d.json` | 4 | 7 |
| `runs/R2-14/export_D_com.saep.lab.g2e.json` | 0 | 0 |
| `runs/R2-15/export_com.saep.lab.base.json` | 36 | 141 |
| `runs/R2-2/export_D_com.saep.lab.g22.json` | 0 | 0 |
| `runs/R2-3/export_D_com.saep.lab.g23.json` | 3 | 2 |
| `runs/R2-4a/export_D_com.saep.lab.g24a.json` | 4 | 4 |
| `runs/R2-4b/export_D_com.saep.lab.g24b.json` | 5 | 4 |
| `runs/R2-4c/export_D_com.saep.lab.g24c.json` | 4 | 7 |
| `runs/R2-5/export_D_com.saep.lab.g25.json` | 4 | 4 |
| `runs/R2-6/export_D_com.saep.lab.g26.json` | 7 | 20 |
| `runs/R2-7/export_D_com.saep.lab.g27.json` | 4 | 7 |
| `runs/R2-8/export_D_com.saep.lab.g28.json` | 7 | 15 |
| `runs/R2-9/export_D_com.saep.lab.g29.json` | 4 | 7 |
| `runs/R3-1/export_S_com.saep.lab.dyn.json` | 0 | 0 |
| `runs/R3-2/export_D_com.saep.lab.dyn.json` | 4 | 7 |
| `runs/R3-4r2/export_D_com.saep.lab.dyn.json` | 16 | 55 |
| `runs/R3-5/export_S_com.saep.lab.dyn.json` | 16 | 55 |
| `runs/R3-6/export_S_com.saep.lab.dyn.json` | 16 | 55 |
| `runs/R3-7/export_D_com.saep.lab.dyn.json` | 21 | 76 |
| `runs/R3-7/export_S_com.saep.lab.dyn.json` | 16 | 55 |
| `runs/R3-8/export_S_com.saep.lab.dyn.json` | 21 | 76 |
| `runs/R4-1/export_Q_com.saep.lab.base.json` | 41 | 162 |
| `runs/R4-3/export_D_com.saep.lab.g43.json` | 4 | 7 |
| `runs/R4-6a/export_D_com.saep.lab.dyn.json` | 26 | 96 |
| `runs/R4-6b/export_D_com.saep.lab.dyn.json` | 31 | 117 |
| `runs/R4-7/export_Q_com.saep.lab.base.json` | 41 | 162 |
| `runs/R4-8/export_D_com.saep.lab.dyn.json` | 39 | 138 |
| `runs/R4-9/export_S_com.saep.lab.dyn.json` | 31 | 117 |
| `runs/R4-9a/export_D_com.saep.lab.dyn.json` | 32 | 120 |
| `runs/R4-9c/export_D_com.saep.lab.dyn.json` | 33 | 123 |

逐层解析agent_identify、agent_decision字符串及其中policy字符串，并继续解析agent动作参数内的JSON字符串（130条均成功）；framework的126条参数是x=数字,y=数字文本，均不是JSON；149份身份、149份决策、149份policy均可解析。R4-2字段清单所有键均在全量样本中出现，全量framework另出现该清单未列的action_args。

## 2. 文档字段对照表

$为审计记录顶层，路径已穿透JSON字符串。分母固定为去重后agent 149、framework 423；键存在即计出现。null和空字符串分开计数，缺键不算空值。pkg_sha256的framework数量包含已注明别名package_sha256。对照结论按两类记录覆盖给出：一类缺键也记缺失并注明来源；“可空”不表示该键实测已出现；显式空值与别名优先列其对应结论。

用户提供的是取值示例，未给出封闭枚举约束；因此GUI_Action未列于示例、click与CLICK大小写不同等差异如实保留，不据此判为违法取值。本次无足够依据给出“取值与文档不符”。execute_id、instruction_id没有已证明的语义等价关系，不直接替换event_id/session_id。

| 字段 | 文档类别 | 文档可空 | agent记录中出现 | framework记录中出现 | 位置与键名 | 空值数 | 取值 | 对照结论 |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| `agent_name` | agent_identify | 否 | 149/149 (100.00%) | 423/423 (100.00%) | `$.agent_name`；同键名 | agent: null=0, 空串=0；framework: null=0, 空串=0 | 非空普通字符串（本批长度11） | 一致 |
| `agent_uuid` | agent_identify | 否 | 149/149 (100.00%) | 423/423 (100.00%) | `$.agent_identify.agent_uuid`；`$.agent_uuid`；同键名 | agent: null=0, 空串=0；framework: null=0, 空串=0 | 64位大写十六进制字符串 | 一致 |
| `pkg_sha256` | agent_identify | 否 | 149/149 (100.00%) | 423/423 (100.00%) | `$.agent_identify.pkg_sha256`；`$.package_sha256` | agent: null=0, 空串=0；framework: null=0, 空串=0 | 64位大写十六进制字符串；两类来源哈希值相同 | 改名或有替代字段（framework：package_sha256） |
| `agent_type` | agent_identify | 否 | 149/149 (100.00%) | 0/423 (0.00%) | `$.agent_identify.agent_type`；同键名 | agent: null=0, 空串=0；framework: null=0, 空串=0 | agent `AUTONOMOUS`: 149 | 缺失（framework 423条无键） |
| `trust_level` | agent_identify | 否 | 149/149 (100.00%) | 0/423 (0.00%) | `$.agent_identify.trust_level`；同键名 | agent: null=0, 空串=0；framework: null=0, 空串=0 | agent `TRUSTED_TEE`: 149 | 缺失（framework 423条无键） |
| `package_name` | agent_identify | 否 | 149/149 (100.00%) | 0/423 (0.00%) | `$.agent_identify.package_name`；同键名 | agent: null=0, 空串=0；framework: null=0, 空串=0 | 点分Android包名字符串 | 缺失（framework 423条无键） |
| `agent_version` | agent_identify | 是 | 149/149 (100.00%) | 0/423 (0.00%) | `$.agent_identify.agent_version`；同键名 | agent: null=0, 空串=0；framework: null=0, 空串=0 | 四段点分数字版本字符串 | 缺失（framework 423条无键） |
| `agent_md5` | agent_identify | 是 | 149/149 (100.00%) | 0/423 (0.00%) | `$.agent_identify.agent_md5`；同键名 | agent: null=0, 空串=0；framework: null=0, 空串=0 | 32位小写十六进制字符串 | 缺失（framework 423条无键） |
| `proof_signature` | agent_identify | 否 | 149/149 (100.00%) | 0/423 (0.00%) | `$.agent_identify.proof_signature`；同键名 | agent: null=0, 空串=0；framework: null=0, 空串=0 | 96字符Base64字符串（3种不同值） | 缺失（framework 423条无键） |
| `log_source` | agent_decision | 否 | 149/149 (100.00%) | 423/423 (100.00%) | `$.log_source`；同键名 | agent: null=0, 空串=0；framework: null=0, 空串=0 | agent `agent`: 149；framework `framework`: 423 | 一致 |
| `timestamp` | agent_decision | 否 | 149/149 (100.00%) | 423/423 (100.00%) | `$.timestamp`；同键名 | agent: null=0, 空串=0；framework: null=0, 空串=0 | 13位十进制数字字符串；int后为Unix毫秒 | 一致 |
| `event_id` | agent_decision | 否 | 0/149 (0.00%) | 0/423 (0.00%) | 所有已解析层均无此键 | agent: null=0, 空串=0；framework: null=0, 空串=0 | 无此键；execute_id仅在额外字段表作关联作用推断 | 缺失（两类均无此键） |
| `session_id` | agent_decision | 否 | 0/149 (0.00%) | 0/423 (0.00%) | 所有已解析层均无此键 | agent: null=0, 空串=0；framework: null=0, 空串=0 | 无此键；instruction_id/log_id仅在额外字段表作关联作用推断 | 缺失（两类均无此键） |
| `step_id` | agent_decision | 否 | 149/149 (100.00%) | 0/423 (0.00%) | `$.agent_decision.step_id`；同键名 | agent: null=0, 空串=19；framework: null=0, 空串=0 | 130条call_加24位字母数字；19条空字符串 | 文档标为不可空，但出现了空值 |
| `target_package` | agent_decision | 否 | 149/149 (100.00%) | 423/423 (100.00%) | `$.target_package`；同键名 | agent: null=0, 空串=0；framework: null=0, 空串=0 | 点分Android包名字符串 | 一致 |
| `target_activity` | agent_decision | 是 | 149/149 (100.00%) | 423/423 (100.00%) | `$.target_activity`；同键名 | agent: null=0, 空串=0；framework: null=0, 空串=0 | agent为完整类名；framework为ActivityRecord{… 包名/类名 t编号}字符串 | 一致 |
| `intent_type` | agent_decision | 是 | 149/149 (100.00%) | 0/423 (0.00%) | `$.intent_type`；同键名 | agent: null=0, 空串=0；framework: null=0, 空串=0 | agent `GUI_Action`: 149；示例非封闭枚举，不据此认定GUI_Action非法 | 缺失（framework 423条无键） |
| `decision_outcome` | agent_decision | 否 | 0/149 (0.00%) | 0/423 (0.00%) | 所有已解析层均无此键 | agent: null=0, 空串=0；framework: null=0, 空串=0 | 无任何取值 | 缺失（两类均无此键） |
| `user_authorization_type` | agent_decision | 是 | 0/149 (0.00%) | 0/423 (0.00%) | 所有已解析层均无此键 | agent: null=0, 空串=0；framework: null=0, 空串=0 | 无任何取值 | 缺失（两类均无此键） |
| `action_type` | agent_decision | 否 | 130/149 (87.25%) | 423/423 (100.00%) | `$.action_type`；`$.agent_decision.action_type`；同键名 | agent: null=0, 空串=0；framework: null=0, 空串=0 | agent `call_user`: 1；agent `click`: 63；agent `open_app`: 25；agent `stop`: 2；agent `take_notes`: 9；agent `type`: 13；agent `wait`: 17；framework `SCREENSHOT`: 297；framework `TOUCH_DOWN`: 63；framework `TOUCH_UP`: 63；click与示例CLICK大小写不同；保留原值，不把示例扩充为允许值全集 | 缺失（agent 19条无键） |
| `action_args` | agent_decision | 否 | 130/149 (87.25%) | 126/423 (29.79%) | `$.action_args`；`$.agent_decision.action_args`；同键名 | agent: null=0, 空串=0；framework: null=0, 空串=0 | agent为单元素数组，元素再解码为JSON参数数组；framework为x=数字,y=数字的普通文本，126条均不能作为JSON解码 | 缺失（agent 19条无键；framework 297条无键） |
| `policy` | agent_decision | 是 | 149/149 (100.00%) | 0/423 (0.00%) | `$.agent_decision.policy`；同键名 | agent: null=0, 空串=0；framework: null=0, 空串=0 | JSON字符串，再解析为对象，内部字段逐项列于第三部分 | 缺失（framework 423条无键） |
| `reason_code` | agent_decision | 是 | 0/149 (0.00%) | 0/423 (0.00%) | 所有已解析层均无此键 | agent: null=0, 空串=0；framework: null=0, 空串=0 | 无任何取值 | 缺失（两类均无此键） |
| `duration_ms` | agent_decision | 是 | 0/149 (0.00%) | 0/423 (0.00%) | 所有已解析层均无此键 | agent: null=0, 空串=0；framework: null=0, 空串=0 | 无此键；导出audit.duration_ns是整次查询耗时，不能替代单条记录duration_ms | 缺失（两类均无此键） |

枚举值明细如下；“收录轮次”指哪些正式导出含该值，一条去重记录可有多个来源，但次数只计一次，不据收录轮次推断实际执行轮次。缺键不补写成NULL枚举值，特别是reason_code缺失不同于取值为字符串NULL。

| 字段 | 来源 | 取值 | 条数 | 收录轮次 |
| --- | --- | --- | --- | --- |
| agent_type | agent | `AUTONOMOUS` | 149 | R1A, R1B, R2-10r2, R2-11c, R2-13, R2-15, R2-3, R2-4a, R2-4b, R2-4c, R2-5, R2-6, R2-7, R2-8, R2-9, R3-2, R3-4r2, R3-5, R3-6, R3-7, R3-8, R4-1, R4-3, R4-6a, R4-6b, R4-7, R4-8, R4-9, R4-9a, R4-9c |
| trust_level | agent | `TRUSTED_TEE` | 149 | R1A, R1B, R2-10r2, R2-11c, R2-13, R2-15, R2-3, R2-4a, R2-4b, R2-4c, R2-5, R2-6, R2-7, R2-8, R2-9, R3-2, R3-4r2, R3-5, R3-6, R3-7, R3-8, R4-1, R4-3, R4-6a, R4-6b, R4-7, R4-8, R4-9, R4-9a, R4-9c |
| log_source | agent | `agent` | 149 | R1A, R1B, R2-10r2, R2-11c, R2-13, R2-15, R2-3, R2-4a, R2-4b, R2-4c, R2-5, R2-6, R2-7, R2-8, R2-9, R3-2, R3-4r2, R3-5, R3-6, R3-7, R3-8, R4-1, R4-3, R4-6a, R4-6b, R4-7, R4-8, R4-9, R4-9a, R4-9c |
| log_source | framework | `framework` | 423 | R1A, R1B, R2-10r2, R2-11c, R2-13, R2-15, R2-3, R2-4a, R2-4b, R2-4c, R2-5, R2-6, R2-7, R2-8, R2-9, R3-2, R3-4r2, R3-5, R3-6, R3-7, R3-8, R4-1, R4-3, R4-6a, R4-6b, R4-7, R4-8, R4-9, R4-9a, R4-9c |
| intent_type | agent | `GUI_Action` | 149 | R1A, R1B, R2-10r2, R2-11c, R2-13, R2-15, R2-3, R2-4a, R2-4b, R2-4c, R2-5, R2-6, R2-7, R2-8, R2-9, R3-2, R3-4r2, R3-5, R3-6, R3-7, R3-8, R4-1, R4-3, R4-6a, R4-6b, R4-7, R4-8, R4-9, R4-9a, R4-9c |
| action_type | agent | `call_user` | 1 | R2-4b |
| action_type | agent | `click` | 63 | R1A, R1B, R2-11c, R2-13, R2-15, R2-4a, R2-4b, R2-4c, R2-5, R2-6, R2-7, R2-8, R2-9, R3-2, R3-4r2, R3-5, R3-6, R3-7, R3-8, R4-1, R4-3, R4-6a, R4-6b, R4-7, R4-8, R4-9, R4-9a, R4-9c |
| action_type | agent | `open_app` | 25 | R1A, R1B, R2-10r2, R2-11c, R2-13, R2-15, R2-3, R2-4a, R2-4b, R2-4c, R2-5, R2-6, R2-8, R2-9, R3-2, R3-4r2, R3-5, R3-6, R3-7, R3-8, R4-1, R4-3, R4-6a, R4-6b, R4-7, R4-8, R4-9, R4-9a, R4-9c |
| action_type | agent | `stop` | 2 | R2-7, R2-8 |
| action_type | agent | `take_notes` | 9 | R1A, R1B, R2-15, R3-4r2, R3-5, R3-6, R3-7, R3-8, R4-1, R4-6a, R4-6b, R4-7, R4-8, R4-9, R4-9a, R4-9c |
| action_type | agent | `type` | 13 | R1A, R1B, R2-15, R2-6, R2-8, R3-4r2, R3-5, R3-6, R3-7, R3-8, R4-1, R4-6a, R4-6b, R4-7, R4-8, R4-9, R4-9a, R4-9c |
| action_type | agent | `wait` | 17 | R1A, R1B, R2-10r2, R2-11c, R2-13, R2-15, R2-3, R2-4a, R2-4b, R2-4c, R2-5, R2-6, R2-7, R2-8, R2-9, R3-2, R3-4r2, R3-5, R3-6, R3-7, R3-8, R4-1, R4-3, R4-6a, R4-6b, R4-7, R4-8, R4-9, R4-9a, R4-9c |
| action_type | framework | `SCREENSHOT` | 297 | R1A, R1B, R2-10r2, R2-11c, R2-13, R2-15, R2-3, R2-4a, R2-4b, R2-4c, R2-5, R2-6, R2-7, R2-8, R2-9, R3-2, R3-4r2, R3-5, R3-6, R3-7, R3-8, R4-1, R4-3, R4-6a, R4-6b, R4-7, R4-8, R4-9, R4-9a, R4-9c |
| action_type | framework | `TOUCH_DOWN` | 63 | R1A, R1B, R2-11c, R2-13, R2-15, R2-4a, R2-4b, R2-4c, R2-5, R2-6, R2-7, R2-8, R2-9, R3-2, R3-4r2, R3-5, R3-6, R3-7, R3-8, R4-1, R4-3, R4-6a, R4-6b, R4-7, R4-8, R4-9, R4-9a, R4-9c |
| action_type | framework | `TOUCH_UP` | 63 | R1A, R1B, R2-11c, R2-13, R2-15, R2-4a, R2-4b, R2-4c, R2-5, R2-6, R2-7, R2-8, R2-9, R3-2, R3-4r2, R3-5, R3-6, R3-7, R3-8, R4-1, R4-3, R4-6a, R4-6b, R4-7, R4-8, R4-9, R4-9a, R4-9c |

## 3. 文档未列出的字段表

作用均标为推断，不能代替缺失的规范字段；agent_identify和agent_decision是文档类别对应的JSON字符串容器，已在位置说明处理。

| 位置/键名 | 出现条数 | 格式或枚举值 | 作用说明 |
| --- | --- | --- | --- |
| `$.agent_decision.error_code` | agent 149；framework 0 | 字符串0：148条；1：1条（R2-4b） | 推断：动作结果/错误码，不等于reason_code或decision_outcome |
| `$.agent_decision.instruction_id` | agent 149；framework 0 | 32位小写十六进制；25种值 | 推断：指令关联标识，R1B两包共用；未证明与session_id等价 |
| `$.agent_decision.log_id` | agent 149；framework 0 | 34字符：14位数字前缀+20位大写十六进制；26种值 | 推断：任务/请求关联标识；23个ID覆盖多个不同步骤，非单条记录唯一键 |
| `$.device_id` | agent 149；framework 0 | 16位数字字符串（1种值） | 推断：设备关联标识 |
| `$.execute_id` | agent 149；framework 0 | 130条36字符UUID格式且各不同；19条空字符串 | 推断：动作执行关联ID；未证明与event_id等价 |
| `$.instruction_type` | agent 130；framework 0 | normal，130条 | 推断：任务指令类型 |
| `$.package_sha256` | agent 0；framework 423 | 64位大写十六进制字符串 | 推断：framework中的Agent包摘要，与agent_identify.pkg_sha256值一致 |
| `$.tool_name` | agent 149；framework 0 | GUI_Action，149条 | 推断：工具/操作通道名，与intent_type同值 |
| `$.uid` | agent 149；framework 0 | 16位数字字符串（1种值） | 推断：身份/主体标识；不能仅据名称认定为Android应用UID |

逐个展开policy内部字段如下，P表示$.agent_decision.policy解码后的对象；这些不是新增审计顶层列，也不因未列于日志字段表就判为实现错误。带点的完整Activity类名是一个实际对象键；次数按含该路径的去重记录计。

| policy内部路径 | 出现条数 | 格式或枚举值 | 作用说明 |
| --- | --- | --- | --- |
| `P.schema` | agent 149；framework 0 | schema版本字符串 | 推断：策略结构版本 |
| `P.policy_version` | agent 149；framework 0 | JSON整数；不同值 1, 2, 3, 8, 9, 10；同版本可有不同内容 | 推断：策略版本标识 |
| `P.package` | agent 149；framework 0 | Android包名字符串 | 推断：策略适用包 |
| `P.updated_at` | agent 149；framework 0 | 字符串：13位数字145条，非数字4条（R2-13预设条件） | 推断：策略更新时间元数据，不是日志发生时间 |
| `P.default_policy` | agent 149；framework 0 | JSON对象 | 推断：组织应用、页面或意图规则 |
| `P.default_policy.app` | agent 149；framework 0 | JSON对象 | 推断：组织应用、页面或意图规则 |
| `P.default_policy.app.global_disable` | agent 149；framework 0 | 布尔：false:137, true:12 | 推断：对应层或意图的限制开关 |
| `P.default_policy.app.screenshot_disable` | agent 149；framework 0 | 布尔：false:145, true:4 | 推断：对应层或意图的限制开关 |
| `P.default_policy.app.input_disable` | agent 149；framework 0 | 布尔：false:145, true:4 | 推断：对应层或意图的限制开关 |
| `P.scope` | agent 149；framework 0 | JSON对象 | 推断：组织应用、页面或意图规则 |
| `P.scope.app` | agent 149；framework 0 | JSON对象 | 推断：组织应用、页面或意图规则 |
| `P.scope.app.global_disable` | agent 145；framework 0 | 布尔：false:145 | 推断：对应层或意图的限制开关 |
| `P.scope.app.screenshot_disable` | agent 145；framework 0 | 布尔：false:145 | 推断：对应层或意图的限制开关 |
| `P.scope.app.input_disable` | agent 145；framework 0 | 布尔：false:139, true:6 | 推断：对应层或意图的限制开关 |
| `P.scope.activities` | agent 145；framework 0 | JSON对象 | 推断：组织应用、页面或意图规则 |
| `P.scope.activities.com.saep.lab.TargetActivity` | agent 145；framework 0 | JSON对象 | 推断：组织应用、页面或意图规则 |
| `P.scope.activities.com.saep.lab.TargetActivity.name` | agent 145；framework 0 | 中文名称字符串 | 推断：页面展示名称 |
| `P.scope.activities.com.saep.lab.TargetActivity.page_scope` | agent 145；framework 0 | JSON对象 | 推断：组织应用、页面或意图规则 |
| `P.scope.activities.com.saep.lab.TargetActivity.page_scope.global_disable` | agent 145；framework 0 | 布尔：false:137, true:8 | 推断：对应层或意图的限制开关 |
| `P.scope.activities.com.saep.lab.TargetActivity.page_scope.screenshot_disable` | agent 145；framework 0 | 布尔：false:135, true:10 | 推断：对应层或意图的限制开关 |
| `P.scope.activities.com.saep.lab.TargetActivity.page_scope.input_disable` | agent 145；framework 0 | 布尔：false:141, true:4 | 推断：对应层或意图的限制开关 |
| `P.scope.agent_intents` | agent 145；framework 0 | JSON对象 | 推断：组织应用、页面或意图规则 |
| `P.scope.agent_intents.modify_content` | agent 145；framework 0 | 布尔：false:133, true:12 | 推断：对应层或意图的限制开关 |
| `P.scope.agent_intents.post_content` | agent 145；framework 0 | 布尔：false:138, true:7 | 推断：对应层或意图的限制开关 |
| `P.scope.agent_intents.delete_content` | agent 145；framework 0 | 布尔：false:136, true:9 | 推断：对应层或意图的限制开关 |
| `P.scope.agent_intents.account_incentive` | agent 145；framework 0 | 布尔：false:141, true:4 | 推断：对应层或意图的限制开关 |
| `P.scope.activities.com.saep.lab.MainActivity` | agent 5；framework 0 | JSON对象 | 推断：组织应用、页面或意图规则 |
| `P.scope.activities.com.saep.lab.MainActivity.name` | agent 5；framework 0 | 中文名称字符串 | 推断：页面展示名称 |
| `P.scope.activities.com.saep.lab.MainActivity.page_scope` | agent 5；framework 0 | JSON对象 | 推断：组织应用、页面或意图规则 |
| `P.scope.activities.com.saep.lab.MainActivity.page_scope.input_disable` | agent 5；framework 0 | 布尔：false:3, true:2 | 推断：对应层或意图的限制开关 |
| `P.scope.activities.com.saep.lab.InputActivity` | agent 16；framework 0 | JSON对象 | 推断：组织应用、页面或意图规则 |
| `P.scope.activities.com.saep.lab.InputActivity.name` | agent 16；framework 0 | 中文名称字符串 | 推断：页面展示名称 |
| `P.scope.activities.com.saep.lab.InputActivity.page_scope` | agent 16；framework 0 | JSON对象 | 推断：组织应用、页面或意图规则 |
| `P.scope.activities.com.saep.lab.InputActivity.page_scope.input_disable` | agent 16；framework 0 | 布尔：true:16 | 推断：对应层或意图的限制开关 |
| `P.scope.activities.com.saep.lab.PublishActivity` | agent 4；framework 0 | JSON对象 | 推断：组织应用、页面或意图规则 |
| `P.scope.activities.com.saep.lab.PublishActivity.name` | agent 4；framework 0 | 中文名称字符串 | 推断：页面展示名称 |
| `P.scope.activities.com.saep.lab.PublishActivity.page_scope` | agent 4；framework 0 | JSON对象 | 推断：组织应用、页面或意图规则 |
| `P.scope.activities.com.saep.lab.PublishActivity.page_scope.input_disable` | agent 4；framework 0 | 布尔：true:4 | 推断：对应层或意图的限制开关 |
| `P.scope.activities.com.saep.lab.RecordActivity` | agent 5；framework 0 | JSON对象 | 推断：组织应用、页面或意图规则 |
| `P.scope.activities.com.saep.lab.RecordActivity.name` | agent 5；framework 0 | 中文名称字符串 | 推断：页面展示名称 |
| `P.scope.activities.com.saep.lab.RecordActivity.page_scope` | agent 5；framework 0 | JSON对象 | 推断：组织应用、页面或意图规则 |
| `P.scope.activities.com.saep.lab.RecordActivity.page_scope.input_disable` | agent 5；framework 0 | 布尔：true:5 | 推断：对应层或意图的限制开关 |
| `P.scope.activities.com.saep.lab.RewardActivity` | agent 5；framework 0 | JSON对象 | 推断：组织应用、页面或意图规则 |
| `P.scope.activities.com.saep.lab.RewardActivity.name` | agent 5；framework 0 | 中文名称字符串 | 推断：页面展示名称 |
| `P.scope.activities.com.saep.lab.RewardActivity.page_scope` | agent 5；framework 0 | JSON对象 | 推断：组织应用、页面或意图规则 |
| `P.scope.activities.com.saep.lab.RewardActivity.page_scope.screenshot_disable` | agent 5；framework 0 | 布尔：true:5 | 推断：对应层或意图的限制开关 |

## 4. 接管与停止记录的形态表

仅描述既有记录；时间均为对应导出audit.records[].timestamp按int()转为Unix毫秒，不把空动作补写成接管或停止。R2-4b显式call_user时间1792499639544，比其round.json.task_end_device_ms=1792499638538晚1006毫秒，但已被轮后导出收录，故保留。

| 轮次 | Unix毫秒 | action_type/形态 | 其他字段 | action_args/说明 | 来源 |
| --- | --- | --- | --- | --- | --- |
| R2-3 | 1792499237599 | 未找到显式call_user/stop | agent 3条；action_type分布 wait:1, 〈缺键〉:1, open_app:1 | 缺action_type记录的step_id/execute_id为空串、error_code=0，不足以代表接管/停止 | `runs/R2-3/export_D_com.saep.lab.g23.json` |
| R2-4a | 1792499408494 | 未找到显式call_user/stop | agent 4条；action_type分布 click:1, wait:1, 〈缺键〉:1, open_app:1 | 缺action_type记录的step_id/execute_id为空串、error_code=0，不足以代表接管/停止 | `runs/R2-4a/export_D_com.saep.lab.g24a.json` |
| R2-4b | 1792499639544 | call_user | error_code=1；step_id为call_…；execute_id非空；decision_outcome/reason_code/user_authorization_type缺键 | [提示文本, 手动操作, 请求协助] | `runs/R2-4b/export_D_com.saep.lab.g24b.json`：audit.records[0].agent_decision |
| R2-4c | 1792499768879 | 未找到显式call_user/stop | agent 4条；action_type分布 click:1, wait:1, 〈缺键〉:1, open_app:1 | 缺action_type记录的step_id/execute_id为空串、error_code=0，不足以代表接管/停止 | `runs/R2-4c/export_D_com.saep.lab.g24c.json` |
| R2-5 | 1792500463893 | 未找到显式call_user/stop | agent 4条；action_type分布 click:1, wait:1, 〈缺键〉:1, open_app:1 | 缺action_type记录的step_id/execute_id为空串、error_code=0，不足以代表接管/停止 | `runs/R2-5/export_D_com.saep.lab.g25.json` |
| R2-6 | 1792502145275 | 未找到显式call_user/stop | agent 7条；action_type分布 type:1, click:3, wait:1, 〈缺键〉:1, open_app:1 | 缺action_type记录的step_id/execute_id为空串、error_code=0，不足以代表接管/停止 | `runs/R2-6/export_D_com.saep.lab.g26.json` |
| R2-7 | 1792502562180 | stop | error_code=0；step_id为call_…；execute_id非空；decision_outcome/reason_code/user_authorization_type缺键 | [停止说明, failed, false, task_failed] | `runs/R2-7/export_D_com.saep.lab.g27.json`：audit.records[0].agent_decision |
| R2-8 | 1792502812449 | stop | error_code=0；step_id为call_…；execute_id非空；decision_outcome/reason_code/user_authorization_type缺键 | [停止说明, failed, false, task_failed] | `runs/R2-8/export_D_com.saep.lab.g28.json`：audit.records[0].agent_decision |
| R2-9 | 1792503093356 | 未找到显式call_user/stop | agent 4条；action_type分布 click:1, wait:1, 〈缺键〉:1, open_app:1 | 缺action_type记录的step_id/execute_id为空串、error_code=0，不足以代表接管/停止 | `runs/R2-9/export_D_com.saep.lab.g29.json` |
| R2-10 | 无 | 未找到显式call_user/stop | agent 0条；action_type分布  | 审计为空，无法验证日志动作形态 | `runs/R2-10/export_D_com.saep.lab.g2a.json` |
| R2-10r2 | 1792505565811 | 未找到显式call_user/stop | agent 3条；action_type分布 wait:1, 〈缺键〉:1, open_app:1 | 缺action_type记录的step_id/execute_id为空串、error_code=0，不足以代表接管/停止 | `runs/R2-10r2/export_D_com.saep.lab.g2a.json` |

显式call_user共1条、stop共2条，均位于agent_decision.action_type，是小写动作值；没有独立decision_outcome=CALL_USER/BLOCK或reason_code。其他轮次的空动作记录不足以确认代表接管/停止；未重新判定任何轮次。

## 5. 建议补入全局偏差的条目

- **G04**：framework身份字段仅有agent_name、agent_uuid、package_sha256，后者对应agent侧pkg_sha256，而agent_type、trust_level、package_name、agent_version、agent_md5、proof_signature均未随framework记录返回。
- **G05**：19条agent记录缺少文档标为不可空的action_type和action_args，另有297条framework截图记录缺少action_args。
- **G06**：两类记录均未出现event_id、decision_outcome、user_authorization_type、duration_ms，execute_id与error_code的存在不能证明它们与这些字段等价。
- **G07**：嵌套agent_decision.log_id在149条agent记录中仅有26个不同值，且23个值覆盖不同步骤，不能用作单条审计记录的去重键。
- **G08**：agent的action_args为单元素数组、元素仍是JSON参数数组字符串，而framework的126条action_args均为x=数字,y=数字普通文本，两类参数编码不同。
