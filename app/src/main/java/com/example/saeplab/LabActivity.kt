package com.example.saeplab

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.WindowInsets
import android.widget.*
import java.util.concurrent.Executors

abstract class LabActivity : Activity() {
    protected lateinit var content: LinearLayout
    protected lateinit var scroll: ScrollView
    protected lateinit var store: LabStore
    private val executor = Executors.newSingleThreadExecutor()
    protected val ink = Color.rgb(25, 45, 43)
    protected val muted = Color.rgb(100, 117, 112)
    protected val teal = Color.rgb(8, 117, 104)
    protected val amber = Color.rgb(146, 92, 19)
    protected val canvas = Color.rgb(244, 246, 245)

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = LabStore(this)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = canvas
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        if (Build.VERSION.SDK_INT >= 30) window.setDecorFitsSystemWindows(false)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(canvas) }
        root.setOnApplyWindowInsetsListener { view, insets ->
            if (Build.VERSION.SDK_INT >= 30) {
                val edges = insets.getInsets(WindowInsets.Type.systemBars() or WindowInsets.Type.displayCutout() or WindowInsets.Type.ime())
                view.setPadding(edges.left, edges.top, edges.right, edges.bottom)
            } else view.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop,
                insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
            insets
        }
        scroll = ScrollView(this).apply { isFillViewport = true; clipToPadding = false }
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(14), dp(22), dp(28))
        }
        scroll.addView(content, ViewGroup.LayoutParams(-1, -2))
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)
        root.requestApplyInsets()
    }

    override fun onResume() {
        super.onResume()
        store.event("page_opened", javaClass.simpleName, observationContext())
    }

    override fun onPause() {
        store.event("page_paused", javaClass.simpleName, observationContext())
        super.onPause()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (::store.isInitialized) store.event("window_focus", javaClass.simpleName,
            observationContext().put("has_focus", hasFocus))
    }

    override fun dispatchTouchEvent(event: android.view.MotionEvent): Boolean {
        if (::store.isInitialized && event.actionMasked in listOf(android.view.MotionEvent.ACTION_DOWN,
                android.view.MotionEvent.ACTION_UP, android.view.MotionEvent.ACTION_CANCEL)) {
            store.event("touch_received", javaClass.simpleName, observationContext()
                .put("action", event.actionMasked).put("x", event.x).put("y", event.y)
                .put("event_time_ms", event.eventTime).put("device_id", event.deviceId)
                .put("input_source", event.source))
        }
        return super.dispatchTouchEvent(event)
    }

    @Suppress("DEPRECATION")
    protected fun observationContext(): org.json.JSONObject {
        val screen = if (Build.VERSION.SDK_INT >= 30) display else windowManager.defaultDisplay
        return org.json.JSONObject().put("display_id", screen?.displayId)
            .put("display_name", screen?.name).put("display_flags", screen?.flags)
            .put("window_flags", window.attributes.flags)
            .put("flag_secure", window.attributes.flags and android.view.WindowManager.LayoutParams.FLAG_SECURE != 0)
            .put("task_id", taskId)
    }

    override fun onDestroy() { executor.shutdown(); super.onDestroy() }

    protected fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    protected fun shape(color: Int, radius: Int = 18, stroke: Int? = null) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
        if (stroke != null) setStroke(dp(1), stroke)
    }

    protected fun text(parent: LinearLayout, value: String, size: Float = 15f,
        color: Int = ink, bold: Boolean = false, bottom: Int = 8): TextView {
        val view = TextView(this).apply {
            this.text = value; textSize = size; setTextColor(color)
            if (bold) typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            setLineSpacing(dp(3).toFloat(), 1f)
        }
        parent.addView(view, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(bottom) })
        return view
    }

    protected fun header(title: String, subtitle: String, home: Boolean = false) {
        val top = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = android.view.Gravity.CENTER_VERTICAL }
        content.addView(top, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(18) })
        text(top, "SAEP / LAB", 12f, teal, true, 0).apply {
            letterSpacing = 0.18f
            layoutParams = LinearLayout.LayoutParams(0, -2, 1f)
        }
        if (!home) top.addView(Button(this).apply {
            this.text = "返回"; isAllCaps = false; textSize = 13f; setTextColor(teal)
            background = shape(Color.WHITE, 14); minWidth = 0; minimumWidth = 0
            setOnClickListener { finish() }
        }, LinearLayout.LayoutParams(dp(70), dp(44)))
        text(content, title, if (home) 30f else 27f, ink, true, 8)
        text(content, subtitle, 14f, muted, false, 22)
    }

    protected fun card(parent: LinearLayout = content, fill: Int = Color.WHITE): LinearLayout {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL; background = shape(fill)
            setPadding(dp(18), dp(18), dp(18), dp(18))
        }
        parent.addView(box, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) })
        return box
    }

    protected fun button(parent: LinearLayout, label: String, primary: Boolean = false,
        id: Int = View.NO_ID, action: () -> Unit): Button {
        val button = Button(this).apply {
            this.id = id; text = label; isAllCaps = false; textSize = 15f
            setTextColor(if (primary) Color.WHITE else teal)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
            background = RippleDrawable(ColorStateList.valueOf(0x22000000),
                shape(if (primary) teal else Color.rgb(231, 242, 237), 13), null)
            minimumHeight = dp(50); setPadding(dp(14), dp(8), dp(14), dp(8))
            setOnClickListener { action() }
        }
        parent.addView(button, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) })
        return button
    }

    protected fun input(parent: LinearLayout, label: String, hint: String, id: Int, multiline: Boolean = false): EditText {
        text(parent, label, 13f, muted, true, 8)
        val edit = EditText(this).apply {
            this.id = id; this.hint = hint; textSize = 16f; setTextColor(ink); setHintTextColor(muted)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = shape(Color.rgb(250, 252, 251), 12, Color.rgb(215, 226, 221))
            inputType = android.text.InputType.TYPE_CLASS_TEXT or if (multiline) android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE else 0
            if (multiline) { minLines = 4; gravity = android.view.Gravity.TOP } else setSingleLine(true)
            importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_NO
            filters = arrayOf(android.text.InputFilter.LengthFilter(if (multiline) 4000 else 120))
        }
        parent.addView(edit, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(18) })
        return edit
    }

    protected fun prompt(parent: LinearLayout, value: String) {
        text(parent, "试着对豆包说", 12f, muted, true)
        text(parent, "“$value”", 15f, ink, false, 8)
        button(parent, "复制测试指令") {
            (getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("测试指令", value))
            Toast.makeText(this, "测试指令已复制", Toast.LENGTH_SHORT).show()
        }
    }

    protected fun <T> work(trigger: Button? = null, task: () -> T, done: (T) -> Unit) {
        trigger?.isEnabled = false
        executor.execute {
            try {
                val result = task()
                runOnUiThread { if (!isDestroyed) { trigger?.isEnabled = true; done(result) } }
            } catch (e: Exception) {
                runOnUiThread { if (!isDestroyed) {
                    trigger?.isEnabled = true
                    Toast.makeText(this, "操作失败：${e.message}", Toast.LENGTH_LONG).show()
                } }
            }
        }
    }

    protected fun statusName(value: String): String = when (value) {
        "ENABLED" -> "已启用"
        "DISABLED" -> "未启用"
        "UNAVAILABLE" -> "接口不可用"
        "ERROR" -> "调用失败"
        "EMPTY_UNCONFIRMED" -> "返回为空 · 尚无法确认"
        "AVAILABLE" -> "已取得数据"
        else -> "尚未检测"
    }

    protected fun auditDescription(result: org.json.JSONObject): String = when (result.optString("status")) {
        "UNAVAILABLE" -> "当前环境无法调用审计接口，详细信息已保留在报告中。"
        "ERROR" -> "系统审计查询失败，详细原因已保留在报告中。"
        else -> result.optString("note", "查询最近 24 小时")
    }
}
