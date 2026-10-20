package com.example.gallery

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AssistantFragment : Fragment() {

    private lateinit var repo: MediaRepository
    private lateinit var shortcutAdapter: ShortcutAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_assistant, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ctx = requireContext()
        repo = MediaRepository(ctx)

        shortcutAdapter = ShortcutAdapter { s ->
            val f = when (s.key) {
                "favourites" -> Filter(Filter.KIND_FAVOURITES)
                "hidden" -> Filter(Filter.KIND_HIDDEN)
                "trash" -> Filter(Filter.KIND_TRASH)
                else -> Filter(Filter.KIND_CATEGORY, s.key)
            }
            (activity as? MainActivity)?.openPhotos(f)
        }
        val shortcuts = view.findViewById<RecyclerView>(R.id.shortcuts)
        shortcuts.layoutManager = LinearLayoutManager(ctx)
        shortcuts.adapter = shortcutAdapter

        val themeSystem = view.findViewById<RadioButton>(R.id.themeSystem)
        val themeLight = view.findViewById<RadioButton>(R.id.themeLight)
        val themeDark = view.findViewById<RadioButton>(R.id.themeDark)
        val themeAmoled = view.findViewById<RadioButton>(R.id.themeAmoled)
        when (Prefs.theme(ctx)) {
            Prefs.THEME_LIGHT -> themeLight.isChecked = true
            Prefs.THEME_DARK -> themeDark.isChecked = true
            Prefs.THEME_AMOLED -> themeAmoled.isChecked = true
            else -> themeSystem.isChecked = true
        }
        fun applyTheme(value: String) {
            if (Prefs.theme(requireContext()) == value) return
            Prefs.setTheme(requireContext(), value)
            AppCompatDelegate.setDefaultNightMode(Prefs.nightMode(requireContext()))
            requireActivity().recreate()
        }
        themeSystem.setOnClickListener { applyTheme(Prefs.THEME_SYSTEM) }
        themeLight.setOnClickListener { applyTheme(Prefs.THEME_LIGHT) }
        themeDark.setOnClickListener { applyTheme(Prefs.THEME_DARK) }
        themeAmoled.setOnClickListener { applyTheme(Prefs.THEME_AMOLED) }

        val seek = view.findViewById<SeekBar>(R.id.columnSeek)
        val valueLabel = view.findViewById<TextView>(R.id.columnValue)
        val current = Prefs.gridColumns(ctx).coerceIn(1, 6)
        seek.progress = current - 1
        valueLabel.text = "$current"
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar, progress: Int, fromUser: Boolean) {
                val cols = progress + 1
                valueLabel.text = "$cols"
                if (fromUser) Prefs.setGridColumns(requireContext(), cols)
            }
            override fun onStartTrackingTouch(sb: SeekBar) {}
            override fun onStopTrackingTouch(sb: SeekBar) {}
        })

        view.findViewById<View>(R.id.rowHidden).setOnClickListener {
            startActivity(Intent(requireContext(), HiddenActivity::class.java))
        }
        view.findViewById<View>(R.id.rowAbout).setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Gallery 3.0")
                .setMessage(
                    "Saari photos aur videos seedha aapke device se (MediaStore) load hote hain. " +
                        "Kuch bhi internet par upload nahi hota."
                )
                .setPositiveButton("OK", null)
                .show()
        }

        loadShortcuts()
    }

    override fun onResume() {
        super.onResume()
        if (::repo.isInitialized) loadShortcuts()
    }

    private fun loadShortcuts() {
        viewLifecycleOwner.lifecycleScope.launch {
            val all = withContext(Dispatchers.IO) { repo.loadAll() }
            val counts = repo.shortcutCounts(all)
            val trashed = withContext(Dispatchers.IO) { repo.loadTrashed().size }
            val list = ArrayList<Shortcut>()
            list.add(Shortcut("favourites", getString(R.string.favourites), counts["favourites"] ?: 0, R.drawable.ic_heart))
            list.add(Shortcut("hidden", getString(R.string.hidden), counts["hidden"] ?: 0, R.drawable.ic_lock))
            list.add(Shortcut("trash", getString(R.string.trash), trashed, R.drawable.ic_trash))
            list.add(Shortcut("videos", "Videos", counts["videos"] ?: 0, R.drawable.ic_videocam))
            list.add(Shortcut("screenshots", "Screenshots", counts["screenshots"] ?: 0, R.drawable.ic_screen))
            list.add(Shortcut("camera", "Camera", counts["camera"] ?: 0, R.drawable.ic_camera))
            list.add(Shortcut("downloads", "Downloads", counts["downloads"] ?: 0, R.drawable.ic_folder))
            shortcutAdapter.submit(list)

            view?.findViewById<TextView>(R.id.hiddenCount)?.text = "${counts["hidden"] ?: 0} items"
        }
    }
}
