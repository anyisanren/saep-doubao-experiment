package com.example.saeplab

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID

/** Local test data and observations. An app event does not identify the input actor. */
class LabStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("lab", Context.MODE_PRIVATE)

    companion object {
        private val lock = Any()
        fun now(): String = Instant.now().toString()
        fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
    }

    fun session(): String = synchronized(lock) {
        prefs.getString("session_id", null) ?: UUID.randomUUID().toString().also {
            check(prefs.edit().putString("session_id", it).putLong("session_start", System.currentTimeMillis()).commit())
        }
    }

    fun newSession(): String = synchronized(lock) {
        val id = UUID.randomUUID().toString()
        check(prefs.edit().putString("session_id", id).putLong("session_start", System.currentTimeMillis()).commit())
        event("session_started", "MainActivity")
        id
    }

    fun events(): JSONArray = synchronized(lock) { JSONArray(prefs.getString("events", "[]")) }

    fun event(type: String, activity: String, details: JSONObject = JSONObject()) = synchronized(lock) {
        val entries = events()
        val retained = JSONArray()
        val dropped = maxOf(0, entries.length() - 4999)
        for (i in dropped until entries.length()) retained.put(entries.get(i))
        retained.put(JSONObject().put("event_id", UUID.randomUUID().toString())
            .put("timestamp", now()).put("wall_time_ms", System.currentTimeMillis())
            .put("elapsed_realtime_ns", android.os.SystemClock.elapsedRealtimeNanos())
            .put("trial_id", prefs.getString("active_trial_id", ""))
            .put("session_id", session()).put("source", "app_observation")
            .put("actor", "unknown").put("event", type).put("activity", activity).put("details", details))
        check(prefs.edit().putString("events", retained.toString())
            .putLong("events_dropped", prefs.getLong("events_dropped", 0) + dropped).commit())
    }

    fun setTrial(id: String) = synchronized(lock) {
        check(prefs.edit().putString("active_trial_id", id).commit())
    }
    fun retention(): JSONObject = JSONObject().put("capacity", 5000)
        .put("dropped_lifetime", prefs.getLong("events_dropped", 0))

    fun notes(): JSONArray = synchronized(lock) { JSONArray(prefs.getString("notes", "[]")) }

    fun saveNote(existingId: String?, title: String, body: String): String = synchronized(lock) {
        val notes = notes()
        val id = existingId ?: UUID.randomUUID().toString()
        var index = -1
        for (i in 0 until notes.length()) if (notes.getJSONObject(i).getString("id") == id) index = i
        val note = JSONObject().put("id", id).put("title", title).put("body", body).put("updated_at", now())
        if (index < 0) notes.put(note) else notes.put(index, note)
        check(prefs.edit().putString("notes", notes.toString()).commit())
        event("note_saved", "NotesActivity", JSONObject().put("note_id", id)
            .put("title_length", title.length).put("body_length", body.length).put("content_sha256", sha256("$title\n$body")))
        id
    }

    fun restricted(): JSONObject = synchronized(lock) {
        JSONObject(prefs.getString("restricted", "{\"save_count\":0,\"edit_callbacks\":0,\"value\":\"\"}"))
    }

    fun restrictedEdited(length: Int) = synchronized(lock) {
        val state = restricted()
        state.put("edit_callbacks", state.optInt("edit_callbacks") + 1)
        check(prefs.edit().putString("restricted", state.toString()).commit())
        event("restricted_text_changed", "RestrictedInputActivity", JSONObject().put("length", length))
    }

    fun saveRestricted(value: String) = synchronized(lock) {
        val state = restricted().put("value", value).put("saved_at", now())
        state.put("save_count", state.optInt("save_count") + 1)
        check(prefs.edit().putString("restricted", state.toString()).commit())
        event("restricted_saved", "RestrictedInputActivity", JSONObject().put("value_length", value.length)
            .put("value_sha256", sha256(value)).put("save_count", state.optInt("save_count")))
    }

    fun cache(key: String, data: JSONObject) = synchronized(lock) {
        check(prefs.edit().putString(key, data.toString()).commit())
    }
    fun cached(key: String): JSONObject? = synchronized(lock) {
        prefs.getString(key, null)?.let { JSONObject(it) }
    }

    fun capabilities(): JSONObject = synchronized(lock) {
        JSONObject(prefs.getString("capabilities", "{}"))
    }

    fun capability(caseId: String): JSONObject = synchronized(lock) {
        capabilities().optJSONObject(caseId) ?: JSONObject()
    }

    fun observeCapability(caseId: String, action: String, values: JSONObject = JSONObject()) = synchronized(lock) {
        val all = capabilities()
        val state = all.optJSONObject(caseId) ?: JSONObject()
        values.keys().forEach { key -> state.put(key, values.get(key)) }
        state.put("updated_at", now()).put("session_id", session())
        all.put(caseId, state)
        check(prefs.edit().putString("capabilities", all.toString()).commit())
        event("capability_observed", "CapabilityTaskActivity", JSONObject()
            .put("case_id", caseId).put("action", action).put("values", values))
    }

    fun stateSummary(): JSONObject = synchronized(lock) {
        val notes = notes()
        val summaries = JSONArray()
        for (i in 0 until notes.length()) {
            val n = notes.getJSONObject(i)
            summaries.put(JSONObject().put("id", n.getString("id")).put("updated_at", n.optString("updated_at"))
                .put("title_length", n.getString("title").length).put("body_length", n.getString("body").length)
                .put("content_sha256", sha256(n.getString("title") + "\n" + n.getString("body"))))
        }
        val restricted = restricted()
        val value = restricted.optString("value")
        restricted.remove("value")
        restricted.put("value_length", value.length).put("value_sha256", sha256(value))
        JSONObject().put("notes", summaries).put("restricted_input", restricted)
            .put("capabilities", capabilities())
    }
}
