package org.akanework.gramophone.ui

import android.net.Uri
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.flow.flowOf
import org.akanework.gramophone.R
import org.akanework.gramophone.logic.ui.MyRecyclerView
import org.akanework.gramophone.ui.adapters.BaseAdapter
import org.akanework.gramophone.ui.adapters.Sorter
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
class AuditControlledListTest {
    class TestActivity : AppCompatActivity()
    private class Rows(activity: AppCompatActivity) : BaseAdapter<String>(
        null, flowOf(listOf("a", "b", "c")), object : Sorter.Helper<String>(setOf(Sorter.Type.ByTitleAscending)) {
            override fun getTitle(item: String) = item
            override fun getId(item: String) = item
            override fun getCover(item: String): Uri? = null
        }, null, Sorter.Type.ByTitleAscending, R.plurals.songs,
        LayoutType.COMPACT_LIST, hasMenu = false, fallbackContext = activity
    ) {
        var attachCalls = 0
        override fun onAttachedToRecyclerView(recyclerView: MyRecyclerView) {
            super.onAttachedToRecyclerView(recyclerView)
            attachCalls++
        }
        var played: Pair<String, String>? = null
        var favorite: String? = null
        init { list = listOf("a","b","c") to listOf("a","b","c"); layoutType = LayoutType.COMPACT_LIST }
        override fun itemLayout() = R.layout.adapter_song_compact_list_card
        override fun virtualTitleOf(item: String) = item
        override fun onWidthChanged(width: Int) {}
        override fun onClick(item: String, position: Int) { played = item to list!!.second[position] }
        override fun onMenu(item: String, popupMenu: PopupMenu) {}
        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            super.onBindViewHolder(holder, position)
            val item = list!!.second[position]
            holder.favoriteButton!!.visibility = View.VISIBLE
            holder.favoriteButton!!.setOnClickListener { favorite = item }
        }
        fun moveFirst() {
            list = listOf("b","a","c") to listOf("b","a","c")
            notifyItemMoved(0, 1)
        }
    }
    private class Header : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun getItemCount() = 1
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = object : RecyclerView.ViewHolder(TextView(parent.context).apply {
            layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 60)
        }) {}
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {}
    }
    private fun fixture(action: (Rows, MyRecyclerView) -> Unit) {
        val controller = Robolectric.buildActivity(TestActivity::class.java)
        val activity = controller.get()
        activity.setTheme(R.style.Theme_Gramophone)
        controller.create().start().resume()
        val parent = MyRecyclerView(activity)
        val rows = Rows(activity)
        parent.layoutManager = LinearLayoutManager(activity)
        parent.adapter = ConcatAdapter(Header(), rows)
        activity.setContentView(parent)
        controller.visible().windowFocusChanged(true)
        parent.measure(View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY))
        parent.layout(0,0,400,600)
        shadowOf(Looper.getMainLooper()).idle()
        parent.measure(View.MeasureSpec.makeMeasureSpec(400, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY))
        parent.layout(0,0,400,600)
        action(rows, parent)
        parent.adapter = null
        controller.pause().stop().destroy()
    }
    @Test fun legacy177GuardDispatchesNegativeIndexAfterInvalidation() = fixture { rows, parent ->
        val first = parent.findViewHolderForAdapterPosition(1)!!
        first.itemView.setOnClickListener {
            val position = first.bindingAdapterPosition
            if (position < rows.itemCount) {
                check(position >= 0) { "legacy guard accepted negative index $position" }
            }
        }
        rows.notifyDataSetChanged()
        assertEquals(RecyclerView.NO_POSITION, first.bindingAdapterPosition)
        val failure = assertThrows(IllegalStateException::class.java) { first.itemView.performClick() }
        assertTrue(failure.message!!.contains("negative index -1"))
    }
    @Test fun concatPreventsRepeatedChildAttachCallback() = fixture { rows, _ ->
        assertEquals(1, rows.attachCalls)
    }
    @Test fun concatHeaderDoesNotShiftSongIndex() = fixture { rows, parent ->
        parent.findViewHolderForAdapterPosition(1)!!.itemView.performClick()
        assertEquals("a" to "a", rows.played)
    }
    @Test fun moveUpdatesPositionWithoutRebindingClickedObject() = fixture { rows, parent ->
        val oldFirst = parent.findViewHolderForAdapterPosition(1)!!
        rows.moveFirst()
        assertEquals(1, oldFirst.bindingAdapterPosition)
        oldFirst.itemView.performClick()
        assertEquals("a" to "a", rows.played)
        oldFirst.itemView.findViewById<View>(R.id.favorite).performClick()
        assertEquals("a", rows.favorite)
    }
    @Test fun firstFavoriteReceivesActualTouchAtItsCenter() = fixture { rows, parent ->
        val first = parent.findViewHolderForAdapterPosition(1)!!.itemView
        val button = first.findViewById<View>(R.id.favorite)
        val x = first.left + button.left + button.width / 2f
        val y = first.top + button.top + button.height / 2f
        val time = android.os.SystemClock.uptimeMillis()
        val down = MotionEvent.obtain(time,time,MotionEvent.ACTION_DOWN,x,y,0)
        val up = MotionEvent.obtain(time,time+60,MotionEvent.ACTION_UP,x,y,0)
        parent.dispatchTouchEvent(down)
        parent.dispatchTouchEvent(up)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
        assertEquals("touch x=$x y=$y first=${first.top} size=${button.width}x${button.height} parent=${parent.width} played=${rows.played}", "a", rows.favorite)
        assertNull(rows.played)
        down.recycle(); up.recycle()
    }
    @Test fun dataSetInvalidationRejectsRowClickUntilNextLayout() = fixture { rows, parent ->
        val first = parent.findViewHolderForAdapterPosition(1)!!
        rows.notifyDataSetChanged()
        assertEquals(RecyclerView.NO_POSITION, first.bindingAdapterPosition)
        first.itemView.performClick()
        assertNull(rows.played)
    }
}
