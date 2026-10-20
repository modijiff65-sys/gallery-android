package com.example.gallery

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

object Prefs {
    private const val FILE = "gallery_prefs"

    const val THEME_SYSTEM = "system"
    const val THEME_LIGHT = "light"
    const val THEME_DARK = "dark"
    const val THEME_AMOLED = "amoled"

    private fun sp(ctx: Context) = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun theme(ctx: Context): String = sp(ctx).getString("theme", THEME_SYSTEM) ?: THEME_SYSTEM

    fun setTheme(ctx: Context, value: String) {
        sp(ctx).edit().putString("theme", value).apply()
    }

    fun gridColumns(ctx: Context): Int = sp(ctx).getInt("grid_columns", 3)

    fun setGridColumns(ctx: Context, value: Int) {
        sp(ctx).edit().putInt("grid_columns", value).apply()
    }

    fun isAmoled(ctx: Context) = theme(ctx) == THEME_AMOLED

    fun nightMode(ctx: Context): Int = when (theme(ctx)) {
        THEME_LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
        THEME_DARK, THEME_AMOLED -> AppCompatDelegate.MODE_NIGHT_YES
        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }

    // hidden items
    fun hidden(ctx: Context): MutableSet<String> =
        (sp(ctx).getStringSet("hidden", null) ?: emptySet<String>()).toMutableSet()

    fun setHidden(ctx: Context, values: Set<String>) {
        sp(ctx).edit().putStringSet("hidden", values).apply()
    }

    // favourites
    fun favourites(ctx: Context): MutableSet<String> =
        (sp(ctx).getStringSet("favourites", null) ?: emptySet<String>()).toMutableSet()

    fun setFavourites(ctx: Context, values: Set<String>) {
        sp(ctx).edit().putStringSet("favourites", values).apply()
    }
}
