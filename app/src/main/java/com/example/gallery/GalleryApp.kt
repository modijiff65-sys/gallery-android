package com.example.gallery

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class GalleryApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(Prefs.nightMode(this))
    }
}
