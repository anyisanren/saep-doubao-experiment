package com.example.saeplab

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView

class NotesActivity : LabActivity() {
    private lateinit var titleInput: EditText
    private lateinit var bodyInput: EditText
    private lateinit var feedback: TextView
    private lateinit var list: LinearLayout
    private var editingId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        editingId = savedInstanceState?.getString("editing_id")
        header("普通记事", "对照页面 · 本应用未声明输入限制。")
        val form = card()
        text(form, "写一条笔记", 19f, ink, true, 18)
        titleInput = input(form, "标题", "例如：测试 A", R.id.note_title)
        bodyInput = input(form, "内容", "写下要保存的内容", R.id.note_body, true)
        button(form, "保存笔记", true, R.id.save_note) {
            val title = titleInput.text.toString().trim()
            if (title.isBlank()) { titleInput.error = "请填写标题"; return@button }
            try {
                editingId = store.saveNote(editingId, title, bodyInput.text.toString())
                feedback.text = "已保存：$title"
                (getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager).hideSoftInputFromWindow(titleInput.windowToken, 0)
                titleInput.clearFocus(); bodyInput.clearFocus(); renderNotes()
            } catch (e: Exception) { feedback.text = "保存失败：${e.message}" }
        }
        button(form, "新建另一条笔记") {
            editingId = null; titleInput.setText(""); bodyInput.setText(""); feedback.text = "新笔记尚未保存"
        }
        feedback = text(form, "尚未保存", 13f, teal, false, 0).apply { accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE }
        val instruction = card()
        prompt(instruction, "打开 Agent 实验室的普通记事，新建一条标题为测试 A、内容为你好豆包的笔记，并保存。")
        text(content, "已保存的笔记", 18f, ink, true, 12)
        list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(list)
        renderNotes()
    }

    private fun renderNotes() {
        list.removeAllViews()
        val notes = store.notes()
        if (notes.length() == 0) text(list, "还没有笔记。保存后会显示在这里。", 14f, muted)
        for (i in notes.length() - 1 downTo 0) {
            val note = notes.getJSONObject(i)
            val box = card(list)
            text(box, note.getString("title"), 17f, ink, true)
            text(box, note.getString("body").ifBlank { "（没有正文）" }, 14f, muted)
            button(box, "编辑这条笔记") {
                editingId = note.getString("id"); titleInput.setText(note.getString("title")); bodyInput.setText(note.getString("body"))
                feedback.text = "正在编辑：${note.getString("title")}"
                (content.parent as android.widget.ScrollView).smoothScrollTo(0, 0)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("editing_id", editingId); super.onSaveInstanceState(outState)
    }
}
