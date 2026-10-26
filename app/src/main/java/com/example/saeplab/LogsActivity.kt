package com.example.saeplab

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import java.io.File

class LogsActivity : LabActivity() {
    private lateinit var auditStatus: TextView
    private lateinit var exportStatus: TextView
    private lateinit var refresh: Button
    private lateinit var export: Button
    private lateinit var events: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        header("日志导出", "保存一份能对照实际结果的实验记录。")
        val summary = card()
        text(summary, "当前实验", 18f, ink, true)
        text(summary, "记录编号：${store.session().take(8)}\n本地操作：${store.events().length()} 条\n输入限制页保存：${store.restricted().optInt("save_count")} 次", 14f, muted, false, 0)
        val audit = card()
        text(audit, "系统审计 · 最近 24 小时", 18f, ink, true)
        auditStatus = text(audit, "尚未查询", 14f, muted)
        refresh = button(audit, "查询系统审计") {
            auditStatus.text = "正在查询…"
            work(refresh, { SaepBridge(this).audit().also { store.cache("audit", it) } }) { renderAudit() }
        }
        renderAudit()
        val share = card()
        text(share, "导出实验报告", 18f, ink, true)
        text(share, "包含环境诊断、声明策略、系统审计、操作记录和状态摘要。笔记与测试文本以长度和摘要记录。", 14f, muted)
        text(share, "系统审计按原样导出，分享前请检查其中的信息。", 12f, muted)
        export = button(share, "导出 JSON 到下载目录", true, R.id.export_report) { exportReport() }
        exportStatus = text(share, "", 13f, teal, false, 0)
        text(content, "最近的本地记录", 18f, ink, true, 12)
        events = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(events)
        renderEvents()
    }

    private fun renderAudit() {
        val audit = store.cached("audit")
        auditStatus.text = if (audit == null) "尚未查询" else
            "${statusName(audit.optString("status"))}\n${auditDescription(audit)}" +
                if (audit.optString("status") == "AVAILABLE") "\n返回 ${audit.optInt("returned_count")} 条记录" else ""
    }

    private fun renderEvents() {
        events.removeAllViews()
        val entries = store.events()
        val labels = mapOf("page_opened" to "打开页面", "note_saved" to "保存笔记",
            "restricted_text_changed" to "限制页收到文本变化", "restricted_saved" to "限制页实际保存",
            "session_started" to "开始新一轮", "diagnostics_completed" to "完成诊断", "report_generated" to "生成报告",
            "capability_observed" to "功能能力观察")
        for (i in entries.length() - 1 downTo maxOf(0, entries.length() - 12)) {
            val item = entries.getJSONObject(i)
            val box = card(events)
            text(box, labels[item.optString("event")] ?: item.optString("event"), 15f, ink, true)
            text(box, "${item.optString("timestamp")}\n${item.optString("activity")}", 12f, muted, false, 0)
        }
    }

    private fun exportReport() {
        exportStatus.text = "正在收集诊断与审计…"
        work(export, {
            val file = ReportExporter.create(this)
            val name = ReportExporter.fileName()
            if (Build.VERSION.SDK_INT >= 29) ReportExporter.saveDownloads(this, file, name)
            name
        }) { name ->
            if (Build.VERSION.SDK_INT >= 29) exportStatus.text = "已保存到 Download/SAEP-Lab/$name"
            else {
                @Suppress("DEPRECATION")
                startActivityForResult(Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE); type = "application/json"; putExtra(Intent.EXTRA_TITLE, name)
                }, 100)
            }
            renderAudit(); renderEvents()
        }
    }

    @Deprecated("Legacy document picker for Android 8–9")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == Activity.RESULT_OK && data?.data != null) {
            val uri = data.data!!
            work(export, {
                contentResolver.openOutputStream(uri)?.use { out -> File(cacheDir, "latest-report.json").inputStream().use { it.copyTo(out) } }
                    ?: error("无法写入导出文件")
            }) { exportStatus.text = "报告已保存" }
        } else if (requestCode == 100) exportStatus.text = "已取消导出"
    }
}
