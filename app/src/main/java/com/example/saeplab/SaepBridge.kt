package com.example.saeplab

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method
import java.lang.reflect.Modifier

/** Calls the documented framework APIs without changing hidden-API restrictions. */
class SaepBridge(private val context: Context) {
    companion object {
        const val ROBOTS = "android.security.obric.robots.RobotsHelperStub"
        const val AGENTS = "android.security.obric.agentmanager.AgentManagerStub"
        const val AUDIT = "android.security.obric.audit.SecurityAuditManagerStub"
    }

    private fun method(clazz: Class<*>, name: String, types: Array<Class<*>>): Method =
        try { clazz.getMethod(name, *types) } catch (_: NoSuchMethodException) {
            clazz.getDeclaredMethod(name, *types).apply { isAccessible = true }
        }

    private fun call(clazz: Class<*>, target: Method, args: Array<out Any?> = emptyArray()): Any? {
        // Both static and singleton-instance forms occur in the protocol examples.
        val receiver = if (Modifier.isStatic(target.modifiers)) null else {
            method(clazz, "getInstance", emptyArray()).invoke(null)
                ?: throw IllegalStateException("getInstance returned null")
        }
        return target.invoke(receiver, *args)
    }

    private fun failure(error: Throwable): JSONObject {
        val cause = if (error is InvocationTargetException) error.targetException else error
        val status = when (cause) {
            is ClassNotFoundException, is NoSuchMethodException, is NoClassDefFoundError -> "UNAVAILABLE"
            else -> "ERROR"
        }
        return JSONObject().put("status", status).put("error_type", cause.javaClass.name)
            .put("message", cause.message ?: "接口调用失败")
    }

    fun availability(): JSONObject = try {
        val clazz = Class.forName(ROBOTS)
        val withContext = try { method(clazz, "isRobotsEnabled", arrayOf(Context::class.java)) }
            catch (_: NoSuchMethodException) { null }
        val used = withContext ?: method(clazz, "isRobotsEnabled", emptyArray())
        val result = call(clazz, used, if (withContext != null) arrayOf(context) else emptyArray())
        require(result is Boolean) { "Unexpected availability result: ${result?.javaClass?.name}" }
        JSONObject().put("status", if (result) "ENABLED" else "DISABLED")
            .put("enabled", result).put("method", used.toGenericString())
    } catch (e: Exception) { failure(e) } catch (e: LinkageError) { failure(e) }

    private fun agents(): JSONObject = try {
        val clazz = try { Class.forName(AGENTS) } catch (_: ClassNotFoundException) {
            // Alternative class name appears in section 4.6.1 of the public document.
            Class.forName("android.security.obric.agentmanager.RobotsHelperStub")
        }
        val ids = call(clazz, method(clazz, "listRegisteredAgents", emptyArray()))
        require(ids is List<*>) { "Agent list is not a List" }
        val entries = JSONArray()
        for (id in ids.take(100)) {
            val item = JSONObject().put("uuid", id.toString())
            try {
                val raw = call(clazz, method(clazz, "getAgentInfo", arrayOf(String::class.java)), arrayOf(id.toString()))
                item.put("info", raw?.let { parseJsonOrText(it.toString()) } ?: JSONObject.NULL)
            } catch (e: Exception) { item.put("info_error", failure(e)) }
            entries.put(item)
        }
        JSONObject().put("status", if (ids.isEmpty()) "EMPTY_UNCONFIRMED" else "AVAILABLE")
            .put("class", clazz.name).put("count", ids.size).put("agents", entries)
    } catch (e: Exception) { failure(e) } catch (e: LinkageError) { failure(e) }

    fun packagedPolicy(): JSONObject = JSONObject(context.resources.openRawResource(R.raw.agent_saep_policy)
        .bufferedReader().use { it.readText() })

    fun policySha256(): String = LabStore.sha256(context.resources.openRawResource(R.raw.agent_saep_policy)
        .bufferedReader().use { it.readText() })

    @Suppress("DEPRECATION")
    private fun componentVersions(): JSONObject = JSONObject().apply {
        listOf("com.larus.nova", "com.obric.aiagent.plugin").forEach { pkg ->
            put(pkg, try {
                val p = context.packageManager.getPackageInfo(pkg, 0)
                JSONObject().put("version_name", p.versionName)
                    .put("version_code", if (Build.VERSION.SDK_INT >= 28) p.longVersionCode else p.versionCode.toLong())
                    .put("last_update_ms", p.lastUpdateTime)
            } catch (e: Exception) { failure(e) })
        }
    }

    @Suppress("DEPRECATION")
    fun diagnose(): JSONObject {
        val metadata = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA).metaData
        val resourceId = metadata?.getInt("com.obric.agentrobots.POLICY_JSON", 0) ?: 0
        return JSONObject().put("checked_at", LabStore.now())
            .put("device", JSONObject().put("manufacturer", Build.MANUFACTURER).put("model", Build.MODEL)
                .put("android_version", Build.VERSION.RELEASE).put("api_level", Build.VERSION.SDK_INT)
                .put("build_display", Build.DISPLAY).put("build_incremental", Build.VERSION.INCREMENTAL)
                .put("obric_version", JSONObject.NULL).put("obric_version_status", "NOT_INDEPENDENTLY_OBSERVED")
                .put("build_fingerprint", Build.FINGERPRINT))
            .put("app", JSONObject().put("package", context.packageName)
                .put("version_name", BuildConfig.VERSION_NAME).put("version_code", BuildConfig.VERSION_CODE))
            .put("saep", availability()).put("registered_agents", agents()).put("component_versions", componentVersions())
            .put("packaged_policy", JSONObject().put("manifest_resource_present", resourceId == R.raw.agent_saep_policy)
                .put("policy_version", packagedPolicy().getInt("policy_version"))
                .put("resource_sha256", policySha256())
                .put("effective_policy_verified", false)
                .put("note", "仅证明静态策略已随 APK 声明；不代表系统已经加载或执行。"))
    }

    fun audit(start: Long = System.currentTimeMillis() - 24L * 60 * 60 * 1000,
              end: Long = System.currentTimeMillis()): JSONObject {
        val result = try {
            val clazz = Class.forName(AUDIT)
            val query = method(clazz, "queryLogs", arrayOf(java.lang.Long.TYPE, java.lang.Long.TYPE,
                String::class.java, String::class.java))
            val raw = call(clazz, query, arrayOf(start, end, null, null))
            require(raw is List<*>) { "Audit result is not a List" }
            val entries = JSONArray()
            raw.take(2000).forEach { entries.put(parseJsonOrText(it.toString())) }
            JSONObject().put("status", if (raw.isEmpty()) "EMPTY_UNCONFIRMED" else "AVAILABLE")
                .put("method", query.toGenericString()).put("returned_count", raw.size)
                .put("truncated", raw.size > 2000).put("records", entries)
                .put("note", if (raw.isEmpty()) "接口返回空列表；可能无记录，也可能查询失败，不能判定为通过。" else "系统原始返回，未自动判定用例通过。")
        } catch (e: Exception) { failure(e) } catch (e: LinkageError) { failure(e) }
        return result.put("queried_at", LabStore.now()).put("start_time_ms", start).put("end_time_ms", end)
            .put("target_package", context.packageName)
    }

    private fun parseJsonOrText(raw: String): Any = try { JSONObject(raw) } catch (_: Exception) { raw }
}
