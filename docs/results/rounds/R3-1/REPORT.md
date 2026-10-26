> 交接摘要，原文件：`runs/R3-1/REPORT.md`。文内证据路径指向未公开的本地原始归档；结论与数据保持原样。

# R3-1

判定：文档未定义

实测：提交返回 1；紧接读取抛 SecurityException: Unauthorized agent query: package or signature mismatch.

拦截层：无法判定（本项为提交与读取，生效另测）

C1：提交原文与预设 v2 一致；生效 policy 待 R3-2。C2：缺失（同全局偏差 G01）；更新来源没有 reason_code。C3：齐全；轮次、包名、墙钟/uptime、截图、导出、系统日志已保存。

证据：API 返回 1，接受 v2；原文 SHA-256 0af07dac629a957452f83542321f7ebe217b3d693b7f6a961b9789368f389251；export_S 保留唯一提交

备注：提交成功与读取授权分离；首次 UI 点击被键盘遮挡未调用 API，实际仅一次提交
