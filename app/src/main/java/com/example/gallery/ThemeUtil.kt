package com.example.gallery

import android.app.Activity

object ThemeUtil {
    /** Call before super.onCreate/setContentView in every activity. */
    fun apply(activity: Activity) {
        if (Prefs.isAmoled(activity)) {
            activity.setTheme(R.style.Theme_Gallery_Amoled)
        }
    }
}
