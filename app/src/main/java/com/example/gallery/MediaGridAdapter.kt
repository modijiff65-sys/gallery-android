package com.example.gallery

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.imageview.ShapeableImageView

class MediaGridAdapter(
    private val onClick: (MediaItem) -> Unit,
    private val onLongClick: (MediaItem) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_HEADER = 0
        const val TYPE_MEDIA = 1
    }

    private val items = ArrayList<GridItem>()
    private val selected = HashSet<String>()

    var selectionMode = false
        private set

    fun submit(list: List<GridItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun mediaItems(): List<MediaItem> =
        items.filterIsInstance<GridItem.Media>().map { it.item }

    fun selectedItems(): List<MediaItem> =
        mediaItems().filter { selected.contains(it.uriString) }

    fun selectedCount(): Int = selected.size

    fun setSelectionMode(on: Boolean) {
        if (selectionMode == on) return
        selectionMode = on
        if (!on) selected.clear()
        notifyDataSetChanged()
    }

    fun selectAll() {
        mediaItems().forEach { selected.add(it.uriString) }
        notifyDataSetChanged()
    }

    fun toggleSelection(item: MediaItem) {
        if (selected.contains(item.uriString)) selected.remove(item.uriString)
        else selected.add(item.uriString)
        val pos = items.indexOfFirst {
            it is GridItem.Media && it.item.uriString == item.uriString
        }
        if (pos >= 0) notifyItemChanged(pos)
    }

    override fun getItemViewType(position: Int): Int =
        if (items[position] is GridItem.Header) TYPE_HEADER else TYPE_MEDIA

    class HeaderVH(v: View) : RecyclerView.ViewHolder(v) {
        val title: TextView = v.findViewById(R.id.sectionTitle)
    }

    class MediaVH(v: View) : RecyclerView.ViewHolder(v) {
        val thumb: ShapeableImageView = v.findViewById(R.id.thumb)
        val scrim: View = v.findViewById(R.id.selScrim)
        val play: ImageView = v.findViewById(R.id.playBadge)
        val duration: TextView = v.findViewById(R.id.duration)
        val check: ImageView = v.findViewById(R.id.checkIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inf = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_HEADER) {
            HeaderVH(inf.inflate(R.layout.item_header, parent, false))
        } else {
            MediaVH(inf.inflate(R.layout.item_media, parent, false))
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val gi = items[position]) {
            is GridItem.Header -> (holder as HeaderVH).title.text = gi.title

            is GridItem.Media -> {
                val h = holder as MediaVH
                val item = gi.item

                Glide.with(h.thumb)
                    .load(item.uri)
                    .override(420, 420)
                    .centerCrop()
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                    .into(h.thumb)

                h.play.visibility = if (item.isVideo) View.VISIBLE else View.GONE
                if (item.isVideo && item.durationMs > 0) {
                    h.duration.visibility = View.VISIBLE
                    h.duration.text = DateUtils.duration(item.durationMs)
                } else {
                    h.duration.visibility = View.GONE
                }

                val isSel = selected.contains(item.uriString)
                h.scrim.visibility = if (isSel) View.VISIBLE else View.GONE
                h.check.visibility = if (isSel) View.VISIBLE else View.GONE

                h.itemView.setOnClickListener {
                    if (selectionMode) onLongClick(item) else onClick(item)
                }
                h.itemView.setOnLongClickListener {
                    onLongClick(item)
                    true
                }
            }
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        if (holder is MediaVH) {
            Glide.with(holder.thumb).clear(holder.thumb)
        }
        super.onViewRecycled(holder)
    }
}
