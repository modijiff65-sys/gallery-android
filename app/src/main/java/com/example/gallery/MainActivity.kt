package com.example.gallery

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class MainActivity : AppCompatActivity() {

    private lateinit var repo: MediaRepository
    private lateinit var grid: RecyclerView
    private lateinit var emptyView: View
    private lateinit var titleView: TextView
    private lateinit var toggle: MaterialButtonToggleGroup
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var mediaAdapter: MediaAdapter
    private lateinit var albumAdapter: AlbumAdapter

    private var allItems: List<MediaItem> = emptyList()
    private var tab = 0
    private var albumFilter: String? = null
    private var query = ""
    private var syncing = false

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (hasPermission()) loadMedia() else showPermissionDialog()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        repo = MediaRepository(this)
        grid = findViewById(R.id.grid)
        emptyView = findViewById(R.id.empty)
        titleView = findViewById(R.id.title)
        toggle = findViewById(R.id.segmented)
        bottomNav = findViewById(R.id.bottomNav)

        mediaAdapter = MediaAdapter { item ->
            val list = visibleItems()
            val idx = list.indexOfFirst { it.uriString == item.uriString }.coerceAtLeast(0)
            ViewerActivity.start(this, ArrayList(list), idx)
        }
        albumAdapter = AlbumAdapter { album ->
            albumFilter = album.name
            render()
        }

        grid.layoutManager = GridLayoutManager(this, 3)
        grid.adapter = mediaAdapter

        toggle.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked || syncing) return@addOnButtonCheckedListener
            tab = when (checkedId) {
                R.id.tabAlbums -> 1
                R.id.tabFavs -> 2
                else -> 0
            }
            albumFilter = null
            syncBottomNav()
            render()
        }

        bottomNav.setOnItemSelectedListener { item ->
            val t = when (item.itemId) {
                R.id.navAlbums -> 1
                R.id.navFavs -> 2
                else -> 0
            }
            if (t != tab) {
                tab = t
                albumFilter = null
                syncing = true
                toggle.check(
                    when (t) {
                        1 -> R.id.tabAlbums
                        2 -> R.id.tabFavs
                        else -> R.id.tabPhotos
                    }
                )
                syncing = false
                render()
            }
            true
        }

        findViewById<View>(R.id.btnSearch).setOnClickListener { openSearch() }
        findViewById<View>(R.id.btnMore).setOnClickListener { openAbout() }
        findViewById<View>(R.id.fab).setOnClickListener { openCamera() }

        if (!hasPermission()) requestPermissions()
    }

    override fun onResume() {
        super.onResume()
        if (hasPermission()) loadMedia()
    }

    private fun syncBottomNav() {
        bottomNav.selectedItemId = when (tab) {
            1 -> R.id.navAlbums
            2 -> R.id.navFavs
            else -> R.id.navPhotos
        }
    }

    private fun requiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun hasPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestPermissions() {
        permLauncher.launch(requiredPermissions())
    }

    private fun showPermissionDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Photos access chahiye")
            .setMessage("Gallery ko aapki photos aur videos dikhane ke liye storage access dena zaroori hai.")
            .setPositiveButton("Settings") { _, _ ->
                val i = Intent(
                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.fromParts("package", packageName, null)
                )
                startActivity(i)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun loadMedia() {
        allItems = repo.loadAll()
        render()
    }

    private fun visibleItems(): List<MediaItem> {
        var list = allItems
        val af = albumFilter
        if (af != null) list = list.filter { it.bucket == af }
        if (tab == 2) {
            val favs = Favs.get(this)
            list = list.filter { favs.contains(it.uriString) }
        }
        if (query.isNotBlank()) {
            val q = query.lowercase()
            list = list.filter {
                it.name.lowercase().contains(q) || it.bucket.lowercase().contains(q)
            }
        }
        return list
    }

    private fun render() {
        if (!hasPermission()) {
            grid.visibility = View.GONE
            emptyView.visibility = View.VISIBLE
            return
        }

        if (tab == 1 && albumFilter == null) {
            titleView.text = getString(R.string.tab_albums)
            val albums = allItems.groupBy { it.bucket }
                .map { (name, list) ->
                    Album(name, list.size, list.first().uriString, list.first().isVideo)
                }
                .sortedByDescending { it.count }
            grid.layoutManager = GridLayoutManager(this, 2)
            grid.adapter = albumAdapter
            albumAdapter.submit(albums)
            val isEmpty = albums.isEmpty()
            emptyView.visibility = if (isEmpty) View.VISIBLE else View.GONE
            grid.visibility = if (isEmpty) View.GONE else View.VISIBLE
            return
        }

        titleView.text = when {
            albumFilter != null -> albumFilter ?: ""
            tab == 2 -> getString(R.string.tab_favs)
            else -> getString(R.string.app_name)
        }

        val items = visibleItems()
        grid.layoutManager = GridLayoutManager(this, 3)
        grid.adapter = mediaAdapter
        mediaAdapter.submit(items)
        val isEmpty = items.isEmpty()
        emptyView.visibility = if (isEmpty) View.VISIBLE else View.GONE
        grid.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun openSearch() {
        val et = EditText(this)
        et.hint = "Name ya album se search karein"
        et.setText(query)
        MaterialAlertDialogBuilder(this)
            .setTitle("Search")
            .setView(et)
            .setPositiveButton("Search") { _, _ ->
                query = et.text.toString().trim()
                render()
            }
            .setNegativeButton("Clear") { _, _ ->
                query = ""
                render()
            }
            .show()
    }

    private fun openAbout() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Gallery")
            .setMessage(
                "Saari photos aur videos seedha aapke device se (MediaStore) load hote hain. " +
                    "Kuch bhi internet par upload nahi hota."
            )
            .setPositiveButton("OK", null)
            .show()
    }

    private fun openCamera() {
        try {
            startActivity(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
        } catch (e: Exception) {
            Toast.makeText(this, "Camera app nahi mila", Toast.LENGTH_SHORT).show()
        }
    }
}
