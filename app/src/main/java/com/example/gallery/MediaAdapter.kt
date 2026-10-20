package com.example.gallery

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class MediaAdapter(private val onClick: (MediaItem) -> Unit) :
    RecyclerView.Adapter<MediaAdapter.VH>() {

    private val items = ArrayList<MediaItem>()

    fun submit(list: List<MediaItem>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val thumb: ImageView = v.findViewById(R.id.thumb)
        val play: ImageView = v.findViewById(R.id.playBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_media, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.play.visibility = if (item.isVideo) View.VISIBLE else View.GONE
        Glide.with(holder.thumb).load(item.uri).centerCrop().into(holder.thumb)
        holder.itemView.setOnClickListener { onClick(item) }
    }
}
