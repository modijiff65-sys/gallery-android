package com.example.gallery

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.Settings
import android.view.LayoutInflater
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

class PhotosFragment : Fragment() {

    companion object {
        private const val ARG_FILTER = "filter"
        private const val MAX_COLUMNS = 6

        fun newInstance(filter: Filter): PhotosFragment {
            val f = PhotosFragment()
            f.arguments = Bundle().apply { putSerializable(ARG_FILTER, filter) }
            return f
        }
    }

    private lateinit var repo: MediaRepository
    private lateinit var grid: RecyclerView
    private lateinit var adapter: MediaGridAdapter
    private lateinit var lm: GridLayoutManager
    private lateinit var sticky: StickyHeaderDecoration
    private lateinit var chips: ChipGroup
    private lateinit var pills: ChipGroup
    private lateinit var subtitle: TextView
    private lateinit var selectionBar: View
    private lateinit var selCount: TextView
    private lateinit var selectionActions: View
    private lateinit var loading: View
    private lateinit var empty: View
    private lateinit var emptyIcon: ImageView
    private lateinit var emptyTitle: TextView
    private lateinit var emptySub: TextView
    private lateinit var btnGrant: MaterialButton
    private lateinit var fab: View

    private var filter = Filter()
    private var spanCount = 3
    private var mode = DateUtils.MODE_ALL
    private var allItems: List<MediaItem> = emptyList()
    private var loadingNow = false
    private var loadedOnce = false
    private var selectionMode = false
    private var startedByLongPress = false
    private var syncingPills = false

    private val permLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        if (hasPermission()) loadMedia() else showPermissionDialog()
    }

    private val deleteLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { res ->
        if (res.resultCode == android.app.Activity.RESULT_OK) {
            toast("Trash mein bhej diya")
            exitSelection()
            loadMedia(silent = true)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_photos, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        filter = arguments?.getSerializable(ARG_FILTER) as? Filter ?: Filter()
        repo = MediaRepository(requireContext())

        subtitle = view.findViewById(R.id.subtitle)
        chips = view.findViewById(R.id.chips)
        pills = view.findViewById(R.id.hdrPills)
        selectionBar = view.findViewById(R.id.selectionBar)
        selCount = view.findViewById(R.id.selCount)
        selectionActions = view.findViewById(R.id.selectionActions)
        loading = view.findViewById(R.id.loading)
        empty = view.findViewById(R.id.empty)
        emptyIcon = view.findViewById(R.id.emptyIcon)
        emptyTitle = view.findViewById(R.id.emptyTitle)
        emptySub = view.findViewById(R.id.emptySub)
        btnGrant = view.findViewById(R.id.btnGrant)
        fab = view.findViewById(R.id.fab)
        grid = view.findViewById(R.id.grid)

        adapter = MediaGridAdapter(
            onClick = { item ->
                if (selectionMode) {
                    adapter.toggleSelection(item)
                    updateSelectionUi()
                } else {
                    openViewer(item)
                }
            },
            onLongClick = { item ->
                if (!selectionMode) {
                    selectionMode = true
                    startedByLongPress = true
                    adapter.setSelectionMode(true)
                }
                adapter.toggleSelection(item)
                updateSelectionUi()
            }
        )

        spanCount = Prefs.gridColumns(requireContext())
        lm = GridLayoutManager(requireContext(), spanCount)
        lm.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int =
                if (adapter.getItemViewType(position) == MediaGridAdapter.TYPE_HEADER) spanCount else 1
        }
        grid.layoutManager = lm
        grid.adapter = adapter

        val bg = MaterialColors.getColor(grid, R.attr.gSurface)
        sticky = StickyHeaderDecoration(adapter, requireContext(), bg)
        grid.addItemDecoration(sticky)

        setupChips()
        setupPills()
        setupPinch()

        view.findViewById<View>(R.id.hdrSearch).setOnClickListener {
            (activity as? MainActivity)?.switchTab(R.id.navSearch)
        }
        view.findViewById<View>(R.id.hdrAvatar).setOnClickListener {
            (activity as? MainActivity)?.switchTab(R.id.navAssistant)
        }
        view.findViewById<View>(R.id.hdrMulti).setOnClickListener {
            if (!selectionMode) {
                selectionMode = true
                startedByLongPress = false
                adapter.setSelectionMode(true)
            }
            updateSelectionUi()
        }
        view.findViewById<View>(R.id.btnSelClose).setOnClickListener { exitSelection() }
        view.findViewById<View>(R.id.btnSelectAll).setOnClickListener {
            adapter.selectAll()
            updateSelectionUi()
        }
        view.findViewById<View>(R.id.selShare).setOnClickListener {
            shareItems(adapter.selectedItems())
        }
        view.findViewById<View>(R.id.selDelete).setOnClickListener {
            val sel = adapter.selectedItems()
            if (sel.isEmpty()) return@setOnClickListener
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Move to trash")
                .setMessage("${sel.size} items trash mein bhejne hain?")
                .setPositiveButton("Trash") { _, _ -> trashItems(sel) }
                .setNegativeButton("Cancel", null)
                .show()
        }
        view.findViewById<View>(R.id.selHide).setOnClickListener {
            val sel = adapter.selectedItems()
            if (sel.isEmpty()) return@setOnClickListener
            val hidden = Prefs.hidden(requireContext())
            hidden.addAll(sel.map { it.uriString })
            Prefs.setHidden(requireContext(), hidden)
            toast("${sel.size} items hidden folder mein chale gaye")
            exitSelection()
            loadMedia(silent = true)
        }
        view.findViewById<View>(R.id.selFav).setOnClickListener {
            val sel = adapter.selectedItems()
            if (sel.isEmpty()) return@setOnClickListener
            val favs = Prefs.favourites(requireContext())
            favs.addAll(sel.map { it.uriString })
            Prefs.setFavourites(requireContext(), favs)
            toast("${sel.size} items favourites mein add hue")
            exitSelection()
        }
        btnGrant.setOnClickListener { requestPermissions() }
        fab.setOnClickListener { openCamera() }

        val cb = object : OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                if (selectionMode) exitSelection()
            }
        }
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, cb)
        backCallback = cb

        if (!hasPermission()) requestPermissions()
    }

    private var backCallback: OnBackPressedCallback? = null

    override fun onResume() {
        super.onResume()
        if (hasPermission()) loadMedia(silent = loadedOnce)
    }

    // ---------- chips ----------

    private fun setupChips() {
        if (filter.kind != Filter.KIND_ALL) {
            chips.visibility = View.GONE
            return
        }
        val chipBg = MaterialColors.getColor(chips, R.attr.gChipBg)
        val onSurface = MaterialColors.getColor(chips, R.attr.gOnSurface)
        val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
        val bgList = ColorStateList(states, intArrayOf(Color.parseColor("#6D5EFC"), chipBg))
        val textList = ColorStateList(states, intArrayOf(Color.WHITE, onSurface))
        for (i in 0 until chips.childCount) {
            val c = chips.getChildAt(i) as? Chip ?: continue
            c.chipBackgroundColor = bgList
            c.setTextColor(textList)
            c.chipStrokeWidth = 0f
        }
        chips.setOnCheckedStateChangeListener { _, checkedIds ->
            val id = checkedIds.firstOrNull() ?: R.id.chipAll
            mode = when (id) {
                R.id.chipYears -> DateUtils.MODE_YEARS
                R.id.chipMonths -> DateUtils.MODE_MONTHS
                R.id.chipDays -> DateUtils.MODE_DAYS
                else -> DateUtils.MODE_ALL
            }
            render()
        }
    }

    private fun setupPills() {
        val chipBg = MaterialColors.getColor(pills, R.attr.gChipBg)
        val onSurface = MaterialColors.getColor(pills, R.attr.gOnSurface)
        val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
        val bgList = ColorStateList(states, intArrayOf(Color.parseColor("#5B6BE8"), chipBg))
        val textList = ColorStateList(states, intArrayOf(Color.WHITE, onSurface))
        for (i in 0 until pills.childCount) {
            val c = pills.getChildAt(i) as? Chip ?: continue
            c.chipBackgroundColor = bgList
            c.setTextColor(textList)
            c.chipStrokeWidth = 0f
        }
        syncingPills = true
        pills.check(R.id.pillPhotos)
        syncingPills = false
        pills.setOnCheckedStateChangeListener { _, checkedIds ->
            if (syncingPills) return@setOnCheckedStateChangeListener
            when (checkedIds.firstOrNull()) {
                R.id.pillAlbums -> (activity as? MainActivity)?.switchTab(R.id.navAlbums)
                R.id.pillCloud -> toast("Cloud backup jald aa raha hai")
            }
        }
    }

    // ---------- pinch to change columns ----------

    private fun setupPinch() {
        val detector = ScaleGestureDetector(
            requireContext(),
            object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(d: ScaleGestureDetector): Boolean {
                    val target = (spanCount / d.scaleFactor).roundToInt().coerceIn(1, MAX_COLUMNS)
                    if (target != spanCount) setSpan(target)
                    return true
                }
            }
        )
        grid.setOnTouchListener { _, e ->
            detector.onTouchEvent(e)
            false
        }
    }

    private fun setSpan(n: Int) {
        spanCount = n
        lm.spanCount = n
        lm.spanSizeLookup?.invalidateSpanIndexCache()
        adapter.notifyDataSetChanged()
        Prefs.setGridColumns(requireContext(), n)
        grid.requestLayout()
    }

    // ---------- permissions ----------

    private fun requiredPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
        else arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)

    private fun hasPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES) ==
                PackageManager.PERMISSION_GRANTED
        else
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED

    private fun requestPermissions() = permLauncher.launch(requiredPermissions())

    private fun showPermissionDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.permission_title)
            .setMessage(R.string.permission_sub)
            .setPositiveButton("Settings") { _, _ ->
                startActivity(
                    Intent(
                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        Uri.fromParts("package", requireContext().packageName, null)
                    )
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    // ---------- data ----------

    private fun loadMedia(silent: Boolean = false) {
        if (!hasPermission()) {
            render()
            return
        }
        if (!silent) {
            loadingNow = true
            render()
        }
        viewLifecycleOwner.lifecycleScope.launch {
            val items = withContext(Dispatchers.IO) {
                if (filter.kind == Filter.KIND_TRASH) repo.loadTrashed() else repo.loadAll()
            }
            allItems = items
            loadingNow = false
            loadedOnce = true
            render()
        }
    }

    private fun visibleItems(): List<MediaItem> {
        val hidden = Prefs.hidden(requireContext())
        var list = allItems
        when (filter.kind) {
            Filter.KIND_ALBUM -> list = list.filter { it.bucket == filter.value }
            Filter.KIND_CATEGORY -> list = list.filter { matchesCategory(it, filter.value) }
            Filter.KIND_FAVOURITES -> {
                val favs = Prefs.favourites(requireContext())
                list = list.filter { favs.contains(it.uriString) }
            }
            Filter.KIND_RECENTS -> list = list.take(120)
            Filter.KIND_HIDDEN -> list = list.filter { hidden.contains(it.uriString) }
            Filter.KIND_TRASH -> return list
            else -> Unit
        }
        if (filter.kind != Filter.KIND_HIDDEN) {
            list = list.filter { !hidden.contains(it.uriString) }
        }
        return list
    }

    private fun matchesCategory(item: MediaItem, key: String): Boolean = when (key) {
        "videos" -> item.isVideo
        "screenshots" -> item.bucket.contains("screenshot", true) || item.name.startsWith("Screenshot", true)
        "camera" -> item.bucket.contains("camera", true)
        "downloads" -> item.bucket.contains("download", true)
        "gifs" -> item.name.endsWith(".gif", true)
        else -> true
    }

    private fun buildGridItems(list: List<MediaItem>): List<GridItem> {
        val out = ArrayList<GridItem>()
        var lastKey: String? = null
        for (m in list) {
            val key = DateUtils.groupKey(m.dateMs, mode)
            if (key != lastKey) {
                out.add(GridItem.Header(DateUtils.groupLabel(requireContext(), m.dateMs, mode)))
                lastKey = key
            }
            out.add(GridItem.Media(m))
        }
        return out
    }

    // ---------- render ----------

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

        val list = visibleItems()
        val section = when (filter.kind) {
            Filter.KIND_ALBUM -> filter.value
            Filter.KIND_CATEGORY -> filter.value.replaceFirstChar { it.uppercase() }
            Filter.KIND_FAVOURITES -> getString(R.string.favourites)
            Filter.KIND_RECENTS -> getString(R.string.recents)
            Filter.KIND_TRASH -> getString(R.string.trash)
            Filter.KIND_HIDDEN -> getString(R.string.hidden)
            else -> ""
        }
        subtitle.text = if (section.isEmpty()) plural(list.size, "item", "items")
        else section + " · " + plural(list.size, "item", "items")
        adapter.submit(buildGridItems(list))
        loading.visibility = View.GONE
        val isEmpty = list.isEmpty()
        grid.visibility = if (isEmpty) View.GONE else View.VISIBLE
        empty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        if (isEmpty) {
            when (filter.kind) {
                Filter.KIND_FAVOURITES -> setEmpty(R.drawable.ic_heart, "No favourites yet", "Kisi photo par heart dabakar add karein.", false)
                Filter.KIND_TRASH -> setEmpty(R.drawable.ic_trash, "Trash khali hai", "Trash mein bheji gayi items yahan dikhti hain.", false)
                Filter.KIND_HIDDEN -> setEmpty(R.drawable.ic_lock, "Hidden folder khali hai", "Items ko select karke Hidden par bhejein.", false)
                else -> setEmpty(R.drawable.ic_image, getString(R.string.empty_title), getString(R.string.empty_sub), false)
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

    // ---------- selection ----------

    private fun updateSelectionUi() {
        if (selectionMode && startedByLongPress && adapter.selectedCount() == 0) {
            exitSelection()
            return
        }
        backCallback?.isEnabled = selectionMode
        if (selectionMode) {
            selectionBar.visibility = View.VISIBLE
            selectionActions.visibility = View.VISIBLE
            chips.visibility = View.GONE
            fab.visibility = View.GONE
            selCount.text = "${adapter.selectedCount()} selected"
        } else {
            selectionBar.visibility = View.GONE
            selectionActions.visibility = View.GONE
            fab.visibility = View.VISIBLE
            if (filter.kind == Filter.KIND_ALL) chips.visibility = View.VISIBLE
        }
    }

    private fun exitSelection() {
        selectionMode = false
        startedByLongPress = false
        adapter.setSelectionMode(false)
        updateSelectionUi()
    }

    // ---------- actions ----------

    private fun openViewer(item: MediaItem) {
        val list = visibleItems()
        val idx = list.indexOfFirst { it.uriString == item.uriString }.coerceAtLeast(0)
        ViewerActivity.start(requireContext(), ArrayList(list), idx)
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

    private fun trashItems(items: List<MediaItem>) {
        if (items.isEmpty()) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                val uris = items.map { it.uri }
                val pi = MediaStore.createTrashRequest(contentResolver(), uris, true)
                deleteLauncher.launch(IntentSenderRequest.Builder(pi.intentSender).build())
            } catch (e: Exception) {
                toast("Trash nahi ho paya")
            }
        } else {
            var ok = 0
            items.forEach { m ->
                try {
                    if (contentResolver().delete(m.uri, null, null) > 0) ok++
                } catch (e: Exception) {}
            }
            if (ok > 0) {
                toast("$ok items delete hue")
                exitSelection()
                loadMedia(silent = true)
            } else toast("Delete nahi ho paya")
        }
    }

    private fun contentResolver() = requireContext().contentResolver

    private fun openCamera() {
        try {
            startActivity(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA))
        } catch (e: Exception) {
            toast("Camera app nahi mila")
        }
    }

    private fun toast(msg: String) = Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
}
