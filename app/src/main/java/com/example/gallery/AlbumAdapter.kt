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
        items.clear(); items.addAll(list); notifyDataSetChanged()
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val cover1: ShapeableImageView = v.findViewById(R.id.cover1)
        val cover2: ShapeableImageView = v.findViewById(R.id.cover2)
        val cover3: ShapeableImageView = v.findViewById(R.id.cover3)
        val name: TextView = v.findViewById(R.id.albumName)
        val count: TextView = v.findViewById(R.id.albumCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_album, parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val a = items[position]
        holder.name.text = a.name
        holder.count.text = "${a.count} photos"
        val views = listOf(holder.cover1, holder.cover2, holder.cover3)
        for (i in views.indices) {
            val url = a.covers.getOrNull(i)
            if (url != null) {
                views[i].visibility = View.VISIBLE
                Glide.with(views[i]).load(Uri.parse(url)).centerCrop().into(views[i])
            } else {
                Glide.with(views[i]).clear(views[i])
                views[i].setImageDrawable(null)
            }
        }
        holder.itemView.setOnClickListener { onClick(a) }
    }

    override fun onViewRecycled(holder: VH) {
        Glide.with(holder.cover1).clear(holder.cover1)
        Glide.with(holder.cover2).clear(holder.cover2)
        Glide.with(holder.cover3).clear(holder.cover3)
        super.onViewRecycled(holder)
    }
}
