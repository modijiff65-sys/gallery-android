package com.example.gallery

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var repo: MediaRepository
    private lateinit var grid: RecyclerView
    private lateinit var gridAdapter: MediaGridAdapter
    private lateinit var albumAdapter: AlbumAdapter

    private lateinit var headerRow: View
    private lateinit var searchRow: View
    private lateinit var title: TextView
    private lateinit var subtitle: TextView
    private lateinit var segmented: MaterialButtonToggleGroup
    private lateinit var selectionBar: View
    private lateinit var selCount: TextView
    private lateinit var selectionActions: View
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var fab: View
    private lateinit var loading: View
    private lateinit var empty: View
    private lateinit var emptyIcon: ImageView
    private lateinit var emptyTitle: TextView
    private lateinit var emptySub: TextView
    private lateinit var btnGrant: MaterialButton
    private lateinit var searchInput: EditText

    private var allItems: List<MediaItem> = emptyList()
    private var tab = 0
    private var albumFilter: String? = null
    private var query = ""
    private var searchActive = false
    private var selectionMode = false
    private var syncing = false
    private var loadingNow = false
    private var loadedOnce = false

    private val searchHandler = Handler(Looper.getMainLooper())
    private val searchRunnable = Runnable { query = searchInput.text.toString().trim(); render() }

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (hasPermission()) loadMedia() else showPermissionDialog()
    }

    private val deleteLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { res ->
        if (res.resultCode == RESULT_OK) {
            toast("Delete ho gaya")
            exitSelection()
            loadMedia(silent = true)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        repo = MediaRepository(this)

        grid = findViewById(R.id.grid)
        headerRow = findViewById(R.id.headerRow)
        searchRow = findViewById(R.id.searchRow)
        title = findViewById(R.id.title)
        subtitle = findViewById(R.id.subtitle)
        segmented = findViewById(R.id.segmented)
        selectionBar = findViewById(R.id.selectionBar)
        selCount = findViewById(R.id.selCount)
        selectionActions = findViewById(R.id.selectionActions)
        bottomNav = findViewById(R.id.bottomNav)
        fab = findViewById(R.id.fab)
        loading = findViewById(R.id.loading)
        empty = findViewById(R.id.empty)
        emptyIcon = findViewById(R.id.emptyIcon)
        emptyTitle = findViewById(R.id.emptyTitle)
        emptySub = findViewById(R.id.emptySub)
        btnGrant = findViewById(R.id.btnGrant)
        searchInput = findViewById(R.id.searchInput)

        gridAdapter = MediaGridAdapter(
            onClick = { item ->
                if (selectionMode) {
                    gridAdapter.toggleSelection(item)
                    updateSelectionUi()
                } else {
                    openViewer(item)
                }
            },
            onLongClick = { item ->
                if (!selectionMode) {
                    selectionMode = true
                    gridAdapter.setSelectionMode(true)
                }
                gridAdapter.toggleSelection(item)
                updateSelectionUi()
            }
        )
        albumAdapter = AlbumAdapter { album ->
            albumFilter = album.name
            render()
        }

        grid.adapter = gridAdapter

        segmented.addOnButtonCheckedListener { _, checkedId, isChecked ->
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
                segmented.check(
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
        findViewById<View>(R.id.btnMore).setOnClickListener { openMore() }
        findViewById<View>(R.id.fab).setOnClickListener { openCamera() }
        findViewById<View>(R.id.btnSearchBack).setOnClickListener { closeSearch() }
        findViewById<View>(R.id.btnSearchClear).setOnClickListener {
            searchInput.setText("")
            query = ""
            render()
        }
        findViewById<View>(R.id.btnSelClose).setOnClickListener { exitSelection() }
        findViewById<View>(R.id.btnSelectAll).setOnClickListener {
            gridAdapter.selectAll()
            updateSelectionUi()
        }
        findViewById<View>(R.id.selShare).setOnClickListener {
            shareItems(gridAdapter.selectedItems())
        }
        findViewById<View>(R.id.selDelete).setOnClickListener {
            val sel = gridAdapter.selectedItems()
            if (sel.isEmpty()) return@setOnClickListener
            MaterialAlertDialogBuilder(this)
                .setTitle("Delete")
                .setMessage("${sel.size} items delete karne hain?")
                .setPositiveButton("Delete") { _, _ -> deleteItems(sel) }
                .setNegativeButton("Cancel", null)
                .show()
        }
        findViewById<View>(R.id.selFav).setOnClickListener {
            val sel = gridAdapter.selectedItems()
            if (sel.isEmpty()) return@setOnClickListener
            Favs.add(this, sel.map { it.uriString })
            toast("${sel.size} items favourites mein add hue")
            exitSelection()
            render()
        }
        btnGrant.setOnClickListener { requestPermissions() }

        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchHandler.removeCallbacks(searchRunnable)
                searchHandler.postDelayed(searchRunnable, 180)
            }
        })

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    selectionMode -> exitSelection()
                    searchActive -> closeSearch()
                    albumFilter != null -> {
                        albumFilter = null
                        render()
                    }
                    else -> {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                    }
                }
            }
        })

        if (!hasPermission()) requestPermissions()
    }

    override fun onResume() {
        super.onResume()
        if (!hasPermission()) return
        loadMedia(silent = loadedOnce)
    }

    // ---------------- permissions ----------------

    private fun requiredPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO
            )
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

    private fun hasPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }

    private fun requestPermissions() = permLauncher.launch(requiredPermissions())

    private fun showPermissionDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.permission_title)
            .setMessage(R.string.permission_sub)
            .setPositiveButton("Settings") { _, _ ->
                startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", packageName, null)
                    )
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ---------------- data ----------------

    private fun loadMedia(silent: Boolean = false) {
        if (!hasPermission()) return
        if (!silent) {
            loadingNow = true
            render()
        }
        lifecycleScope.launch {
            val items = withContext(Dispatchers.IO) { repo.loadAll() }
            allItems = items
            loadingNow = false
            loadedOnce = true
            render()
        }
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

    private fun buildGridItems(list: List<MediaItem>): List<GridItem> {
        val out = ArrayList<GridItem>()
        var lastKey: String? = null
        for (m in list) {
            val key = DateUtils.dayKey(m.dateMs)
            if (key != lastKey) {
                out.add(GridItem.Header(DateUtils.headerLabel(this, m.dateMs)))
                lastKey = key
            }
            out.add(GridItem.Media(m))
        }
        return out
    }

    private fun threeColLayout(): GridLayoutManager {
        val lm = GridLayoutManager(this, 3)
        lm.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int =
                if (gridAdapter.getItemViewType(position) == MediaGridAdapter.TYPE_HEADER) 3 else 1
        }
        return lm
    }

    // ---------------- render ----------------

    private fun render() {
        if (!hasPermission()) {
            grid.visibility = View.GONE
            loading.visibility = View.GONE
            empty.visibility = View.VISIBLE
            setEmpty(R.drawable.ic_image, getString(R.string.permission_title), getString(R.string.permission_sub), true)
            return
        }
        if (loadingNow) {
            loading.visibility = View.VISIBLE
            empty.visibility = View.GONE
            grid.visibility = View.GONE
            return
        }

        val showAlbums = tab == 1 && albumFilter == null && !searchActive
        if (showAlbums) {
            val albums = repo.albums(allItems)
            title.text = getString(R.string.tab_albums)
            subtitle.text = plural(albums.size, "album", "albums")
            grid.layoutManager = GridLayoutManager(this, 2)
            grid.adapter = albumAdapter
            albumAdapter.submit(albums)
            loading.visibility = View.GONE
            val isEmpty = albums.isEmpty()
            grid.visibility = if (isEmpty) View.GONE else View.VISIBLE
            empty.visibility = if (isEmpty) View.VISIBLE else View.GONE
            if (isEmpty) {
                setEmpty(R.drawable.ic_album, "No albums", "Aapke device par koi album nahi mila.", false)
            }
            return
        }

        val list = visibleItems()
        title.text = when {
            searchActive -> "Search"
            albumFilter != null -> albumFilter ?: ""
            tab == 2 -> getString(R.string.tab_favs)
            else -> getString(R.string.app_name)
        }
        subtitle.text = when {
            searchActive -> plural(list.size, "result", "results")
            tab == 2 -> plural(list.size, "favourite", "favourites")
            else -> plural(list.size, "item", "items")
        }

        grid.layoutManager = threeColLayout()
        grid.adapter = gridAdapter
        gridAdapter.submit(buildGridItems(list))

        loading.visibility = View.GONE
        val isEmpty = list.isEmpty()
        grid.visibility = if (isEmpty) View.GONE else View.VISIBLE
        empty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        if (isEmpty) {
            when {
                searchActive -> setEmpty(
                    R.drawable.ic_search,
                    getString(R.string.no_results_title),
                    "“$query” ke liye kuch nahi mila. Doosra naam try karein.",
                    false
                )
                tab == 2 -> setEmpty(
                    R.drawable.ic_heart,
                    "No favourites yet",
                    "Kisi photo par heart dabakar use favourites mein add karein.",
                    false
                )
                else -> setEmpty(
                    R.drawable.ic_image,
                    getString(R.string.empty_title),
                    getString(R.string.empty_sub),
                    false
                )
            }
        }
    }

    private fun setEmpty(icon: Int, t: String, sub: String, grant: Boolean) {
        emptyIcon.setImageResource(icon)
        emptyTitle.text = t
        emptySub.text = sub
        btnGrant.visibility = if (grant) View.VISIBLE else View.GONE
    }

    private fun plural(n: Int, one: String, many: String) = "$n " + if (n == 1) one else many

    // ---------------- search ----------------

    private fun openSearch() {
        searchActive = true
        headerRow.visibility = View.GONE
        segmented.visibility = View.GONE
        searchRow.visibility = View.VISIBLE
        searchInput.requestFocus()
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showSoftInput(searchInput, InputMethodManager.SHOW_IMPLICIT)
        render()
    }

    private fun closeSearch() {
        searchActive = false
        query = ""
        searchInput.setText("")
        searchRow.visibility = View.GONE
        headerRow.visibility = View.VISIBLE
        if (!selectionMode) segmented.visibility = View.VISIBLE
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchInput.windowToken, 0)
        render()
    }

    // ---------------- selection ----------------

    private fun updateSelectionUi() {
        if (selectionMode && gridAdapter.selectedCount() == 0) {
            selectionMode = false
            gridAdapter.setSelectionMode(false)
        }
        if (selectionMode) {
            headerRow.visibility = View.GONE
            searchRow.visibility = View.GONE
            segmented.visibility = View.GONE
            selectionBar.visibility = View.VISIBLE
            selectionActions.visibility = View.VISIBLE
            bottomNav.visibility = View.GONE
            fab.visibility = View.GONE
            selCount.text = "${gridAdapter.selectedCount()} selected"
        } else {
            selectionBar.visibility = View.GONE
            selectionActions.visibility = View.GONE
            bottomNav.visibility = View.VISIBLE
            fab.visibility = View.VISIBLE
            if (searchActive) {
                headerRow.visibility = View.GONE
                segmented.visibility = View.GONE
                searchRow.visibility = View.VISIBLE
            } else {
                headerRow.visibility = View.VISIBLE
                segmented.visibility = View.VISIBLE
            }
        }
    }

    private fun exitSelection() {
        selectionMode = false
        gridAdapter.setSelectionMode(false)
        updateSelectionUi()
    }

    // ---------------- actions ----------------

    private fun openViewer(item: MediaItem) {
        val list = visibleItems()
        val idx = list.indexOfFirst { it.uriString == item.uriString }.coerceAtLeast(0)
        ViewerActivity.start(this, ArrayList(list), idx)
    }

    private fun shareItems(items: List<MediaItem>) {
        if (items.isEmpty()) return
        try {
            if (items.size == 1) {
                val m = items[0]
                val i = Intent(Intent.ACTION_SEND).apply {
                    type = if (m.isVideo) "video/*" else "image/*"
                    putExtra(Intent.EXTRA_STREAM, m.uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(i, "Share via"))
            } else {
                val uris = ArrayList<Uri>()
                items.forEach { uris.add(it.uri) }
                val i = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = "*/*"
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                startActivity(Intent.createChooser(i, "Share via"))
            }
        } catch (e: Exception) {
            toast("Share nahi ho paya")
        }
    }

    private fun deleteItems(items: List<MediaItem>) {
        if (items.isEmpty()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val uris = items.map { it.uri }
                val pi = MediaStore.createDeleteRequest(contentResolver, uris)
                deleteLauncher.launch(IntentSenderRequest.Builder(pi.intentSender).build())
            } catch (e: Exception) {
                toast("Delete nahi ho paya")
            }
        } else {
            var ok = 0
            items.forEach { m ->
                try {
                    if (contentResolver.delete(m.uri, null, null) > 0) ok++
                } catch (e: Exception) {
                    // ignore
                }
            }
            if (ok > 0) {
                toast("$ok items delete hue")
                exitSelection()
                loadMedia(silent = true)
            } else {
                toast("Delete nahi ho paya")
            }
        }
    }

    private fun openMore() {
        val sheet = BottomSheetDialog(this)
        val v = layoutInflater.inflate(R.layout.sheet_more, null)
        sheet.setContentView(v)
        v.findViewById<View>(R.id.sheetRefresh).setOnClickListener {
            sheet.dismiss()
            loadMedia()
            toast("Refresh ho gaya")
        }
        v.findViewById<View>(R.id.sheetAbout).setOnClickListener {
            sheet.dismiss()
            MaterialAlertDialogBuilder(this)
                .setTitle("Gallery")
                .setMessage(
                    "Saari photos aur videos seedha aapke device se (MediaStore) load hote hain. " +
                        "Kuch bhi internet par upload nahi hota. Version 2.0"
                )
                .setPositiveButton("OK", null)
                .show()
        }
        sheet.show()
    }

    private fun openCamera() {
        try {
            startActivity(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
        } catch (e: Exception) {
            toast("Camera app nahi mila")
        }
    }

    private fun syncBottomNav() {
        bottomNav.selectedItemId = when (tab) {
            1 -> R.id.navAlbums
            2 -> R.id.navFavs
            else -> R.id.navPhotos
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
