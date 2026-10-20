package com.example.gallery

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.imageview.ShapeableImageView

class StripAdapter(private val onClick: (Int) -> Unit) :
    RecyclerView.Adapter<StripAdapter.VH>() {

    private val items = ArrayList<MediaItem>()
    private var selected = 0

    fun submit(list: List<MediaItem>, sel: Int) {
        items.clear(); items.addAll(list); selected = sel; notifyDataSetChanged()
    }

    fun setSelected(i: Int) {
        val old = selected
        selected = i
        if (old in items.indices) notifyItemChanged(old)
        if (i in items.indices) notifyItemChanged(i)
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val img: ShapeableImageView = v.findViewById(R.id.stripThumb)
        val border: View = v.findViewById(R.id.stripBorder)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_strip, parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        Glide.with(holder.img).load(items[position].uri).centerCrop().into(holder.img)
        val isSel = position == selected
        holder.img.alpha = if (isSel) 1f else 0.6f
        holder.border.visibility = if (isSel) View.VISIBLE else View.GONE
        holder.itemView.setOnClickListener { onClick(position) }
    }

    override fun onViewRecycled(holder: VH) {
        Glide.with(holder.img).clear(holder.img)
        super.onViewRecycled(holder)
    }
}
