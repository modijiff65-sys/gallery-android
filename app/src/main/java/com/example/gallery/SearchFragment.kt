package com.example.gallery

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchFragment : Fragment() {

    private lateinit var repo: MediaRepository
    private lateinit var adapter: MediaGridAdapter
    private lateinit var results: RecyclerView
    private lateinit var empty: View
    private lateinit var emptyTitle: TextView
    private lateinit var emptySub: TextView

    private var allItems: List<MediaItem> = emptyList()
    private var query = ""

    private val handler = Handler(Looper.getMainLooper())
    private val debounce = Runnable { render() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_search, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repo = MediaRepository(requireContext())

        results = view.findViewById(R.id.results)
        empty = view.findViewById(R.id.empty)
        emptyTitle = view.findViewById(R.id.emptyTitle)
        emptySub = view.findViewById(R.id.emptySub)

        adapter = MediaGridAdapter(
            onClick = { item ->
                val list = filtered()
                val idx = list.indexOfFirst { it.uriString == item.uriString }.coerceAtLeast(0)
                ViewerActivity.start(requireContext(), ArrayList(list), idx)
            },
            onLongClick = { item ->
                ViewerActivity.start(requireContext(), arrayListOf(item), 0)
            }
        )
        results.layoutManager = GridLayoutManager(requireContext(), 3)
        results.adapter = adapter

        val input = view.findViewById<EditText>(R.id.searchInput)
        input.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                handler.removeCallbacks(debounce)
                handler.postDelayed(debounce, 160)
            }
        })

        viewLifecycleOwner.lifecycleScope.launch {
            val items = withContext(Dispatchers.IO) { repo.loadAll() }
            allItems = items
            render()
        }
        render()
    }

    override fun onDestroyView() {
        handler.removeCallbacks(debounce)
        super.onDestroyView()
    }

    private fun filtered(): List<MediaItem> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return emptyList()
        val hidden = Prefs.hidden(requireContext())
        return allItems.filter {
            !hidden.contains(it.uriString) &&
                (it.name.lowercase().contains(q) || it.bucket.lowercase().contains(q))
        }
    }

    private fun render() {
        query = view?.findViewById<EditText>(R.id.searchInput)?.text?.toString() ?: ""
        val list = filtered()
        adapter.submit(list.map { GridItem.Media(it) })

        val isEmpty = list.isEmpty()
        results.visibility = if (isEmpty) View.GONE else View.VISIBLE
        empty.visibility = if (isEmpty) View.VISIBLE else View.GONE
        if (isEmpty) {
            if (query.isBlank()) {
                emptyTitle.text = "Search"
                emptySub.text = "Photo ka naam ya album likhkar dhundhein."
            } else {
                emptyTitle.text = getString(R.string.no_results_title)
                emptySub.text = "“$query” ke liye kuch nahi mila."
            }
        }
    }
}
