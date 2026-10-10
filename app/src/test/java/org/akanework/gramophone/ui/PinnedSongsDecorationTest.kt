package org.akanework.gramophone.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.MaterialColors
import org.akanework.gramophone.R
import org.akanework.gramophone.ui.components.PinnedSongsDecoration
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PinnedSongsDecorationTest {
    private class Rows : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun getItemCount() = 4
        override fun onCreateViewHolder(parent: ViewGroup, type: Int) =
            object : RecyclerView.ViewHolder(TextView(parent.context).apply {
                layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 80)
            }) {}
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {}
    }

    private fun render(boundary: Int?, columns: Int): Pair<Bitmap, Int> {
        val context = ContextThemeWrapper(RuntimeEnvironment.getApplication(), R.style.Theme_Gramophone)
        val parent = RecyclerView(context)
        val rows = Rows()
        parent.adapter = rows
        parent.layoutManager = GridLayoutManager(context, columns)
        parent.addItemDecoration(PinnedSongsDecoration(rows, { boundary }, { columns }))
        parent.measure(View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY))
        parent.layout(0, 0, 400, 400)
        val bitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)
        parent.draw(Canvas(bitmap))
        assertEquals(4, rows.itemCount)
        return bitmap to MaterialColors.getColor(parent, androidx.appcompat.R.attr.colorPrimary)
    }

    @Test fun listDividerUsesThemeColorAtBoundaryOnly() {
        val (bitmap, primary) = render(2, 1)
        assertEquals(primary, bitmap.getPixel(200, 161))
        assertNotEquals(primary, bitmap.getPixel(200, 81))
    }

    @Test fun emptyBoundaryDoesNotDrawExtraLine() {
        val (bitmap, primary) = render(null, 1)
        assertNotEquals(primary, bitmap.getPixel(200, 81))
    }

    @Test fun mixedGridRowSeparatesEachColumnWithoutCrossingPinnedCard() {
        val (bitmap, primary) = render(1, 2)
        assertNotEquals(primary, bitmap.getPixel(100, 1))
        assertEquals(primary, bitmap.getPixel(300, 1))
        assertEquals(primary, bitmap.getPixel(100, 81))
        assertNotEquals(primary, bitmap.getPixel(300, 81))
    }
}
