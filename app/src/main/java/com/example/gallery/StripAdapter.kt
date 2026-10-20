package com.example.gallery

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class StripAdapter(private val onClick: (Int) -> Unit) :
    RecyclerView.Adapter<StripAdapter.VH>() {

    private val items = ArrayList<MediaItem>()
    private var selected = 0

    fun submit(list: List<MediaItem>, sel: Int) {
        items.clear()
        items.addAll(list)
        selected = sel
        notifyDataSetChanged()
    }

    fun setSelected(i: Int) {
        val old = selected
        selected = i
        if (old in items.indices) notifyItemChanged(old)
        if (i in items.indices) notifyItemChanged(i)
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val img: ImageView = v.findViewById(R.id.stripThumb)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_strip, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        Glide.with(holder.img).load(items[position].uri).centerCrop().into(holder.img)
        holder.img.alpha = if (position == selected) 1f else 0.5f
        holder.itemView.setOnClickListener { onClick(position) }
    }
}
