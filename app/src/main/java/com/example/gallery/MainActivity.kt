package com.example.gallery

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtil.apply(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottomNav)
        if (savedInstanceState == null) show(0)

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navAlbums -> show(1)
                R.id.navSearch -> show(2)
                R.id.navSettings -> show(3)
                else -> show(0)
            }
            true
        }
    }

    private fun show(index: Int) {
        val fragment: Fragment = when (index) {
            1 -> AlbumsFragment()
            2 -> SearchFragment()
            3 -> SettingsFragment()
            else -> PhotosFragment.newInstance(Filter())
        }
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }

    fun switchTab(itemId: Int) {
        if (bottomNav.selectedItemId == itemId) return
        bottomNav.selectedItemId = itemId
    }

    fun openPhotos(filter: Filter) {
        bottomNav.selectedItemId = R.id.navPhotos
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, PhotosFragment.newInstance(filter))
            .commit()
    }
}
