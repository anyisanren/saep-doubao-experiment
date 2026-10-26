package com.example.saeplab

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import org.json.JSONObject
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object ReportExporter {
    fun create(context: Context): File {
        val store = LabStore(context)
        val bridge = SaepBridge(context)
        val diagnostics = bridge.diagnose()
        val audit = bridge.audit()
        store.cache("diagnostics", diagnostics)
        store.cache("audit", audit)
        store.event("report_generated", "LogsActivity")
        val report = JSONObject().put("format", "saep-lab-report/1.0").put("exported_at", LabStore.now())
            .put("session_id", store.session()).put("diagnostics", diagnostics)
            .put("declared_policy", bridge.packagedPolicy()).put("system_audit", audit)
            .put("app_observations", store.events()).put("observation_retention", store.retention())
            .put("app_state_summary", store.stateSummary())
            .put("interpretation", JSONObject()
                .put("input_actor", "unknown; app callbacks alone cannot distinguish human, adb, or Agent")
                .put("policy_enforcement", "not automatically verified")
                .put("privacy", "Note text and restricted input text are omitted; lengths and hashes are included. System audit records are returned as-is.")
                .put("retention", "Last 5000 app observations; last 24 hours of system audit, up to 2000 returned records. Security trial export uses its own bounded time window."))
        return File(context.cacheDir, "latest-report.json").apply { writeText(report.toString(2)) }
    }

    fun fileName(): String = "saep-report-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".json"

    fun saveDownloads(context: Context, file: File, name: String, mimeType: String = "application/json"): Uri {
        if (Build.VERSION.SDK_INT < 29) error("旧系统请使用文件选择器导出")
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, name)
            put(MediaStore.Downloads.MIME_TYPE, mimeType)
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/SAEP-Lab")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: error("无法创建下载文件")
        try {
            resolver.openOutputStream(uri)?.use { out -> file.inputStream().use { it.copyTo(out) } }
                ?: error("无法写入下载文件")
            check(resolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null) > 0)
            return uri
        } catch (e: Exception) { resolver.delete(uri, null, null); throw e }
    }
}
