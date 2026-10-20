package com.example.gallery

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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

    private lateinit var pager: ViewPager2
    private lateinit var stripAdapter: StripAdapter
    private lateinit var vTitle: TextView
    private lateinit var vDate: TextView
    private lateinit var favIcon: ImageView

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
        vTitle = findViewById(R.id.vTitle)
        vDate = findViewById(R.id.vDate)
        favIcon = findViewById(R.id.actFavIcon)

        val strip = findViewById<RecyclerView>(R.id.strip)
        strip.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        stripAdapter = StripAdapter { pos -> pager.setCurrentItem(pos, true) }
        strip.adapter = stripAdapter
        stripAdapter.submit(items, index)

        pager.adapter = ViewerAdapter(items)
        pager.setCurrentItem(index, false)
        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                index = position
                updateHeader()
                stripAdapter.setSelected(position)
                strip.scrollToPosition(position)
            }
        })

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<View>(R.id.btnMore).setOnClickListener { showInfo() }
        findViewById<View>(R.id.actShare).setOnClickListener { share() }
        findViewById<View>(R.id.actFav).setOnClickListener { toggleFav() }
        findViewById<View>(R.id.actDelete).setOnClickListener { confirmDelete() }
        findViewById<View>(R.id.actInfo).setOnClickListener { showInfo() }

        updateHeader()
    }

    private fun current(): MediaItem? = items.getOrNull(index)

    private fun updateHeader() {
        val item = current() ?: return
        vTitle.text = item.name
        val df = SimpleDateFormat("dd MMM yyyy · hh:mm a", Locale.getDefault())
        vDate.text = df.format(Date(item.dateMs))
        favIcon.alpha = if (Favs.contains(this, item.uriString)) 1f else 0.45f
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
            Toast.makeText(this, "Share nahi ho paya", Toast.LENGTH_SHORT).show()
        }
    }

    private fun toggleFav() {
        val item = current() ?: return
        val added = Favs.toggle(this, item.uriString)
        favIcon.alpha = if (added) 1f else 0.45f
        Toast.makeText(
            this,
            if (added) "Favourites mein add hua" else "Favourites se hata",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun confirmDelete() {
        val item = current() ?: return
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete")
            .setMessage("Ye ${if (item.isVideo) "video" else "photo"} delete karni hai?")
            .setPositiveButton("Delete") { _, _ -> doDelete(item) }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun doDelete(item: MediaItem) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val pi = MediaStore.createDeleteRequest(contentResolver, listOf(item.uri))
                deleteLauncher.launch(IntentSenderRequest.Builder(pi.intentSender).build())
            } else {
                val n = contentResolver.delete(item.uri, null, null)
                if (n > 0) deleteCurrent()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Delete nahi ho paya", Toast.LENGTH_SHORT).show()
        }
    }

    private fun deleteCurrent() {
        if (index < items.size) items.removeAt(index)
        if (items.isEmpty()) {
            finish()
            return
        }
        if (index >= items.size) index = items.size - 1
        pager.adapter = ViewerAdapter(items)
        pager.setCurrentItem(index, false)
        stripAdapter.submit(items, index)
        updateHeader()
        Toast.makeText(this, "Delete ho gaya", Toast.LENGTH_SHORT).show()
    }

    private fun showInfo() {
        val item = current() ?: return
        val df = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val mb = item.sizeBytes / (1024.0 * 1024.0)
        val sb = StringBuilder()
        sb.append("Name: ").append(item.name).append("\n")
        sb.append("Album: ").append(item.bucket).append("\n")
        sb.append("Date: ").append(df.format(Date(item.dateMs))).append("\n")
        sb.append("Size: ")
            .append(String.format(Locale.getDefault(), "%.2f MB", mb))
            .append("\n")
        if (item.isVideo) {
            val secs = item.durationMs / 1000
            sb.append("Duration: ")
                .append(String.format(Locale.getDefault(), "%d:%02d", secs / 60, secs % 60))
                .append("\n")
            sb.append("Type: Video")
        } else {
            sb.append("Type: Image")
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Details")
            .setMessage(sb.toString())
            .setPositiveButton("OK", null)
            .show()
    }
}
