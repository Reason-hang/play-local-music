package org.akanework.gramophone.ui

import android.os.Bundle
import android.text.InputType
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.setPadding
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.MediaItem
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.getFile
import org.akanework.gramophone.logic.gramophoneApplication
import org.akanework.gramophone.logic.library.LocalLibraryManager

/** Explicit user tools for app-private grouping and non-destructive media hiding. */
class LibraryManagementActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        title = "媒体库整理"
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(24))
            addView(LinearLayout(this@LibraryManagementActivity).apply {
                orientation = LinearLayout.VERTICAL
                addView(TextView(this@LibraryManagementActivity).apply {
                    text = "整理本地媒体"
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
                    gravity = Gravity.CENTER
                })
                addView(TextView(this@LibraryManagementActivity).apply {
                    text = "所有移除操作只影响本地听歌，不会删除手机文件。"
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                    gravity = Gravity.CENTER
                    setPadding(0, dp(8), 0, dp(20))
                })
                addView(actionButton("加入分类") { chooseVisibleMedia(forCategory = true) })
                addView(actionButton("从媒体库移除") { chooseVisibleMedia(forCategory = false) })
                addView(actionButton("已移除内容") { chooseHiddenMedia() })
                addView(actionButton("管理分类") { manageCategories() })
            }, LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ))
        })
    }

    private fun actionButton(text: String, action: () -> Unit) = MaterialButton(this).apply {
        this.text = text
        isAllCaps = false
        minHeight = dp(60)
        cornerRadius = dp(28)
        layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { setMargins(0, dp(6), 0, dp(6)) }
        setOnClickListener { action() }
    }

    private fun chooseVisibleMedia(forCategory: Boolean) = lifecycleScope.launch {
        val songs = withContext(Dispatchers.IO) { gramophoneApplication.reader.songListFlow.first() }
        showMultiChoiceDialog(
            title = if (forCategory) "勾选要加入分类的媒体" else "勾选要移出的媒体",
            items = songs,
            label = ::mediaLabel,
            positiveText = if (forCategory) "下一步" else "移除"
        ) { selected ->
            if (forCategory) selectCategory(selected) else confirmHide(selected)
        }
    }

    private fun confirmHide(items: List<MediaItem>) {
        MaterialAlertDialogBuilder(this)
            .setTitle("从媒体库移除？")
            .setMessage("将从本地听歌中隐藏 ${items.size} 项媒体。手机里的原始文件不会删除，可在“已移除内容”中恢复。")
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton("移除") { _, _ ->
                gramophoneApplication.localLibraryManager.hide(items)
                Toast.makeText(this, "已移出媒体库；手机文件未删除", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun chooseHiddenMedia() {
        val manager = gramophoneApplication.localLibraryManager
        val records = manager.state.value.hiddenRecords.toList()
        when {
            records.isNotEmpty() -> showMultiChoiceDialog(
                title = "勾选要恢复的媒体",
                items = records,
                label = { it.second.title },
                positiveText = "恢复所选"
            ) { selected ->
                manager.restore(selected.map { it.first })
                Toast.makeText(this, "已恢复 ${selected.size} 项媒体；手机文件未修改", Toast.LENGTH_SHORT).show()
            }

            manager.state.value.hidden.isNotEmpty() -> MaterialAlertDialogBuilder(this)
                .setTitle("历史移除记录")
                .setMessage("这些媒体在早期版本中已从媒体库移除。由于当时未保存标题，暂不能逐项显示。全部恢复只会恢复应用内的隐藏记录，不会删除、移动或修改手机文件。")
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton("全部恢复") { _, _ -> manager.restoreAll() }
                .show()

            else -> Toast.makeText(this, "没有已移除内容", Toast.LENGTH_SHORT).show()
        }
    }

    private fun <T> showMultiChoiceDialog(
        title: String,
        items: List<T>,
        label: (T) -> String,
        positiveText: String,
        onSelected: (List<T>) -> Unit
    ) {
        val checked = BooleanArray(items.size)
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        items.forEachIndexed { index, item ->
            list.addView(CheckBox(this).apply {
                text = label(item)
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                maxLines = 2
                ellipsize = TextUtils.TruncateAt.END
                gravity = Gravity.CENTER_VERTICAL
                minHeight = dp(64)
                setPadding(dp(8), dp(6), dp(8), dp(6))
                setOnCheckedChangeListener { _, selected -> checked[index] = selected }
            })
            if (index != items.lastIndex) {
                list.addView(View(this).apply {
                    setBackgroundResource(R.drawable.dashed_list_divider)
                }, LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(1)
                ).apply { marginStart = dp(52) })
            }
        }
        val scroll = ScrollView(this).apply {
            addView(list)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (resources.displayMetrics.heightPixels * 0.52f).toInt()
            )
        }
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setView(scroll)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(positiveText) { _, _ ->
                val selected = items.filterIndexed { index, _ -> checked[index] }
                if (selected.isNotEmpty()) onSelected(selected)
            }
            .show()
    }

    private fun selectCategory(items: List<MediaItem>) {
        val categories = gramophoneApplication.localLibraryManager.state.value.categories.keys.toList()
        val options = categories + "新建分类"
        MaterialAlertDialogBuilder(this)
            .setTitle("加入分类")
            .setItems(options.toTypedArray()) { _, index ->
                if (index < categories.size) addToCategory(categories[index], items) else createCategory(items)
            }
            .show()
    }

    private fun createCategory(items: List<MediaItem>) {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT
            hint = "例如：课程"
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("新建分类")
            .setView(input)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton("创建") { _, _ -> addToCategory(input.text.toString(), items) }
            .show()
    }

    private fun addToCategory(name: String, items: List<MediaItem>) = runCatching {
        gramophoneApplication.localLibraryManager.addToCategory(name, items)
    }.onSuccess {
        Toast.makeText(this, "已加入分类：${name.trim()}", Toast.LENGTH_SHORT).show()
    }.onFailure { Toast.makeText(this, "分类名称不能为空", Toast.LENGTH_SHORT).show() }

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

    private fun mediaLabel(item: MediaItem): String =
        item.mediaMetadata.title?.toString() ?: item.getFile()?.name ?: item.mediaId

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
