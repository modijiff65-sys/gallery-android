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
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.fragment_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val ctx = requireContext()

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

        val hiddenCount = view.findViewById<TextView>(R.id.hiddenCount)
        hiddenCount.text = "${Prefs.hidden(ctx).size} items"

        view.findViewById<View>(R.id.rowHidden).setOnClickListener {
            startActivity(Intent(requireContext(), HiddenActivity::class.java))
        }

        view.findViewById<View>(R.id.rowAbout).setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Gallery 3.0")
                .setMessage(
                    "Saari photos aur videos seedha aapke device se (MediaStore) load hote hain. " +
                        "Kuch bhi internet par upload nahi hota. Hidden folder aapke device par " +
                        "hi rehta hai."
                )
                .setPositiveButton("OK", null)
                .show()
        }
    }

    override fun onResume() {
        super.onResume()
        view?.findViewById<TextView>(R.id.hiddenCount)?.text = "${Prefs.hidden(requireContext()).size} items"
    }
}
