package com.example.gallery

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.imageview.ShapeableImageView

class QuickAdapter(private val onClick: (QuickCard) -> Unit) :
    RecyclerView.Adapter<QuickAdapter.VH>() {

    private val items = ArrayList<QuickCard>()

    fun submit(list: List<QuickCard>) {
        items.clear(); items.addAll(list); notifyDataSetChanged()
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val cover: ShapeableImageView = v.findViewById(R.id.quickCover)
        val icon: ImageView = v.findViewById(R.id.quickIcon)
        val title: TextView = v.findViewById(R.id.quickTitle)
        val count: TextView = v.findViewById(R.id.quickCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_quick, parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val q = items[position]
        holder.title.text = q.title
        holder.count.text = "${q.count} items"
        if (q.coverUriString.isNotEmpty()) {
            Glide.with(holder.cover).load(Uri.parse(q.coverUriString)).centerCrop().into(holder.cover)
        } else {
            holder.cover.setImageDrawable(null)
        }
        holder.icon.setImageResource(q.iconRes)
        holder.icon.visibility = View.VISIBLE
        holder.itemView.setOnClickListener { onClick(q) }
    }

    override fun onViewRecycled(holder: VH) {
        Glide.with(holder.cover).clear(holder.cover)
        super.onViewRecycled(holder)
    }
}

class CategoryAdapter(private val onClick: (SmartCategory) -> Unit) :
    RecyclerView.Adapter<CategoryAdapter.VH>() {

    private val items = ArrayList<SmartCategory>()

    fun submit(list: List<SmartCategory>) {
        items.clear(); items.addAll(list); notifyDataSetChanged()
    }

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val cover: ShapeableImageView = v.findViewById(R.id.catCover)
        val icon: ImageView = v.findViewById(R.id.catIcon)
        val title: TextView = v.findViewById(R.id.catTitle)
        val count: TextView = v.findViewById(R.id.catCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_category, parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val c = items[position]
        holder.title.text = c.title
        holder.count.text = "${c.count}"
        Glide.with(holder.cover).load(Uri.parse(c.coverUriString)).centerCrop().into(holder.cover)
        holder.icon.setImageResource(c.iconRes)
        holder.icon.visibility = View.VISIBLE
        holder.itemView.setOnClickListener { onClick(c) }
    }

    override fun onViewRecycled(holder: VH) {
        Glide.with(holder.cover).clear(holder.cover)
        super.onViewRecycled(holder)
    }
}
