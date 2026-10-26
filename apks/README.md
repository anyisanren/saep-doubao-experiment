# 实验 APK 清单

共 32 个冻结 APK：正式 SLAB 24 个、早期 Agent 实验室 8 个。均来自既有实验产物，本次没有重新签名或替换。构建新包输出到 dist/，不覆盖此目录。

校验命令：从仓库根目录运行 `python3 scripts/verify-apks.py`，或运行 `cd apks && shasum -a 256 -c SHA256SUMS.txt`。完整 SHA-256、包信息、签名证书摘要、策略资源与来源路径见 [manifest.json](manifest.json)。

同一包名的三个 DYN APK 只能覆盖安装，不能并存：初始 v1 / code20、APK-lo 静态 v7 / code21、APK-hi 静态 v9 / code22；动态 v2/v3/v8/v10 是运行时提交内容，不是额外 APK。

| APK | 包名 | 应用版本 / code | 静态策略版本 | 用途 |
| --- | --- | --- | --- | --- |
| [slab/a.apk](slab/a.apk) | com.saep.lab.a | 2.0 / 20 | 1 | R1B |
| [slab/b.apk](slab/b.apk) | com.saep.lab.b | 2.0 / 20 | 1 | R1B |
| [slab/base.apk](slab/base.apk) | com.saep.lab.base | 2.0 / 20 | 1 | P0、R1A-4、R1A、R2-15、R4-1、R4-7 |
| [slab/dyn-hi.apk](slab/dyn-hi.apk) | com.saep.lab.dyn | 2.0 / 22 | 9 | R4-6b（APK-hi） |
| [slab/dyn-lo.apk](slab/dyn-lo.apk) | com.saep.lab.dyn | 2.0 / 21 | 7 | R4-6a（APK-lo） |
| [slab/dyn-static-v1.apk](slab/dyn-static-v1.apk) | com.saep.lab.dyn | 2.0 / 20 | 1 | R3 系列初始包、R4-8 重装包 |
| [slab/g21.apk](slab/g21.apk) | com.saep.lab.g21 | 2.0 / 20 | 1 | R2-1 |
| [slab/g22.apk](slab/g22.apk) | com.saep.lab.g22 | 2.0 / 20 | 1 | R2-2 |
| [slab/g23.apk](slab/g23.apk) | com.saep.lab.g23 | 2.0 / 20 | 1 | R2-3 |
| [slab/g24a.apk](slab/g24a.apk) | com.saep.lab.g24a | 2.0 / 20 | 1 | R2-4a |
| [slab/g24b.apk](slab/g24b.apk) | com.saep.lab.g24b | 2.0 / 20 | 1 | R2-4b |
| [slab/g24c.apk](slab/g24c.apk) | com.saep.lab.g24c | 2.0 / 20 | 1 | R2-4c |
| [slab/g25.apk](slab/g25.apk) | com.saep.lab.g25 | 2.0 / 20 | 1 | R2-5 |
| [slab/g26.apk](slab/g26.apk) | com.saep.lab.g26 | 2.0 / 20 | 1 | R2-6 |
| [slab/g27.apk](slab/g27.apk) | com.saep.lab.g27 | 2.0 / 20 | 1 | R2-7 |
| [slab/g28.apk](slab/g28.apk) | com.saep.lab.g28 | 2.0 / 20 | 1 | R2-8 |
| [slab/g29.apk](slab/g29.apk) | com.saep.lab.g29 | 2.0 / 20 | 1 | R2-9 |
| [slab/g2a.apk](slab/g2a.apk) | com.saep.lab.g2a | 2.0 / 20 | 1 | R2-10 / R2-10r2 |
| [slab/g2b.apk](slab/g2b.apk) | com.saep.lab.g2b | 2.0 / 20 | 1 | R2-11 |
| [slab/g2c.apk](slab/g2c.apk) | com.saep.lab.g2c | 2.0 / 20 | 损坏 JSON（R2-12 既定条件） | R2-12 |
| [slab/g2d.apk](slab/g2d.apk) | com.saep.lab.g2d | 2.0 / 20 | 1 | R2-13 |
| [slab/g2e.apk](slab/g2e.apk) | com.saep.lab.g2e | 2.0 / 20 | 1 | R2-14 |
| [slab/g2f.apk](slab/g2f.apk) | com.saep.lab.g2f | 2.0 / 20 | 1 | R2-11c |
| [slab/g43.apk](slab/g43.apk) | com.saep.lab.g43 | 2.0 / 20 | 1 | R4-3 |
| [legacy/AgentLab-control-v0.3.0.apk](legacy/AgentLab-control-v0.3.0.apk) | com.example.saeplab.control | 0.3.0 / 3 | 3 | 早期 Agent 实验室；不计入正式 v2 的 42 项 |
| [legacy/AgentLab-delete-v0.3.0.apk](legacy/AgentLab-delete-v0.3.0.apk) | com.example.saeplab.delete | 0.3.0 / 3 | 3 | 早期 Agent 实验室；不计入正式 v2 的 42 项 |
| [legacy/AgentLab-delete-v0.3.1.apk](legacy/AgentLab-delete-v0.3.1.apk) | com.example.saeplab.delete | 0.3.1 / 4 | 4 | 早期 Agent 实验室；不计入正式 v2 的 42 项 |
| [legacy/AgentLab-modify-v0.3.0.apk](legacy/AgentLab-modify-v0.3.0.apk) | com.example.saeplab.modify | 0.3.0 / 3 | 3 | 早期 Agent 实验室；不计入正式 v2 的 42 项 |
| [legacy/AgentLab-modify-v0.3.1.apk](legacy/AgentLab-modify-v0.3.1.apk) | com.example.saeplab.modify | 0.3.1 / 4 | 4 | 早期 Agent 实验室；不计入正式 v2 的 42 项 |
| [legacy/AgentLab-v0.1.0.apk](legacy/AgentLab-v0.1.0.apk) | com.example.saeplab | 0.1.0 / 1 | 1 | 早期 Agent 实验室；不计入正式 v2 的 42 项 |
| [legacy/AgentLab-v0.2.0.apk](legacy/AgentLab-v0.2.0.apk) | com.example.saeplab | 0.2.0 / 2 | 2 | 早期 Agent 实验室；不计入正式 v2 的 42 项 |
| [legacy/AgentLab-v0.3.0.apk](legacy/AgentLab-v0.3.0.apk) | com.example.saeplab | 0.3.0 / 3 | 3 | 早期 Agent 实验室；不计入正式 v2 的 42 项 |

`policies/` 保存从每个 APK 逐字节提取的策略；g2c 的截断 JSON、g2d 的非数字 updated_at、g2e 的错包名都是实验条件。早期 0.1/0.2 APK 保留为历史物料，当前 app/ 源码是 0.3 系列，不声称可由当前源码逐字节复现所有旧包。

原始签名私钥不在仓库。首次构建会生成自己的实验密钥，无法直接覆盖安装由原密钥签名的归档包；如需保留原设备数据，应另行交接原实验密钥。
