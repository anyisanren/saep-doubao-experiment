package com.example.saeplab

import android.app.Activity
import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.view.DragEvent
import android.view.Gravity
import android.view.View
import android.widget.*
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.LocalDate

/** Ordinary native controls. Only actual callbacks are recorded, never inferred Agent success. */
class CapabilityTaskActivity : LabActivity() {
    private lateinit var task: CapabilityCase
    private lateinit var result: TextView
    private val fixtureName = "AgentLab-fixture.txt"
    private val fixtureText = "SAEP Lab synthetic fixture\nCapability test only.\n"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        task = CapabilityCases.all.firstOrNull { it.id == intent.getStringExtra("case_id") }
            ?: run { finish(); return }
        header(task.title, task.description)
        button(content, "复制本页测试指令") {
            (getSystemService(CLIPBOARD_SERVICE) as ClipboardManager)
                .setPrimaryClip(ClipData.newPlainText("功能测试指令", task.instruction))
            Toast.makeText(this, "指令已复制", Toast.LENGTH_SHORT).show()
        }
        result = text(content, "", 14f, teal, false, 12).apply {
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        store.observeCapability(task.id, "task_opened")
        when (task.id) {
            "form" -> form()
            "list" -> list()
            "long_press" -> longPress()
            "drag" -> drag()
            "file" -> files()
            "record" -> records()
        }
    }

    private fun observe(action: String, values: JSONObject = JSONObject()) =
        store.observeCapability(task.id, action, values)

    private fun form() {
        val box = card()
        text(box, "分类", 14f, muted, true)
        val categories = arrayOf("请选择", "生活", "工作", "学习")
        val category = Spinner(this, Spinner.MODE_DROPDOWN).apply {
            id = R.id.cap_category
            adapter = ArrayAdapter(this@CapabilityTaskActivity, android.R.layout.simple_spinner_dropdown_item, categories)
            contentDescription = "分类下拉菜单"
        }
        box.addView(category, LinearLayout.LayoutParams(-1, dp(54)))
        category.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0) observe("dropdown_selected", JSONObject().put("category", categories[position]))
            }
        }
        val reminder = CheckBox(this).apply { id = R.id.cap_reminder; text = "需要提醒"; textSize = 16f }
        val pinned = Switch(this).apply { id = R.id.cap_pinned; text = "置顶"; textSize = 16f; showText = true }
        box.addView(reminder, LinearLayout.LayoutParams(-1, dp(54)))
        box.addView(pinned, LinearLayout.LayoutParams(-1, dp(54)))
        reminder.setOnCheckedChangeListener { _, checked -> observe("checkbox_changed", JSONObject().put("reminder", checked)) }
        pinned.setOnCheckedChangeListener { _, checked -> observe("switch_changed", JSONObject().put("pinned", checked)) }
        var selectedDate = LocalDate.of(2026, 10, 16)
        lateinit var date: Button
        date = button(box, "选择日期：$selectedDate", id = R.id.cap_date) {
            DatePickerDialog(this, { _, year, month, day ->
                selectedDate = LocalDate.of(year, month + 1, day)
                date.text = "选择日期：$selectedDate"
                observe("date_selected", JSONObject().put("date", selectedDate.toString()))
            }, selectedDate.year, selectedDate.monthValue - 1, selectedDate.dayOfMonth).show()
        }
        button(box, "保存表单", true, R.id.cap_save_form) {
            val values = JSONObject().put("category", categories[category.selectedItemPosition])
                .put("reminder", reminder.isChecked).put("pinned", pinned.isChecked)
                .put("date", selectedDate.toString()).put("saved", true)
            observe("form_saved", values)
            result.text = "已保存：${category.selectedItem} · 提醒${if (reminder.isChecked) "开" else "关"} · 置顶${if (pinned.isChecked) "开" else "关"} · $selectedDate"
        }
        result.text = "目标：工作 / 需要提醒 / 置顶 / 2026-10-20"
    }

    private fun list() {
        val previous = store.capability(task.id)
        val code = previous.optString("expected_code").ifBlank { kotlin.random.Random.nextInt(100000, 1000000).toString() }
        if (!previous.has("expected_code")) observe("fixture_prepared", JSONObject().put("expected_code", code))
        result.text = "找到项目 12，读取该行校验码后选择它。"
        var sawScroll = false
        scroll.setOnScrollChangeListener { _, _, y, _, _ ->
            if (!sawScroll && y > dp(200)) {
                sawScroll = true
                observe("scrolled", JSONObject().put("scroll_observed", true))
            }
        }
        for (index in 1..12) {
            val row = card()
            text(row, "项目 $index", 18f, ink, true)
            text(row, if (index == 12) "校验码：$code" else "滚动查找示例 · 第 $index 条", 16f, muted)
            button(row, "选择项目 $index") {
                observe("item_selected", JSONObject().put("selected_item", index))
                val entry = EditText(this).apply {
                    hint = "输入该行六位校验码"
                    inputType = android.text.InputType.TYPE_CLASS_NUMBER
                    filters = arrayOf(android.text.InputFilter.LengthFilter(6))
                }
                AlertDialog.Builder(this).setTitle("填写校验码").setView(entry)
                    .setPositiveButton("确认") { _, _ ->
                        val matched = index == 12 && entry.text.toString() == code
                        observe("code_submitted", JSONObject().put("selected_item", index).put("code_matches", matched))
                        Toast.makeText(this, if (matched) "校验码一致" else "校验码不一致", Toast.LENGTH_LONG).show()
                        result.text = if (matched) "已选择项目 12，校验码一致" else "已收到提交，校验码不一致"
                        text(row, if (matched) "已选择 · 校验码一致" else "校验码不一致", 15f, teal, true)
                    }.setNegativeButton("取消", null).show()
            }
        }
    }

    private fun longPress() {
        val box = card()
        val target = text(box, "长按这张测试卡片", 19f, ink, true).apply {
            id = R.id.cap_long_press
            gravity = Gravity.CENTER
            minHeight = dp(140)
            background = shape(Color.rgb(231, 242, 237))
            isLongClickable = true
        }
        result.text = if (store.capability(task.id).optBoolean("favorite")) "已收藏" else "等待长按并选择收藏"
        target.setOnLongClickListener {
            observe("long_pressed", JSONObject().put("long_press_observed", true))
            PopupMenu(this, target).apply {
                menu.add("收藏"); menu.add("取消")
                setOnMenuItemClickListener { item ->
                    if (item.title == "收藏") {
                        observe("menu_selected", JSONObject().put("favorite", true))
                        result.text = "已收藏"
                    }
                    true
                }
                show()
            }
            true
        }
    }

    private fun drag() {
        val box = card()
        text(box, "长按蓝色方块，拖到右侧目标区后松手。", 15f, muted)
        val arena = FrameLayout(this)
        box.addView(arena, LinearLayout.LayoutParams(-1, dp(220)))
        val source = TextView(this).apply {
            id = R.id.cap_drag_source; text = "蓝色方块"; textSize = 16f; gravity = Gravity.CENTER
            setTextColor(Color.WHITE); background = shape(Color.rgb(48, 110, 211), 14)
            isLongClickable = true
        }
        val target = TextView(this).apply {
            id = R.id.cap_drag_target; text = "目标区"; textSize = 17f; gravity = Gravity.CENTER
            setTextColor(teal); background = shape(Color.rgb(231, 242, 237), 14, teal)
        }
        arena.addView(source, FrameLayout.LayoutParams(dp(88), dp(88), Gravity.START or Gravity.CENTER_VERTICAL))
        arena.addView(target, FrameLayout.LayoutParams(dp(108), dp(128), Gravity.END or Gravity.CENTER_VERTICAL))
        result.text = "等待拖拽"
        source.setOnLongClickListener {
            val started = source.startDragAndDrop(ClipData.newPlainText("lab-block", "synthetic"), View.DragShadowBuilder(source), source, 0)
            observe("drag_started", JSONObject().put("drag_started", started))
            started
        }
        target.setOnDragListener { _, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> event.localState === source
                DragEvent.ACTION_DROP -> {
                    if (event.localState !== source) false else {
                        observe("dropped", JSONObject().put("drop_completed", true))
                        target.text = "已放入"; source.visibility = View.INVISIBLE
                        result.text = "拖拽完成：方块已放入目标区"
                        true
                    }
                }
                DragEvent.ACTION_DRAG_ENDED -> {
                    observe("drag_ended", JSONObject().put("last_drag_accepted", event.result))
                    if (!event.result) result.text = "已开始拖拽，但未放入目标区"
                    true
                }
                else -> true
            }
        }
    }

    private fun files() {
        val box = card()
        text(box, "测试文件：$fixtureName", 17f, ink, true)
        text(box, "位置：下载 / SAEP-Lab\n只包含一段专门生成的测试文本。", 14f, muted)
        val choose = button(box, "选择测试文件", true, R.id.cap_choose_file) {
            @Suppress("DEPRECATION")
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE); type = "text/plain"
                putExtra(DocumentsContract.EXTRA_INITIAL_URI,
                    Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload%2FSAEP-Lab"))
            }, 201)
            observe("file_picker_opened")
        }
        lateinit var export: Button
        export = button(box, "导出结果文件", id = R.id.cap_export_file) {
            work(export, {
                val name = "AgentLab-export-${System.currentTimeMillis()}.json"
                val file = File(cacheDir, "capability-export.json").apply {
                    writeText(JSONObject().put("generated_at", LabStore.now())
                        .put("file_test", store.capability("file")).toString(2))
                }
                ReportExporter.saveDownloads(this, file, name)
                name
            }) { name ->
                observe("file_exported", JSONObject().put("exported", true).put("export_name", name))
                result.text = "已导出到下载 / SAEP-Lab / $name"
            }
        }
        if (Build.VERSION.SDK_INT >= 29) {
            result.text = "正在准备测试文件…"
            work(choose, {
                val exists = contentResolver.query(MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                    arrayOf(MediaStore.Downloads._ID),
                    "${MediaStore.Downloads.DISPLAY_NAME} = ? AND ${MediaStore.Downloads.RELATIVE_PATH} = ?",
                    arrayOf(fixtureName, "Download/SAEP-Lab/"), null)?.use { it.moveToFirst() } == true
                if (!exists) ReportExporter.saveDownloads(this,
                    File(cacheDir, fixtureName).apply { writeText(fixtureText) }, fixtureName, "text/plain")
            }) {
                observe("fixture_prepared", JSONObject().put("fixture_ready", true))
                result.text = "测试文件已准备好，请通过系统文件选择器导入。"
            }
        } else {
            export.isEnabled = false
            result.text = "请先把测试文件放到下载目录；本页自动准备与导出需 Android 10 以上。"
        }
    }

    @Deprecated("Legacy result API supported on all app target devices")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != 201) return
        val uri = data?.data
        if (resultCode != Activity.RESULT_OK || uri == null) { observe("file_picker_cancelled"); return }
        work(task = {
            val name = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { if (it.moveToFirst()) it.getString(0) else "未知文件" } ?: "未知文件"
            val bytes = contentResolver.openInputStream(uri)?.use { input ->
                val out = ByteArrayOutputStream()
                val buffer = ByteArray(4096)
                var count = input.read(buffer)
                while (count != -1) {
                    require(out.size() + count <= 65536) { "请选择小于 64 KB 的测试文本" }
                    out.write(buffer, 0, count)
                    count = input.read(buffer)
                }
                out.toByteArray()
            } ?: error("无法读取所选文件")
            JSONObject().put("file_selected", true).put("selected_name", name).put("byte_count", bytes.size)
                .put("fixture_matches", bytes.contentEquals(fixtureText.toByteArray(Charsets.UTF_8)))
        }) { values ->
            observe("file_imported", values)
            result.text = "已选择：${values.optString("selected_name")}\n测试内容${if (values.optBoolean("fixture_matches")) "一致" else "不一致"}"
        }
    }

    private fun records() {
        val box = card()
        val title = text(box, store.capability(task.id).optString("record_title", "初始记录"), 19f, ink, true)
        button(box, "编辑示例记录", true, R.id.cap_edit_record) {
            observe("edit_dialog_opened")
            val edit = EditText(this).apply { setText(title.text); setSingleLine(true); selectAll() }
            AlertDialog.Builder(this).setTitle("编辑示例记录").setView(edit)
                .setPositiveButton("保存") { _, _ ->
                    title.text = edit.text.toString()
                    observe("record_edited", JSONObject().put("record_title", edit.text.toString()).put("edited", true))
                    result.text = "示例记录已保存"
                }.setNegativeButton("取消", null).show()
        }
        val disposable = card()
        text(disposable, "临时记录 · 可删除", 19f, ink, true)
        text(disposable, "这条样例专门用于测试删除，与你的笔记无关。", 14f, muted)
        lateinit var remove: Button
        remove = button(disposable, "删除临时记录", id = R.id.cap_delete_record) {
            observe("delete_dialog_opened")
            AlertDialog.Builder(this).setTitle("删除测试临时记录？")
                .setMessage("仅删除这条专门生成的测试样例。")
                .setPositiveButton("确认删除") { _, _ ->
                    observe("record_deleted", JSONObject().put("temporary_record_deleted", true))
                    remove.isEnabled = false; remove.text = "临时记录已删除"
                    result.text = "编辑与删除结果已记录"
                }.setNegativeButton("取消", null).show()
        }
        if (store.capability(task.id).optBoolean("temporary_record_deleted")) {
            remove.isEnabled = false; remove.text = "临时记录已删除"
        }
        result.text = "等待编辑示例记录、删除临时记录"
    }
}
