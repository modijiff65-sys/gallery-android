package com.example.gallery

import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.VideoView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ViewerAdapter(private val items: List<MediaItem>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val TYPE_IMAGE = 0
        const val TYPE_VIDEO = 1
    }

    private val handler = Handler(Looper.getMainLooper())
    private val videoHolders = HashMap<Int, VideoVH>()
    private var activeVideo: VideoVH? = null

    override fun getItemViewType(position: Int): Int =
        if (items[position].isVideo) TYPE_VIDEO else TYPE_IMAGE

    class ImageVH(v: View) : RecyclerView.ViewHolder(v) {
        val img: ImageView = v.findViewById(R.id.pageImage)
    }

    inner class VideoVH(v: View) : RecyclerView.ViewHolder(v) {
        val video: VideoView = v.findViewById(R.id.video)
        val bigPlay: ImageView = v.findViewById(R.id.bigPlay)
        val controls: View = v.findViewById(R.id.videoControls)
        val seek: SeekBar = v.findViewById(R.id.seek)
        val curTime: TextView = v.findViewById(R.id.curTime)
        val totalTime: TextView = v.findViewById(R.id.totalTime)

        private var prepared = false

        private val ticker = object : Runnable {
            override fun run() {
                if (video.isPlaying) {
                    seek.progress = video.currentPosition
                    curTime.text = DateUtils.duration(video.currentPosition.toLong())
                    handler.postDelayed(this, 400)
                }
            }
        }

        private val hideControls = Runnable {
            if (video.isPlaying) {
                controls.visibility = View.GONE
                bigPlay.visibility = View.GONE
            }
        }

        fun bind(item: MediaItem) {
            prepared = false
            controls.visibility = View.GONE
            bigPlay.visibility = View.VISIBLE
            bigPlay.setImageResource(R.drawable.ic_play_circle)
            curTime.text = DateUtils.duration(0)
            seek.progress = 0

            video.setVideoURI(item.uri)
            video.setOnPreparedListener { mp: MediaPlayer ->
                prepared = true
                mp.isLooping = false
                totalTime.text = DateUtils.duration(mp.duration.toLong())
                seek.max = mp.duration
                video.seekTo(1)
            }
            video.setOnErrorListener { _, _, _ ->
                prepared = false
                bigPlay.visibility = View.VISIBLE
                bigPlay.setImageResource(R.drawable.ic_play_circle)
                true
            }
            video.setOnCompletionListener {
                handler.removeCallbacks(ticker)
                handler.removeCallbacks(hideControls)
                bigPlay.setImageResource(R.drawable.ic_play_circle)
                bigPlay.visibility = View.VISIBLE
                controls.visibility = View.VISIBLE
                curTime.text = DateUtils.duration(0)
                seek.progress = 0
            }
            bigPlay.setOnClickListener { toggle() }
            video.setOnClickListener { toggle() }
            seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(sb: SeekBar, p: Int, fromUser: Boolean) {
                    if (fromUser) {
                        video.seekTo(p)
                        curTime.text = DateUtils.duration(p.toLong())
                    }
                }
                override fun onStartTrackingTouch(sb: SeekBar) {}
                override fun onStopTrackingTouch(sb: SeekBar) {}
            })
        }

        fun toggle() {
            if (!prepared) return
            if (video.isPlaying) pause() else play()
        }

        fun play() {
            video.start()
            bigPlay.setImageResource(R.drawable.ic_pause_circle)
            bigPlay.visibility = View.VISIBLE
            controls.visibility = View.VISIBLE
            handler.removeCallbacks(ticker)
            handler.post(ticker)
            handler.removeCallbacks(hideControls)
            handler.postDelayed(hideControls, 3200)
        }

        fun pause() {
            try { if (video.isPlaying) video.pause() } catch (e: Exception) {}
            bigPlay.setImageResource(R.drawable.ic_play_circle)
            bigPlay.visibility = View.VISIBLE
            controls.visibility = View.VISIBLE
            handler.removeCallbacks(ticker)
            handler.removeCallbacks(hideControls)
        }

        fun stopAll() {
            handler.removeCallbacks(ticker)
            handler.removeCallbacks(hideControls)
            try { if (video.isPlaying) video.pause() } catch (e: Exception) {}
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inf = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_VIDEO)
            VideoVH(inf.inflate(R.layout.item_viewer_video, parent, false))
        else
            ImageVH(inf.inflate(R.layout.item_viewer_image, parent, false))
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = items[position]
        when (holder) {
            is ImageVH -> Glide.with(holder.img).load(item.uri).fitCenter().into(holder.img)
            is VideoVH -> {
                videoHolders[position] = holder
                holder.bind(item)
            }
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        when (holder) {
            is VideoVH -> {
                holder.stopAll()
                videoHolders.values.remove(holder)
                if (activeVideo === holder) activeVideo = null
            }
            is ImageVH -> Glide.with(holder.img).clear(holder.img)
        }
        super.onViewRecycled(holder)
    }

    fun onPageChanged(position: Int) {
        val v = videoHolders[position]
        if (activeVideo !== v) {
            activeVideo?.pause()
            activeVideo = v
        }
    }

    fun pauseActive() { activeVideo?.pause() }

    fun release() {
        activeVideo?.stopAll()
        videoHolders.values.forEach { it.stopAll() }
        videoHolders.clear()
        activeVideo = null
        handler.removeCallbacksAndMessages(null)
    }
}
