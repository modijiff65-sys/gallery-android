package com.example.gallery

import android.app.WallpaperManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.GestureDetector
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlin.math.abs

class ViewerActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_ITEMS = "items"
        private const val EXTRA_INDEX = "index"

        fun start(ctx: Context, items: ArrayList<MediaItem>, index: Int) {
            val i = Intent(ctx, ViewerActivity::class.java)
            i.putExtra(EXTRA_ITEMS, items)
            i.putExtra(EXTRA_INDEX, index)
            ctx.startActivity(i)
        }
    }

    private val items = ArrayList<MediaItem>()
    private var index = 0
    private var barsVisible = true

    private lateinit var pager: ViewPager2
    private lateinit var adapter: ViewerAdapter
    private lateinit var stripAdapter: StripAdapter
    private lateinit var topBar: View
    private lateinit var bottomBar: View
    private lateinit var vTitle: TextView
    private lateinit var vDate: TextView
    private lateinit var favTop: ImageView

    private val deleteLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) deleteCurrent()
    }

    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_viewer)

        val incoming = intent.getSerializableExtra(EXTRA_ITEMS) as? ArrayList<MediaItem>
        if (incoming != null) items.addAll(incoming)
        if (items.isEmpty()) {
            finish()
            return
        }
        index = intent.getIntExtra(EXTRA_INDEX, 0).coerceIn(0, items.size - 1)

        pager = findViewById(R.id.pager)
        topBar = findViewById(R.id.viewerTop)
        bottomBar = findViewById(R.id.viewerBottom)
        vTitle = findViewById(R.id.vTitle)
        vDate = findViewById(R.id.vDate)
        favTop = findViewById(R.id.btnFavTop)

        val strip = findViewById<RecyclerView>(R.id.strip)
        strip.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        stripAdapter = StripAdapter { pos -> pager.setCurrentItem(pos, true) }
        strip.adapter = stripAdapter
        stripAdapter.submit(items, index)

        adapter = ViewerAdapter(items)
        pager.adapter = adapter
        pager.setCurrentItem(index, false)
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                index = position
                adapter.onPageChanged(position)
                updateHeader()
                stripAdapter.setSelected(position)
                strip.scrollToPosition(position)
            }
        })

        val gd = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                toggleBars()
                return true
            }

            override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
                if (e1 != null) {
                    val dy = e2.y - e1.y
                    val dx = e2.x - e1.x
                    if (dy > 150 && abs(dy) > abs(dx)) {
                        finish()
                        return true
                    }
                }
                return false
            }
        })
        pager.setOnTouchListener { _, e ->
            gd.onTouchEvent(e)
            false
        }

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<View>(R.id.btnFavTop).setOnClickListener { toggleFav() }
        findViewById<View>(R.id.btnInfoTop).setOnClickListener { showInfo() }
        findViewById<View>(R.id.btnMore).setOnClickListener { showTools() }
        findViewById<View>(R.id.actShare).setOnClickListener { share() }
        findViewById<View>(R.id.actEdit).setOnClickListener { edit() }
        findViewById<View>(R.id.actDelete).setOnClickListener { confirmTrash() }
        findViewById<View>(R.id.actTools).setOnClickListener { showTools() }

        updateHeader()
    }

    override fun onPause() {
        super.onPause()
        adapter.pauseActive()
    }

    override fun onDestroy() {
        super.onDestroy()
        adapter.release()
    }

    private fun current(): MediaItem? = items.getOrNull(index)

    private fun toggleBars() {
        barsVisible = !barsVisible
        val target = if (barsVisible) 1f else 0f
        topBar.animate().alpha(target).setDuration(160).start()
        bottomBar.animate().alpha(target).setDuration(160).start()
        topBar.visibility = View.VISIBLE
        bottomBar.visibility = View.VISIBLE
        if (!barsVisible) {
            topBar.postDelayed({
                if (!barsVisible) {
                    topBar.visibility = View.GONE
                    bottomBar.visibility = View.GONE
                }
            }, 180)
        }
    }

    private fun updateHeader() {
        val item = current() ?: return
        vTitle.text = item.name
        vDate.text = DateUtils.fullStamp(item.dateMs)
        favTop.alpha = if (Prefs.favourites(this).contains(item.uriString)) 1f else 0.5f
    }

    private fun share() {
        val item = current() ?: return
        try {
            val i = Intent(Intent.ACTION_SEND).apply {
                type = if (item.isVideo) "video/*" else "image/*"
                putExtra(Intent.EXTRA_STREAM, item.uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(i, "Share via"))
        } catch (e: Exception) {
            toast("Share nahi ho paya")
        }
    }

    private fun edit() {
        val item = current() ?: return
        if (item.isVideo) {
            toast("Video editing abhi support nahi hai")
            return
        }
        EditorActivity.start(this, item)
    }

    private fun toggleFav() {
        val item = current() ?: return
        val favs = Prefs.favourites(this)
        val added = if (favs.contains(item.uriString)) {
            favs.remove(item.uriString); false
        } else {
            favs.add(item.uriString); true
        }
        Prefs.setFavourites(this, favs)
        favTop.alpha = if (added) 1f else 0.5f
        toast(if (added) "Favourites mein add hua" else "Favourites se hata")
    }

    private fun confirmTrash() {
        val item = current() ?: return
        MaterialAlertDialogBuilder(this)
            .setTitle("Move to trash")
            .setMessage("Ye ${if (item.isVideo) "video" else "photo"} trash mein bhejni hai?")
            .setPositiveButton("Trash") { _, _ -> doTrash(item) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun doTrash(item: MediaItem) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val pi = MediaStore.createTrashRequest(contentResolver, listOf(item.uri), true)
                deleteLauncher.launch(IntentSenderRequest.Builder(pi.intentSender).build())
            } else {
                val n = contentResolver.delete(item.uri, null, null)
                if (n > 0) deleteCurrent()
            }
        } catch (e: Exception) {
            toast("Trash nahi ho paya")
        }
    }

    private fun deleteCurrent() {
        if (index < items.size) items.removeAt(index)
        if (items.isEmpty()) {
            finish()
            return
        }
        if (index >= items.size) index = items.size - 1
        adapter.release()
        adapter = ViewerAdapter(items)
        pager.adapter = adapter
        pager.setCurrentItem(index, false)
        stripAdapter.submit(items, index)
        updateHeader()
        toast("Trash mein bhej diya")
    }

    private fun showTools() {
        val item = current() ?: return
        val hidden = Prefs.hidden(this).contains(item.uriString)
        val options = ArrayList<String>()
        options.add("Set as wallpaper")
        options.add(if (hidden) "Unhide" else "Hide")
        options.add("Favourite")
        options.add("Share")
        options.add("Details")
        MaterialAlertDialogBuilder(this)
            .setTitle(item.name)
            .setItems(options.toTypedArray()) { _, which ->
                when (which) {
                    0 -> setWallpaper(item)
                    1 -> toggleHidden(item)
                    2 -> toggleFav()
                    3 -> share()
                    4 -> showInfo()
                }
            }
            .show()
    }

    private fun toggleHidden(item: MediaItem) {
        val hidden = Prefs.hidden(this)
        val nowHidden = if (hidden.contains(item.uriString)) {
            hidden.remove(item.uriString); false
        } else {
            hidden.add(item.uriString); true
        }
        Prefs.setHidden(this, hidden)
        toast(if (nowHidden) "Hidden folder mein bhej diya" else "Hidden folder se hata diya")
    }

    private fun setWallpaper(item: MediaItem) {
        if (item.isVideo) {
            toast("Video wallpaper nahi ban sakta")
            return
        }
        try {
            contentResolver.openInputStream(item.uri)?.use { ins ->
                val wm = WallpaperManager.getInstance(this)
                wm.setStream(ins, null, true, WallpaperManager.FLAG_SYSTEM)
                toast("Wallpaper set ho gaya")
            }
        } catch (e: Exception) {
            toast("Wallpaper set nahi ho paya")
        }
    }

    private fun showInfo() {
        val item = current() ?: return
        val sheet = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.sheet_info, null)
        sheet.setContentView(view)
        val host = view.findViewById<LinearLayout>(R.id.infoRows)
        val (day, time) = DateUtils.dayAndTime(item.dateMs)

        val rows = ArrayList<Pair<String, String>>()
        rows.add("Name" to item.name)
        rows.add("Album" to item.bucket)
        rows.add("Date" to day)
        rows.add("Time" to time)
        rows.add("Size" to DateUtils.size(item.sizeBytes))
        if (item.width > 0 && item.height > 0) {
            rows.add("Resolution" to "${item.width} × ${item.height} • ${DateUtils.megapixels(item.width, item.height)}")
        }
        rows.add("Format" to item.mimeType)
        if (item.isVideo) rows.add("Duration" to DateUtils.duration(item.durationMs))
        if (item.relativePath.isNotEmpty()) rows.add("Path" to item.relativePath)
        rows.addAll(ExifUtils.read(this, item.uri))

        for ((label, value) in rows) {
            val row = layoutInflater.inflate(R.layout.item_info_row, host, false)
            row.findViewById<TextView>(R.id.infoLabel).text = label
            row.findViewById<TextView>(R.id.infoValue).text = value
            host.addView(row)
        }
        sheet.show()
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
