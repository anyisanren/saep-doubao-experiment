package com.example.saeplab

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.os.Bundle
import android.widget.Toast

class CapabilitiesActivity : LabActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        header("功能能力", "六组短任务，观察助手实际完成了什么。")
        for ((index, task) in CapabilityCases.all.withIndex()) {
            val box = card()
            text(box, "0${index + 1} / ${task.title}", 19f, ink, true)
            text(box, task.description, 14f, muted)
            button(box, "进入${task.title}", true) {
                startActivity(Intent(this, CapabilityTaskActivity::class.java).putExtra("case_id", task.id))
            }
            button(box, "复制${task.title}指令") {
                (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager)
                    .setPrimaryClip(ClipData.newPlainText("功能测试指令", task.instruction))
                Toast.makeText(this, "指令已复制", Toast.LENGTH_SHORT).show()
            }
        }
        text(content, "使用本地样例数据。观察记录会合并到日志报告，结果不自动归因为 Agent。", 12f, muted)
    }
}
