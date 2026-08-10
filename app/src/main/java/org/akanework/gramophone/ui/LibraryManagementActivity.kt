package org.akanework.gramophone.ui

import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.akanework.gramophone.logic.gramophoneApplication
import org.akanework.gramophone.logic.getFile

/** Explicit user tools for app-private grouping and non-destructive media hiding. */
class LibraryManagementActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "媒体库整理"
        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding((24 * resources.displayMetrics.density).toInt())
        }
        container.addView(button("选择媒体并加入分类") { chooseVisibleMedia(forCategory = true) })
        container.addView(button("选择媒体并移出媒体库") { chooseVisibleMedia(forCategory = false) })
        container.addView(button("恢复已移除内容") { restoreHiddenMedia() })
        container.addView(button("管理分类") { manageCategories() })
        setContentView(container)
    }

    private fun button(text: String, action: () -> Unit) = MaterialButton(this).apply {
        this.text = text
        setOnClickListener { action() }
    }

    private fun chooseVisibleMedia(forCategory: Boolean) = lifecycleScope.launch {
        val songs = withContext(Dispatchers.IO) { gramophoneApplication.reader.songListFlow.first() }
        val labels = songs.map { it.mediaMetadata.title?.toString() ?: it.getFile()?.name ?: it.mediaId }
        val checked = BooleanArray(songs.size)
        MaterialAlertDialogBuilder(this@LibraryManagementActivity)
            .setTitle(if (forCategory) "勾选要加入分类的媒体" else "勾选要移出媒体库的媒体")
            .setMultiChoiceItems(labels.toTypedArray(), checked) { _, index, selected -> checked[index] = selected }
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(if (forCategory) "下一步" else "移出") { _, _ ->
                val selected = songs.filterIndexed { index, _ -> checked[index] }
                if (selected.isEmpty()) return@setPositiveButton
                if (forCategory) selectCategory(selected) else {
                    gramophoneApplication.localLibraryManager.hide(selected)
                    Toast.makeText(this@LibraryManagementActivity, "已移出媒体库；手机文件未删除", Toast.LENGTH_SHORT).show()
                }
            }.show()
    }

    private fun selectCategory(items: List<androidx.media3.common.MediaItem>) {
        val categories = gramophoneApplication.localLibraryManager.state.value.categories.keys.toList()
        val options = categories + "新建分类"
        MaterialAlertDialogBuilder(this)
            .setTitle("加入分类")
            .setItems(options.toTypedArray()) { _, index ->
                if (index < categories.size) addToCategory(categories[index], items) else createCategory(items)
            }.show()
    }

    private fun createCategory(items: List<androidx.media3.common.MediaItem>) {
        val input = EditText(this).apply { inputType = InputType.TYPE_CLASS_TEXT; hint = "例如：课程" }
        MaterialAlertDialogBuilder(this).setTitle("新建分类").setView(input)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton("创建") { _, _ -> addToCategory(input.text.toString(), items) }.show()
    }

    private fun addToCategory(name: String, items: List<androidx.media3.common.MediaItem>) = runCatching {
        gramophoneApplication.localLibraryManager.addToCategory(name, items)
    }.onSuccess {
        Toast.makeText(this, "已加入分类：${name.trim()}", Toast.LENGTH_SHORT).show()
    }.onFailure { Toast.makeText(this, "分类名称不能为空", Toast.LENGTH_SHORT).show() }

    private fun restoreHiddenMedia() {
        val manager = gramophoneApplication.localLibraryManager
        if (manager.state.value.hidden.isEmpty()) {
            Toast.makeText(this, "没有已移除内容", Toast.LENGTH_SHORT).show()
            return
        }
        MaterialAlertDialogBuilder(this).setTitle("恢复已移除内容")
            .setMessage("恢复全部被移出的媒体。不会修改手机文件。")
            .setPositiveButton("全部恢复") { _, _ -> manager.restoreAll() }
            .setNegativeButton(android.R.string.cancel, null).show()
    }

    private fun manageCategories() {
        val manager = gramophoneApplication.localLibraryManager
        val categories = manager.state.value.categories
        if (categories.isEmpty()) {
            Toast.makeText(this, "暂无自定义分类", Toast.LENGTH_SHORT).show()
            return
        }
        val labels = categories.map { (name, keys) -> "$name（${keys.size}）" }.toTypedArray()
        MaterialAlertDialogBuilder(this).setTitle("管理分类").setItems(labels) { _, index ->
            val name = categories.keys.elementAt(index)
            MaterialAlertDialogBuilder(this).setTitle(name).setMessage("删除分类不会删除手机文件或媒体库内容。")
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton("删除分类") { _, _ -> manager.removeCategory(name) }.show()
        }.show()
    }
}
