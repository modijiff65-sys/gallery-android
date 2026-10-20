package com.example.gallery

import android.content.ContentValues
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
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
    private var items: List<MediaItem> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_albums, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repo = MediaRepository(requireContext())

        val quickRow = view.findViewById<LinearLayout>(R.id.quickRow)
        val categoryRow = view.findViewById<LinearLayout>(R.id.categoryRow)
        val folderGrid = view.findViewById<RecyclerView>(R.id.folderGrid)

        quickAdapter = QuickAdapter { q ->
            val kind = when (q.key) {
                "favourites" -> Filter.KIND_FAVOURITES
                "recents" -> Filter.KIND_RECENTS
                "trash" -> Filter.KIND_TRASH
                else -> Filter.KIND_ALL
            }
            (activity as? MainActivity)?.openPhotos(Filter(kind))
        }
        categoryAdapter = CategoryAdapter { c ->
            (activity as? MainActivity)?.openPhotos(Filter(Filter.KIND_CATEGORY, c.key))
        }
        albumAdapter = AlbumAdapter { a ->
            (activity as? MainActivity)?.openPhotos(Filter(Filter.KIND_ALBUM, a.name))
        }

        // quick row is built manually into a LinearLayout
        quickRow.removeAllViews()
        val quickItems = ArrayList<QuickCard>()

        val quickHost = RecyclerView(requireContext())
        quickHost.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(
            requireContext(), androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false
        )
        quickHost.adapter = quickAdapter
        quickHost.setHasFixedSize(true)
        quickRow.addView(
            quickHost,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        val catHost = RecyclerView(requireContext())
        catHost.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(
            requireContext(), androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false
        )
        catHost.adapter = categoryAdapter
        catHost.setHasFixedSize(true)
        categoryRow.addView(
            catHost,
            LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )

        folderGrid.layoutManager = GridLayoutManager(requireContext(), 2)
        folderGrid.adapter = albumAdapter

        load()
    }

    private fun load() {
        viewLifecycleOwner.lifecycleScope.launch {
            val all = withContext(Dispatchers.IO) { repo.loadAll() }
            items = all
            val favs = Prefs.favourites(requireContext())
            val hidden = Prefs.hidden(requireContext())
            val visible = all.filter { !hidden.contains(it.uriString) }

            val quick = ArrayList<QuickCard>()
            val favList = visible.filter { favs.contains(it.uriString) }
            quick.add(
                QuickCard(
                    "favourites", getString(R.string.favourites), favList.size,
                    favList.firstOrNull()?.uriString ?: "", R.drawable.ic_heart
                )
            )
            quick.add(
                QuickCard(
                    "recents", getString(R.string.recents), visible.size,
                    visible.firstOrNull()?.uriString ?: "", R.drawable.ic_refresh
                )
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                quick.add(
                    QuickCard("trash", getString(R.string.trash), 0, "", R.drawable.ic_trash)
                )
            }
            quickAdapter.submit(quick)
            categoryAdapter.submit(repo.categories(visible))
            albumAdapter.submit(repo.albums(visible))
        }
    }
}
