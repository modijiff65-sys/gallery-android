package com.example.gallery

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.imageview.ShapeableImageView

class AlbumAdapter(private val onClick: (Album) -> Unit) :
    RecyclerView.Adapter<AlbumAdapter.VH>() {

    private val items = ArrayList<Album>()

    fun submit(list: List<Album>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val cover: ShapeableImageView = v.findViewById(R.id.cover)
        val name: TextView = v.findViewById(R.id.albumName)
        val count: TextView = v.findViewById(R.id.albumCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_album, parent, false)
        return VH(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val album = items[position]
        holder.name.text = album.name
        holder.count.text = "${album.count} items"
        Glide.with(holder.cover)
            .load(Uri.parse(album.coverUriString))
            .centerCrop()
            .into(holder.cover)
        holder.itemView.setOnClickListener { onClick(album) }
    }

    override fun onViewRecycled(holder: VH) {
        Glide.with(holder.cover).clear(holder.cover)
        super.onViewRecycled(holder)
    }
}
