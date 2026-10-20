package com.example.gallery

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ViewerAdapter(private val items: List<MediaItem>) :
    RecyclerView.Adapter<ViewerAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val img: ImageView = v.findViewById(R.id.pageImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_viewer, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        Glide.with(holder.img).load(items[position].uri).fitCenter().into(holder.img)
    }
}
