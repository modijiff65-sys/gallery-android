package com.example.gallery

import android.content.Context

object Favs {
    private const val PREFS = "gallery_prefs"
    private const val KEY = "favourites"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun get(ctx: Context): MutableSet<String> =
        (prefs(ctx).getStringSet(KEY, null) ?: emptySet<String>()).toMutableSet()

    fun contains(ctx: Context, uri: String): Boolean = get(ctx).contains(uri)

    fun toggle(ctx: Context, uri: String): Boolean {
        val set = get(ctx)
        val added = if (set.contains(uri)) {
            set.remove(uri)
            false
        } else {
            set.add(uri)
            true
        }
        prefs(ctx).edit().putStringSet(KEY, set).apply()
        return added
    }
}
