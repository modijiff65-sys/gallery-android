package com.example.gallery

import java.io.Serializable

data class Filter(
    val kind: String = KIND_ALL,
    val value: String = ""
) : Serializable {
    companion object {
        const val KIND_ALL = "all"
        const val KIND_ALBUM = "album"
        const val KIND_CATEGORY = "category"
        const val KIND_FAVOURITES = "favourites"
        const val KIND_RECENTS = "recents"
        const val KIND_TRASH = "trash"
        const val KIND_HIDDEN = "hidden"
    }
}
