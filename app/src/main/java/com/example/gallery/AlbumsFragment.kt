package com.example.gallery

import android.content.ContentValues
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AlbumsFragment : Fragment() {

    private lateinit var repo: MediaRepository
    private lateinit var quickAdapter: QuickAdapter
    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var albumAdapter: AlbumAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_albums, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repo = MediaRepository(requireContext())

        val quickGrid = view.findViewById<RecyclerView>(R.id.quickGrid)
        val categoryRow = view.findViewById<RecyclerView>(R.id.categoryRow)
        val folderGrid = view.findViewById<RecyclerView>(R.id.folderGrid)

        quickAdapter = QuickAdapter { q ->
            val f = when (q.key) {
                "favourites" -> Filter(Filter.KIND_FAVOURITES)
                "recents" -> Filter(Filter.KIND_RECENTS)
                "hidden" -> Filter(Filter.KIND_HIDDEN)
                "trash" -> Filter(Filter.KIND_TRASH)
                else -> Filter()
            }
            (activity as? MainActivity)?.openPhotos(f)
        }
        categoryAdapter = CategoryAdapter { c ->
            (activity as? MainActivity)?.openPhotos(Filter(Filter.KIND_CATEGORY, c.key))
        }
        albumAdapter = AlbumAdapter { a ->
            (activity as? MainActivity)?.openPhotos(Filter(Filter.KIND_ALBUM, a.name))
        }

        quickGrid.layoutManager = GridLayoutManager(requireContext(), 3)
        quickGrid.adapter = quickAdapter
        quickGrid.setHasFixedSize(true)

        categoryRow.layoutManager = LinearLayoutManager(
            requireContext(), LinearLayoutManager.HORIZONTAL, false
        )
        categoryRow.adapter = categoryAdapter
        categoryRow.setHasFixedSize(true)

        folderGrid.layoutManager = GridLayoutManager(requireContext(), 2)
        folderGrid.adapter = albumAdapter

        // header wiring
        view.findViewById<View>(R.id.hdrSearch).setOnClickListener {
            (activity as? MainActivity)?.switchTab(R.id.navSearch)
        }
        view.findViewById<View>(R.id.hdrAvatar).setOnClickListener {
            (activity as? MainActivity)?.switchTab(R.id.navAssistant)
        }
        val pills = view.findViewById<com.google.android.material.chip.ChipGroup>(R.id.hdrPills)
        val chipBg = com.google.android.material.color.MaterialColors.getColor(pills, R.attr.gChipBg)
        val onSurface = com.google.android.material.color.MaterialColors.getColor(pills, R.attr.gOnSurface)
        val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
        val bgList = android.content.res.ColorStateList(
            states, intArrayOf(android.graphics.Color.parseColor("#5B6BE8"), chipBg)
        )
        val textList = android.content.res.ColorStateList(
            states, intArrayOf(android.graphics.Color.WHITE, onSurface)
        )
        for (i in 0 until pills.childCount) {
            val c = pills.getChildAt(i) as? com.google.android.material.chip.Chip ?: continue
            c.chipBackgroundColor = bgList
            c.setTextColor(textList)
            c.chipStrokeWidth = 0f
        }
        pills.check(R.id.pillAlbums)
        pills.setOnCheckedStateChangeListener { _, checkedIds ->
            when (checkedIds.firstOrNull()) {
                R.id.pillPhotos -> (activity as? MainActivity)?.switchTab(R.id.navPhotos)
                R.id.pillCloud -> Toast.makeText(requireContext(), "Cloud backup jald aa raha hai", Toast.LENGTH_SHORT).show()
            }
        }

        view.findViewById<View>(R.id.fabAdd).setOnClickListener { askNewAlbum() }

        load()
    }

    override fun onResume() {
        super.onResume()
        if (::repo.isInitialized) load()
    }

    private fun load() {
        viewLifecycleOwner.lifecycleScope.launch {
            val all = withContext(Dispatchers.IO) { repo.loadAll() }
            val favs = Prefs.favourites(requireContext())
            val hidden = Prefs.hidden(requireContext())
            val visible = all.filter { !hidden.contains(it.uriString) }

            val quick = ArrayList<QuickCard>()
            quick.add(
                QuickCard("favourites", getString(R.string.favourites),
                    visible.count { favs.contains(it.uriString) }, R.drawable.ic_heart)
            )
            quick.add(
                QuickCard("recents", getString(R.string.recents), visible.size, R.drawable.ic_clock)
            )
            quick.add(
                QuickCard("hidden", getString(R.string.hidden_deleted), hidden.size, R.drawable.ic_lock)
            )
            quickAdapter.submit(quick)
            categoryAdapter.submit(repo.categories(visible))
            albumAdapter.submit(repo.albums(visible))
        }
    }

    private fun askNewAlbum() {
        val input = EditText(requireContext())
        input.hint = "Album ka naam"
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Naya album")
            .setView(input)
            .setPositiveButton("Banao") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isEmpty()) return@setPositiveButton
                createAlbum(name)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun createAlbum(name: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Toast.makeText(requireContext(), "Iske liye Android 10+ chahiye", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, ".nomedia")
                put(MediaStore.MediaColumns.MIME_TYPE, "application/octet-stream")
                put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/$name")
            }
            val uri = requireContext().contentResolver.insert(
                MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY), values
            )
            if (uri != null) {
                Toast.makeText(
                    requireContext(), "Album ban gaya: Pictures/$name", Toast.LENGTH_SHORT
                ).show()
                load()
            } else {
                Toast.makeText(requireContext(), "Album nahi ban paya", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Album nahi ban paya", Toast.LENGTH_SHORT).show()
        }
    }
}
