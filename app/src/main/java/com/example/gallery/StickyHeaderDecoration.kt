package com.example.gallery

import android.content.Context
import android.graphics.Canvas
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class StickyHeaderDecoration(
    private val adapter: MediaGridAdapter,
    context: Context,
    private val bgColor: Int
) : RecyclerView.ItemDecoration() {

    private val headerView: TextView = LayoutInflater.from(context)
        .inflate(R.layout.item_header, null, false) as TextView

    init {
        headerView.setBackgroundColor(bgColor)
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val topChild = parent.getChildAt(0) ?: return
        val topPos = parent.getChildAdapterPosition(topChild)
        if (topPos == RecyclerView.NO_POSITION) return

        val headerPos = adapter.headerPositionFor(topPos)
        if (headerPos < 0) return
        val title = adapter.headerTitleAt(headerPos) ?: return

        headerView.text = title
        headerView.measure(
            View.MeasureSpec.makeMeasureSpec(parent.width, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        headerView.layout(0, 0, parent.width, headerView.measuredHeight)

        var y = 0f
        for (i in 0 until parent.childCount) {
            val child = parent.getChildAt(i)
            val pos = parent.getChildAdapterPosition(child)
            if (pos > headerPos && adapter.getItemViewType(pos) == MediaGridAdapter.TYPE_HEADER) {
                if (child.top < headerView.measuredHeight) {
                    y = (child.top - headerView.measuredHeight).toFloat()
                }
                break
            }
        }

        c.save()
        c.translate(0f, y)
        headerView.draw(c)
        c.restore()
    }
}
