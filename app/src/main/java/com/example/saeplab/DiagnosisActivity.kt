package com.example.saeplab

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import org.json.JSONObject

class DiagnosisActivity : LabActivity() {
    private lateinit var results: LinearLayout
    private lateinit var refresh: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        header("环境诊断", "确认这台手机具备哪些接入能力。")
        refresh = button(content, "重新检测", true) { runDiagnostics() }
        results = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(0, dp(18), 0, 0) }
        content.addView(results)
        runDiagnostics()
    }

    private fun runDiagnostics() {
        refresh.text = "正在检测…"
        work(refresh, {
            val bridge = SaepBridge(this)
            val result = bridge.diagnose().put("audit_probe", bridge.audit())
            store.cache("diagnostics", result)
            store.cache("audit", result.getJSONObject("audit_probe"))
            store.event("diagnostics_completed", "DiagnosisActivity")
            result
        }) { result -> refresh.text = "重新检测"; render(result) }
    }

    private fun render(result: JSONObject) {
        results.removeAllViews()
        val saep = result.getJSONObject("saep")
        val box = card(results)
        text(box, "SAEP 协议", 13f, muted, true)
        text(box, statusName(saep.optString("status")), 23f,
            if (saep.optString("status") == "ENABLED") teal else amber, true)
        text(box, if (saep.optBoolean("enabled"))
            "系统报告协议已启用。仍需通过实际任务确认策略效果。" else
            "当前不能确认系统约束可用，普通记事仍可使用。详细原因可查看原始诊断。", 14f, muted, false, 0)

        val device = result.getJSONObject("device")
        val info = card(results)
        text(info, "测试设备", 17f, ink, true)
        text(info, "${device.optString("manufacturer")} ${device.optString("model")}\nAndroid ${device.optString("android_version")} · API ${device.optInt("api_level")}\n${device.optString("build_display")}", 14f, muted)
        text(info, "ObricUI 版本请在手机设置中核对，协议要求 2.2 及以上。", 13f, muted, false, 0)

        val policy = card(results)
        val packaged = result.getJSONObject("packaged_policy")
        text(policy, "静态策略 · v${packaged.optInt("policy_version")}", 17f, ink, true)
        text(policy, if (packaged.optBoolean("manifest_resource_present")) "已随 APK 声明" else "策略资源缺失", 15f, teal, true)
        text(policy, "普通记事：未限制输入\n输入限制：声明禁止模拟输入\n系统是否执行：待实测", 14f, muted, false, 0)

        val agents = result.getJSONObject("registered_agents")
        val agentCard = card(results)
        text(agentCard, "已注册 Agent", 17f, ink, true)
        val countText = if (agents.has("count")) "${agents.optInt("count")} 个" else "数量未知"
        text(agentCard, "${statusName(agents.optString("status"))} · $countText", 15f, muted)

        val audit = result.getJSONObject("audit_probe")
        val auditCard = card(results)
        text(auditCard, "本应用系统审计", 17f, ink, true)
        text(auditCard, statusName(audit.optString("status")), 15f, muted)
        text(auditCard, auditDescription(audit), 13f, muted, false, 0)

        val rawBox = card(results)
        val raw = text(rawBox, result.toString(2), 11f, muted)
        raw.typeface = android.graphics.Typeface.MONOSPACE
        raw.setTextIsSelectable(true)
        raw.visibility = android.view.View.GONE
        button(rawBox, "展开 / 收起原始诊断") {
            raw.visibility = if (raw.visibility == android.view.View.GONE) android.view.View.VISIBLE else android.view.View.GONE
        }
    }
}
