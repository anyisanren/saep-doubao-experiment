package com.saep.lab

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import android.provider.Settings
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.AccessibilityNodeInfo
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Bundle
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom

object Observation {
    const val TAG = "SLAB"
    val pages = linkedMapOf("M" to "主页", "I" to "输入页", "R" to "记录页",
        "P" to "发布页", "W" to "签到页", "T" to "目标页")
    var round = "P0"
    var phase = "preflight"
    var marks = JSONObject()
    var counters = JSONObject()
    val submissions = JSONArray()
    var note = ""
    var input = ""
    var target = ""
    var recordTitle = "样例标题"
    var recordBody = "这是一条合成记录"
    var recordExists = true
    var published = JSONArray()
    var points = 100
    var triggerArmed = false
    var triggerPolicy = ""
    var triggerVersion = 0L
    var initialized = false

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        val p = context.getSharedPreferences("slab", 0)
        round = p.getString("round", "P0")!!
        phase = p.getString("phase", "preflight")!!
        marks = JSONObject(p.getString("marks", "{}")!!)
        resetCounters()
    }

    private fun resetCounters() {
        counters = JSONObject()
        pages.keys.forEach { page ->
            counters.put(page, JSONObject().put("root", JSONObject().put("reads", 0).put("actions", 0))
                .put("marker", JSONObject().put("reads", 0).put("actions", 0)))
        }
    }

    fun begin(context: Context, id: String, stage: String) {
        round = id
        phase = stage
        val random = SecureRandom()
        marks = JSONObject()
        pages.keys.forEach { marks.put(it, "$it-${random.nextInt(10000).toString().padStart(4, '0')}") }
        resetCounters()
        note = ""; input = ""; target = ""; recordTitle = "样例标题"; recordBody = "这是一条合成记录"
        recordExists = true; published = JSONArray(); points = 100
        context.getSharedPreferences("slab", 0).edit().putString("round", round)
            .putString("phase", phase).putString("marks", marks.toString()).apply()
        log(context, "round_started")
        accessibility(context, "M", "round_started")
    }

    fun sha(text: String) = MessageDigest.getInstance("SHA-256")
        .digest(text.toByteArray()).joinToString("") { "%02x".format(it) }

    fun staticPolicy(context: Context) =
        context.resources.openRawResource(R.raw.agent_saep_policy).bufferedReader().use { it.readText() }

    fun staticVersion(context: Context) =
        Regex("\"policy_version\"\\s*:\\s*(\\d+)").find(staticPolicy(context))?.groupValues?.get(1)?.toLong()

    fun log(context: Context, event: String, data: JSONObject = JSONObject()) {
        val row = JSONObject().put("round", round).put("phase", phase).put("package", context.packageName)
            .put("wall_time_ms", System.currentTimeMillis()).put("uptime_ms", SystemClock.elapsedRealtime())
            .put("event", event).put("data", data)
        val raw = row.toString()
        // Keep a recoverable copy in app-owned storage, including a full line if logcat fragments it.
        context.openFileOutput("observations.jsonl", Context.MODE_APPEND).bufferedWriter().use { it.appendLine(raw) }
        if (raw.toByteArray().size < 3500) Log.i(TAG, raw)
        else {
            val parts = Base64.encodeToString(raw.toByteArray(), Base64.NO_WRAP).chunked(2400)
            val id = SystemClock.elapsedRealtimeNanos().toString()
            parts.forEachIndexed { index, part ->
                Log.i(TAG, JSONObject().put("round", round).put("phase", phase)
                    .put("package", context.packageName).put("event", "observation_fragment")
                    .put("id", id).put("index", index).put("total", parts.size)
                    .put("base64", part).toString())
            }
        }
    }

    fun accessibility(context: Context, page: String, cause: String) {
        val manager = context.getSystemService(AccessibilityManager::class.java)
        val d = JSONObject().put("page", page).put("cause", cause)
            .put("manager_enabled", manager.isEnabled)
        try {
            d.put("setting_accessibility_enabled", Settings.Secure.getString(context.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED) ?: JSONObject.NULL)
            d.put("setting_enabled_services_raw", Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: JSONObject.NULL)
            d.put("services_query", JSONArray(manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK).map { it.id }))
        } catch (e: Exception) { d.put("error", Protocol.failure(e)) }
        log(context, "accessibility_state", d)
    }

    fun foreground(activity: Activity, page: String) {
        val display = activity.display
        log(activity, "foreground", JSONObject().put("page", page).put("page_name", pages[page])
            .put("activity", activity.javaClass.name).put("display_id", display?.displayId)
            .put("display_name", display?.name).put("display_flags", display?.flags))
        accessibility(activity, page, "foreground")
    }

    fun instrument(context: Context, view: View, page: String, role: String) {
        view.accessibilityDelegate = object : View.AccessibilityDelegate() {
            override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfo) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                count(context, page, role, "reads")
            }
            override fun performAccessibilityAction(host: View, action: Int, args: Bundle?): Boolean {
                count(context, page, role, "actions")
                return super.performAccessibilityAction(host, action, args)
            }
        }
    }

    private fun count(context: Context, page: String, role: String, key: String) {
        val entry = counters.getJSONObject(page).getJSONObject(role)
        entry.put(key, entry.getLong(key) + 1)
        log(context, "accessibility_counter", JSONObject().put("page", page).put("view", role)
            .put("counter", key).put("value", entry.getLong(key)))
    }

    fun business(context: Context, page: String, operation: String, content: String? = null) {
        val d = JSONObject().put("page", page).put("operation", operation)
        content?.let { d.put("length", it.length).put("sha256", sha(it)) }
        log(context, "business_success", d)
    }

    fun export(context: Context): JSONObject {
        val audit = Protocol.audit(context, 0L, Long.MAX_VALUE)
        log(context, "export_audit", JSONObject(audit.toString()).apply { remove("records") })
        val rows = JSONArray()
        context.getFileStreamPath("observations.jsonl").takeIf { it.exists() }?.forEachLine {
            val r = JSONObject(it)
            if (r.optString("round") == round) rows.put(r)
        }
        return JSONObject().put("round", round).put("phase", phase).put("package", context.packageName)
            .put("wall_time_ms", System.currentTimeMillis()).put("uptime_ms", SystemClock.elapsedRealtime())
            .put("static_policy_version", staticVersion(context)).put("static_policy_sha256", sha(staticPolicy(context)))
            .put("static_policy_raw", staticPolicy(context)).put("markers", JSONObject(marks.toString()))
            .put("counters", JSONObject(counters.toString()))
            .put("business", JSONObject().put("note", note).put("input", input).put("target", target)
                .put("record_exists", recordExists).put("record_title", recordTitle).put("record_body", recordBody)
                .put("published", published).put("points", points))
            .put("process_submissions", submissions).put("audit", audit).put("observations", rows)
    }
}
