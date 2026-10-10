package org.akanework.gramophone.ui

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import org.akanework.gramophone.R
import org.akanework.gramophone.ui.adapters.BaseAdapter
import org.akanework.gramophone.ui.adapters.Sorter
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BaseAdapterClickTest {
    class TestActivity : AppCompatActivity()

    private class TestAdapter(activity: AppCompatActivity) : BaseAdapter<String>(
        null, flowOf(listOf("a")), object : Sorter.Helper<String>(setOf(Sorter.Type.ByTitleAscending)) {
            override fun getTitle(item: String) = item
            override fun getId(item: String) = item
            override fun getCover(item: String): Uri? = null
        }, null, Sorter.Type.ByTitleAscending, R.plurals.songs,
        LayoutType.COMPACT_LIST, hasMenu = false, fallbackContext = activity
    ) {
        var clicked: Pair<String, Int>? = null
        init { list = listOf("a") to listOf("a"); layoutType = LayoutType.COMPACT_LIST }
        override fun virtualTitleOf(item: String) = item
        override fun itemLayout() = R.layout.adapter_song_compact_list_card
        override fun onClick(item: String, position: Int) { clicked = item to position }
        override fun onMenu(item: String, popupMenu: PopupMenu) {}
    }

    @Test fun detachedHolderWithNoPositionDoesNotDispatchClick() {
        val controller = Robolectric.buildActivity(TestActivity::class.java)
        val activity = controller.get()
        activity.setTheme(R.style.Theme_Gramophone)
        controller.create()
        val adapter = TestAdapter(activity)
        val parent = androidx.recyclerview.widget.RecyclerView(activity)
        parent.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(activity)
        val holder = adapter.onCreateViewHolder(parent, adapter.getItemViewType(0))
        adapter.onBindViewHolder(holder, 0)
        holder.itemView.performClick()
        assertNull(adapter.clicked)
        adapter.onViewRecycled(holder)
        controller.destroy()
    }
}
