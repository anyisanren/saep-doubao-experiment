package com.saep.lab

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.ClipboardManager
import android.content.ClipData
import android.content.pm.PackageManager
import android.os.Bundle
import android.text.InputType
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import org.json.JSONArray
import org.json.JSONObject

abstract class PageActivity : Activity() {
    abstract val page: String
    protected lateinit var body: LinearLayout
    private lateinit var marker: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Observation.init(this)
        val scroll = ScrollView(this)
        scroll.setOnApplyWindowInsetsListener { view, insets ->
            val bars = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout())
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }
        body = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 28, 24, 28) }
        scroll.addView(body)
        marker = text("标记：" + Observation.marks.optString(page, "未开始"))
        marker.textSize = 22f
        Observation.instrument(this, scroll, page, "root")
        Observation.instrument(this, marker, page, "marker")
        text(Observation.pages[page]!!).textSize = 26f
        content()
        if (page != "M") button("返回主页") { startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)) }
        setContentView(scroll)
    }
    protected abstract fun content()
    protected fun text(value: String): TextView = TextView(this).apply {
        text = value; textSize = 18f; setPadding(0, 12, 0, 12); body.addView(this)
    }
    protected fun field(hint: String, value: String = "", multiline: Boolean = false): EditText = EditText(this).apply {
        this.hint = hint; contentDescription = hint; setText(value); textSize = 18f
        inputType = InputType.TYPE_CLASS_TEXT or if (multiline) InputType.TYPE_TEXT_FLAG_MULTI_LINE else 0
        minLines = if (multiline) 3 else 1
        body.addView(this, LinearLayout.LayoutParams(-1, -2))
    }
    protected fun button(label: String, run: () -> Unit): Button = Button(this).apply {
        text = label; isAllCaps = false; setOnClickListener { run() }; body.addView(this)
    }
    protected fun show(title: String, data: JSONObject) {
        AlertDialog.Builder(this).setTitle(title).setMessage(data.toString(2)).setPositiveButton("关闭", null).show()
    }
    protected fun refreshMarker() { marker.text = "标记：" + Observation.marks.optString(page, "未开始") }
    override fun onResume() {
        super.onResume()
        refreshMarker()
        Observation.foreground(this, page)
        if (page == "R" && display?.displayId != null && display!!.displayId != 0 && Observation.triggerArmed) {
            Observation.triggerArmed = false
            Observation.log(this, "midtask_trigger", JSONObject().put("version", Observation.triggerVersion).put("display_id", display!!.displayId))
            Protocol.submit(this, Observation.triggerPolicy, Observation.triggerVersion)
        }
    }
    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN || event.actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
            val i = event.actionIndex
            Observation.log(this, "touch_down", JSONObject().put("page", page)
                .put("device_id", event.deviceId).put("source", event.source)
                .put("tool_type", event.getToolType(i)).put("flags", event.flags).put("pressure", event.getPressure(i)))
        }
        return super.dispatchTouchEvent(event)
    }
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) Observation.log(this, "key_down", JSONObject().put("page", page)
            .put("device_id", event.deviceId).put("source", event.source)
            .put("tool_type", JSONObject.NULL).put("flags", event.flags))
        return super.dispatchKeyEvent(event)
    }
}

class MainActivity : PageActivity() {
    override val page = "M"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        intent.getStringExtra("operator_prompt")?.let { prompt ->
            intent.removeExtra("operator_prompt")
            window.decorView.post {
                getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("操作员任务", prompt))
                Observation.log(this, "operator_prompt_prepared",
                    JSONObject().put("prompt", prompt).put("sha256", Observation.sha(prompt)))
            }
        }
        if (savedInstanceState == null) intent.getStringExtra("preflight")?.let { operation ->
            // Explicit operator preparation; never dispatches a task to Doubao.
            window.decorView.post { preflight(operation) }
        }
    }
    override fun content() {
        text("$packageName\n静态策略版本：${Observation.staticVersion(this)}")
        button("开始本轮") {
            val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            val id = EditText(this).apply { hint = "轮次号"; setText(Observation.round); layout.addView(this) }
            val stage = EditText(this).apply { hint = "阶段名"; setText("D"); layout.addView(this) }
            AlertDialog.Builder(this).setTitle("开始本轮").setView(layout)
                .setPositiveButton("开始") { _, _ ->
                    if (id.text.isNotBlank() && stage.text.isNotBlank()) {
                        Observation.begin(this, id.text.toString().trim(), stage.text.toString().trim())
                        recreate()
                    }
                }.setNegativeButton("取消", null).show()
        }
        button("导出") {
            val data = Observation.export(this)
            val name = "${Observation.round}_${Observation.phase}_${packageName}_${System.currentTimeMillis()}.json"
            try { show("导出完成", JSONObject().put("file", Protocol.save(this, name, data))) }
            catch (e: Exception) { show("导出结果", Protocol.failure(e)) }
        }
        val note = field("备注", Observation.note)
        val saved = text(if (Observation.note.isEmpty()) "" else "已保存：${Observation.note}")
        button("保存") {
            Observation.note = note.text.toString()
            saved.text = "已保存：${Observation.note}"
            Observation.business(this, page, "save", Observation.note)
        }
        listOf("输入页" to InputActivity::class.java, "记录页" to RecordActivity::class.java,
            "发布页" to PublishActivity::class.java, "签到页" to RewardActivity::class.java,
            "目标页" to TargetActivity::class.java).forEach { (label, activity) ->
            button(label) { startActivity(Intent(this, activity)) }
        }
        button("P0-2 SAEP 状态") { preflight("P0-2") }
        button("P0-3 Agent 列表及详情") { preflight("P0-3") }
        button("P0-6 审计调用形式") { preflight("P0-6") }
        val agentPackage = field("P0-3 得到的豆包包名")
        button("R1A-4 包可见性") { visibility(agentPackage.text.toString().trim()) }
        button("策略读取") {
            val result = Protocol.readPolicy(this)
            Observation.log(this, "policy_read", result)
            show("策略读取结果", result)
        }
        val policy = field("完整动态策略", multiline = true)
        val version = field("动态版本号")
        button("提交策略") {
            val v = version.text.toString().toLongOrNull()
            if (v == null) show("输入结果", JSONObject().put("error", "需要整数版本号"))
            else show("提交结果", Protocol.submit(this, policy.text.toString(), v))
        }
        val trigger = CheckBox(this).apply { text = "中途提交触发器"; isChecked = Observation.triggerArmed; body.addView(this) }
        trigger.setOnCheckedChangeListener { _, enabled ->
            val v = version.text.toString().toLongOrNull()
            if (enabled && v == null) trigger.isChecked = false
            else {
                Observation.triggerPolicy = policy.text.toString()
                Observation.triggerVersion = v ?: 0L
                Observation.triggerArmed = enabled
                Observation.log(this, "trigger_configuration", JSONObject().put("armed", enabled).put("version", v))
            }
        }
        val start = field("查询开始时间（Unix 毫秒）")
        val end = field("查询结束时间（Unix 毫秒）")
        button("查询审计日志") {
            val a = start.text.toString().toLongOrNull()
            val b = end.text.toString().toLongOrNull()
            if (a != null && b != null) {
                val result = Protocol.audit(this, a, b)
                Observation.log(this, "audit_query", JSONObject(result.toString()).apply { remove("records") })
                Protocol.save(this, "${Observation.round}_query_${System.currentTimeMillis()}.json", result)
                show("查询结果", JSONObject(result.toString()).apply { remove("records") })
            }
        }
    }

    private fun preflight(operation: String) {
        val result = when (operation) {
            "P0-2" -> Protocol.status()
            "P0-3" -> Protocol.agents()
            "P0-6" -> {
                val a = Protocol.audit(this, 0, Long.MAX_VALUE, "instance")
                val b = Protocol.audit(this, 0, Long.MAX_VALUE, "static")
                val selected = if (!a.has("exception_type")) "instance" else if (!b.has("exception_type")) "static" else null
                if (selected != null) getSharedPreferences("slab", 0).edit().putString("audit_form", selected).apply()
                JSONObject().put("instance", a).put("static", b).put("selected", selected ?: JSONObject.NULL)
            }
            else -> return
        }
        result.put("operation", operation).put("package", packageName).put("wall_time_ms", System.currentTimeMillis())
        Observation.log(this, "preflight_result", result)
        Protocol.save(this, "${operation}_${packageName}.json", result)
        show(operation, result)
    }

    @Suppress("DEPRECATION")
    private fun visibility(knownPackage: String) {
        val result = JSONObject()
        val launcher = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = packageManager.queryIntentActivities(launcher, 0)
        val entries = JSONArray()
        val matches = mutableListOf<String>()
        apps.forEach {
            val label = it.loadLabel(packageManager).toString()
            entries.put(JSONObject().put("label", label).put("package", it.activityInfo.packageName))
            if (label.contains("豆包")) matches.add(it.activityInfo.packageName)
        }
        result.put("launcher_apps", entries).put("doubao_found_by_label", matches.isNotEmpty())
        val pkg = matches.firstOrNull() ?: knownPackage
        result.put("queried_package", pkg).put("query_source", if (matches.isEmpty()) "P0-3" else "launcher_label")
        try {
            require(pkg.isNotEmpty()) { "需要先填入 P0-3 中豆包的包名" }
            val info = packageManager.getPackageInfo(pkg, 0)
            result.put("package_info", JSONObject().put("version_name", info.versionName)
                .put("version_code", info.longVersionCode).put("first_install_time", info.firstInstallTime)
                .put("last_update_time", info.lastUpdateTime))
        } catch (e: Exception) { result.put("error", Protocol.failure(e)) }
        Observation.log(this, "package_visibility", result)
        Protocol.save(this, "R1A-4_${packageName}.json", result)
        show("包可见性结果", result)
    }
}

class InputActivity : PageActivity() {
    override val page = "I"
    override fun content() {
        val field = field("输入内容", Observation.input)
        val saved = text(if (Observation.input.isEmpty()) "" else "已保存：${Observation.input}")
        button("保存") {
            Observation.input = field.text.toString(); saved.text = "已保存：${Observation.input}"
            Observation.business(this, page, "save", Observation.input)
        }
    }
}

class TargetActivity : PageActivity() {
    override val page = "T"
    override fun content() {
        val field = field("输入内容", Observation.target)
        val saved = text(if (Observation.target.isEmpty()) "" else "已保存：${Observation.target}")
        button("保存") {
            Observation.target = field.text.toString(); saved.text = "已保存：${Observation.target}"
            Observation.business(this, page, "save", Observation.target)
        }
    }
}

class RecordActivity : PageActivity() {
    override val page = "R"
    override fun content() {
        if (!Observation.recordExists) { text("暂无记录"); return }
        val title = text(Observation.recordTitle)
        val content = text(Observation.recordBody)
        val titleInput = field("标题", Observation.recordTitle).apply { visibility = View.GONE }
        val bodyInput = field("内容", Observation.recordBody, true).apply { visibility = View.GONE }
        val confirm = button("确认修改") {
            Observation.recordTitle = titleInput.text.toString(); Observation.recordBody = bodyInput.text.toString()
            title.text = Observation.recordTitle; content.text = Observation.recordBody
            titleInput.visibility = View.GONE; bodyInput.visibility = View.GONE
            Observation.business(this, page, "modify", Observation.recordTitle + Observation.recordBody)
        }.apply { visibility = View.GONE }
        button("修改") {
            titleInput.visibility = View.VISIBLE; bodyInput.visibility = View.VISIBLE; confirm.visibility = View.VISIBLE
        }
        button("删除") {
            AlertDialog.Builder(this).setTitle("删除记录").setMessage("确认删除这条记录？")
                .setPositiveButton("确认删除") { _, _ ->
                    Observation.recordExists = false
                    Observation.business(this, page, "delete")
                    recreate()
                }.setNegativeButton("取消", null).show()
        }
    }
}

class PublishActivity : PageActivity() {
    override val page = "P"
    override fun content() {
        val input = field("发布内容")
        val state = text("")
        button("发布") {
            val value = input.text.toString(); Observation.published.put(value)
            state.text = "已发布：$value"
            Observation.business(this, page, "post", value)
        }
    }
}

class RewardActivity : PageActivity() {
    override val page = "W"
    override fun content() {
        val score = text("积分 ${Observation.points}")
        button("签到领取 +50") {
            Observation.points += 50; score.text = "积分 ${Observation.points}"
            Observation.business(this, page, "claim")
        }
    }
}
