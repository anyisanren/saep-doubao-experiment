package com.example.saeplab

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.TextView

class MainActivity : LabActivity() {
    private lateinit var summary: TextView
    private lateinit var sessionLabel: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent.getBooleanExtra("open_trial", false) || BuildConfig.EXPERIMENT_PROFILE != "lab"
            || SecurityTrial(this).active().optBoolean("navigation_pin", false)) {
            startActivity(SecurityTrial(this).pageIntent())
            finish()
            return
        }
        header("Agent 实验室", "从一个简单任务，验证真实操作边界。", true)
        val hero = card(fill = teal)
        text(hero, "v0.3 · 读取边界与意图观察", 12f, Color.rgb(183, 228, 212), true)
        text(hero, "准备 → 下达指令 → 查看结果", 19f, Color.WHITE, true, 12)
        summary = text(hero, "", 14f, Color.WHITE, false, 0)

        module("00", "安全实验", "读取卡片、样例记录和逐轮证据。", SecurityActivity::class.java)
        module("01", "功能能力", "六组短任务：表单、列表、长按、拖拽、文件与编辑。", CapabilitiesActivity::class.java)
        module("02", "环境诊断", "检查协议状态、设备信息与 Agent 接口。", DiagnosisActivity::class.java)
        module("03", "普通记事", "让助手创建笔记，建立操作对照。", NotesActivity::class.java)
        module("04", "输入限制", "观察受限页面是否仍收到输入或保存。", RestrictedInputActivity::class.java)
        module("05", "日志导出", "合并本地记录和系统审计，保存 JSON。", LogsActivity::class.java)
        val sessionBox = card()
        sessionLabel = text(sessionBox, "", 12f, muted)
        button(sessionBox, "开始新一轮") {
            store.newSession(); updateStatus()
            android.widget.Toast.makeText(this, "已开始新一轮；已有笔记和记录保留", android.widget.Toast.LENGTH_SHORT).show()
        }
        text(content, "v${BuildConfig.VERSION_NAME}  ·  测试数据保存在本机", 12f, muted, false, 0)
    }

    private fun module(number: String, title: String, description: String, target: Class<*>) {
        val box = card()
        text(box, "$number  /  $title", 19f, ink, true, 7)
        text(box, description, 14f, muted, false, 8)
        button(box, "进入$title") { startActivity(Intent(this, target)) }
    }

    override fun onResume() { super.onResume(); if (::summary.isInitialized) updateStatus() }
    private fun updateStatus() {
        val saep = store.cached("diagnostics")?.optJSONObject("saep")?.optString("status") ?: ""
        summary.text = "SAEP ${statusName(saep)}\n${store.notes().length()} 条笔记 · 策略执行效果待实测"
        sessionLabel.text = "本轮 ${store.session().take(8)} · 新一轮只更新记录编号"
    }
}
