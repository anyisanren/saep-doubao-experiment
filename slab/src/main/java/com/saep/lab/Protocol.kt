package com.saep.lab

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.os.SystemClock
import android.provider.MediaStore
import org.json.JSONArray
import org.json.JSONObject
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Modifier

object Protocol {
    const val POLICY_URI = "content://com.obric.agentrobots.provider/policy"
    const val AUDIT = "android.security.obric.audit.SecurityAuditManagerStub"
    private const val ROBOTS = "android.security.obric.robots.RobotsHelperStub"
    private const val AGENTS = "android.security.obric.agentmanager.AgentManagerStub"

    fun failure(t: Throwable): JSONObject {
        val e = if (t is InvocationTargetException) t.targetException else t
        return JSONObject().put("exception_type", e.javaClass.name)
            .put("exception_message", e.message ?: JSONObject.NULL)
    }

    private fun parse(raw: Any?): Any = when (raw) {
        null -> JSONObject.NULL
        else -> try { JSONObject(raw.toString()) } catch (_: Exception) { raw.toString() }
    }

    fun status(): JSONObject = try {
        val c = Class.forName(ROBOTS)
        val m = c.getMethod("isRobotsEnabled")
        val receiver = if (Modifier.isStatic(m.modifiers)) null else c.getMethod("getInstance").invoke(null)
        val value = m.invoke(receiver)
        JSONObject().put("method", m.toGenericString()).put("value", value)
            .put("enabled", value == true)
    } catch (e: Throwable) { failure(e) }

    fun agents(): JSONObject = try {
        val c = Class.forName(AGENTS)
        val list = c.getMethod("listRegisteredAgents")
        val receiver = if (Modifier.isStatic(list.modifiers)) null else c.getMethod("getInstance").invoke(null)
        val ids = list.invoke(receiver) as List<*>
        val entries = JSONArray()
        ids.forEach { id ->
            val entry = JSONObject().put("id", id)
            try {
                val info = c.getMethod("getAgentInfo", String::class.java)
                entry.put("info", parse(info.invoke(if (Modifier.isStatic(info.modifiers)) null else receiver, id.toString())))
            } catch (e: Throwable) { entry.put("error", failure(e)) }
            entries.put(entry)
        }
        JSONObject().put("count", ids.size).put("agents", entries)
    } catch (e: Throwable) { failure(e) }

    fun audit(context: Context, start: Long, end: Long, form: String? = null): JSONObject {
        val begun = SystemClock.elapsedRealtimeNanos()
        val selected = form ?: context.getSharedPreferences("slab", 0).getString("audit_form", "instance")!!
        val result = JSONObject().put("start_time", start).put("end_time", end)
            .put("intent_filter", JSONObject.NULL).put("activity_filter", JSONObject.NULL)
            .put("form", selected).put("called_at_ms", System.currentTimeMillis())
        try {
            val c = Class.forName(AUDIT)
            val m = c.getMethod("queryLogs", java.lang.Long.TYPE, java.lang.Long.TYPE, String::class.java, String::class.java)
            val receiver = if (selected == "static") null else c.getMethod("getInstance").invoke(null)
            val raw = m.invoke(receiver, start, end, null, null)
            result.put("method", m.toGenericString()).put("method_is_static", Modifier.isStatic(m.modifiers))
                .put("is_null", raw == null)
            if (raw == null) result.put("count", JSONObject.NULL).put("records", JSONObject.NULL)
            else {
                val records = JSONArray()
                (raw as List<*>).forEach { records.put(parse(it)) }
                result.put("count", records.length()).put("records", records).put("is_empty", records.length() == 0)
            }
        } catch (e: Throwable) {
            val f = failure(e)
            f.keys().forEach { result.put(it, f.get(it)) }
        }
        return result.put("duration_ns", SystemClock.elapsedRealtimeNanos() - begun)
    }

    fun readPolicy(context: Context): JSONObject {
        val r = JSONObject().put("uri", POLICY_URI).put("at_ms", System.currentTimeMillis())
        try {
            val cursor = context.contentResolver.query(Uri.parse(POLICY_URI), null, null, null, null)
            r.put("cursor_is_null", cursor == null)
            cursor?.use { c ->
                r.put("columns", JSONArray(c.columnNames.toList()))
                val rows = JSONArray()
                while (c.moveToNext()) {
                    val row = JSONObject()
                    c.columnNames.forEachIndexed { i, name ->
                        row.put(name, if (c.isNull(i)) JSONObject.NULL else c.getString(i))
                    }
                    rows.put(row)
                }
                r.put("rows", rows).put("count", rows.length())
            }
        } catch (e: Throwable) {
            val f = failure(e); f.keys().forEach { r.put(it, f.get(it)) }
        }
        return r
    }

    fun submit(context: Context, policy: String, version: Long): JSONObject {
        val r = JSONObject().put("at_ms", System.currentTimeMillis())
            .put("uptime_ms", SystemClock.elapsedRealtime()).put("version", version)
            .put("policy", policy).put("policy_sha256", Observation.sha(policy))
        try {
            val values = ContentValues().apply { put("policy", policy); put("version", version) }
            r.put("return_value", context.contentResolver.update(Uri.parse(POLICY_URI), values, null, null))
        } catch (e: Throwable) {
            val f = failure(e); f.keys().forEach { r.put(it, f.get(it)) }
        }
        // Exactly one immediate query of the same URI, even when update throws.
        r.put("immediate_read", readPolicy(context))
        Observation.submissions.put(r)
        Observation.log(context, "dynamic_submission", r)
        return r
    }

    fun save(context: Context, name: String, body: JSONObject): String {
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/SLAB")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("Cannot create export")
        context.contentResolver.openOutputStream(uri)!!.bufferedWriter().use { it.write(body.toString(2)) }
        context.contentResolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
        return name
    }
}
