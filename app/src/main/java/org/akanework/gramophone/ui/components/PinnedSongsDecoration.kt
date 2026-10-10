package org.akanework.gramophone.ui.components

import android.graphics.Canvas
import android.graphics.Paint
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors

/** Draw only: no adapter rows, queue entries or height/index changes. */
internal class PinnedSongsDecoration(
    private val owner: RecyclerView.Adapter<*>,
    private val boundary: () -> Int?,
    private val columns: () -> Int
) : RecyclerView.ItemDecoration() {
    override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val firstUnpinned = boundary() ?: return
        val columnCount = columns()
        val thickness = 3f * parent.resources.displayMetrics.density
        val paint = Paint().apply {
            color = MaterialColors.getColor(parent, androidx.appcompat.R.attr.colorPrimary)
        }
        // In a multi-column layout, mark the first unpinned cell in each column.
        // This keeps the existing grid order and does not draw across a still-pinned cell.
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            val holder = parent.getChildViewHolder(child)
            if (holder.bindingAdapter !== owner) continue
            val position = holder.bindingAdapterPosition
            if (position !in firstUnpinned until firstUnpinned + columnCount) continue
            val y = child.top + child.translationY
            canvas.drawRect(child.left + child.translationX, y,
                child.right + child.translationX, y + thickness, paint)
        }
    }
}
