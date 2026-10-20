package com.example.gallery

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore

class MediaRepository(private val ctx: Context) {

    fun loadAll(): List<MediaItem> {
        val out = ArrayList<MediaItem>()
        out.addAll(queryImages(false))
        out.addAll(queryVideos(false))
        out.sortByDescending { it.dateMs }
        return out
    }

    fun loadTrashed(): List<MediaItem> {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) return emptyList()
        val out = ArrayList<MediaItem>()
        out.addAll(queryImages(true))
        out.addAll(queryVideos(true))
        out.sortByDescending { it.dateMs }
        return out
    }

    fun albums(items: List<MediaItem>): List<Album> =
        items.groupBy { it.bucket }
            .map { (name, list) ->
                Album(name, list.size, list.take(3).map { it.uriString })
            }
            .sortedByDescending { it.count }

    fun categories(items: List<MediaItem>): List<SmartCategory> {
        fun make(key: String, title: String, icon: Int, predicate: (MediaItem) -> Boolean): SmartCategory? {
            val list = items.filter(predicate)
            if (list.isEmpty()) return null
            return SmartCategory(key, title, list.size, list.first().uriString, icon)
        }
        val out = ArrayList<SmartCategory>()
        make("videos", "Videos", R.drawable.ic_videocam) { it.isVideo }?.let { out.add(it) }
        make("camera", "Camera", R.drawable.ic_camera) { it.bucket.contains("camera", true) }?.let { out.add(it) }
        make("screenshots", "Screenshots", R.drawable.ic_screen) {
            it.bucket.contains("screenshot", true) || it.name.startsWith("Screenshot", true)
        }?.let { out.add(it) }
        make("downloads", "Downloads", R.drawable.ic_folder) { it.bucket.contains("download", true) }?.let { out.add(it) }
        make("gifs", "GIFs", R.drawable.ic_image) { it.name.endsWith(".gif", true) }?.let { out.add(it) }
        return out
    }

    fun shortcutCounts(items: List<MediaItem>): Map<String, Int> {
        val favs = Prefs.favourites(ctx)
        return mapOf(
            "favourites" to items.count { favs.contains(it.uriString) },
            "hidden" to Prefs.hidden(ctx).size,
            "videos" to items.count { it.isVideo },
            "screenshots" to items.count {
                it.bucket.contains("screenshot", true) || it.name.startsWith("Screenshot", true)
            },
            "downloads" to items.count { it.bucket.contains("download", true) },
            "camera" to items.count { it.bucket.contains("camera", true) }
        )
    }

    private fun trashedSelection(): String? =
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R)
            "${MediaStore.MediaColumns.IS_TRASHED} = 1" else null

    private fun queryImages(trashed: Boolean): List<MediaItem> {
        val list = ArrayList<MediaItem>()
        val proj = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.RELATIVE_PATH,
            MediaStore.Images.Media.LATITUDE,
            MediaStore.Images.Media.LONGITUDE
        )
        try {
            ctx.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, proj,
                trashedSelection(), null,
                MediaStore.Images.Media.DATE_ADDED + " DESC"
            )?.use { c ->
                val idI = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameI = c.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dateI = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val bucketI = c.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                val sizeI = c.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val wI = c.getColumnIndex(MediaStore.Images.Media.WIDTH)
                val hI = c.getColumnIndex(MediaStore.Images.Media.HEIGHT)
                val mimeI = c.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)
                val pathI = c.getColumnIndex(MediaStore.Images.Media.RELATIVE_PATH)
                val latI = c.getColumnIndex(MediaStore.Images.Media.LATITUDE)
                val lonI = c.getColumnIndex(MediaStore.Images.Media.LONGITUDE)
                while (c.moveToNext()) {
                    val id = c.getLong(idI)
                    val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                    list.add(
                        MediaItem(
                            id = id, uriString = uri.toString(),
                            name = c.getString(nameI) ?: "Image",
                            dateMs = c.getLong(dateI) * 1000L,
                            bucket = c.getString(bucketI) ?: "Unknown",
                            relativePath = if (pathI >= 0) c.getString(pathI) ?: "" else "",
                            isVideo = false, durationMs = 0L,
                            sizeBytes = c.getLong(sizeI),
                            width = if (wI >= 0) c.getInt(wI) else 0,
                            height = if (hI >= 0) c.getInt(hI) else 0,
                            mimeType = if (mimeI >= 0) c.getString(mimeI) ?: "image/*" else "image/*",
                            latitude = if (latI >= 0) c.getDouble(latI) else 0.0,
                            longitude = if (lonI >= 0) c.getDouble(lonI) else 0.0
                        )
                    )
                }
            }
        } catch (e: Exception) { }
        return list
    }

    private fun queryVideos(trashed: Boolean): List<MediaItem> {
        val list = ArrayList<MediaItem>()
        val proj = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT,
            MediaStore.Video.Media.MIME_TYPE,
            MediaStore.Video.Media.RELATIVE_PATH,
            MediaStore.Video.Media.LATITUDE,
            MediaStore.Video.Media.LONGITUDE
        )
        try {
            ctx.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI, proj,
                trashedSelection(), null,
                MediaStore.Video.Media.DATE_ADDED + " DESC"
            )?.use { c ->
                val idI = c.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameI = c.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val dateI = c.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val bucketI = c.getColumnIndexOrThrow(MediaStore.Video.Media.BUCKET_DISPLAY_NAME)
                val sizeI = c.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val durI = c.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val wI = c.getColumnIndex(MediaStore.Video.Media.WIDTH)
                val hI = c.getColumnIndex(MediaStore.Video.Media.HEIGHT)
                val mimeI = c.getColumnIndex(MediaStore.Video.Media.MIME_TYPE)
                val pathI = c.getColumnIndex(MediaStore.Video.Media.RELATIVE_PATH)
                val latI = c.getColumnIndex(MediaStore.Video.Media.LATITUDE)
                val lonI = c.getColumnIndex(MediaStore.Video.Media.LONGITUDE)
                while (c.moveToNext()) {
                    val id = c.getLong(idI)
                    val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                    list.add(
                        MediaItem(
                            id = id, uriString = uri.toString(),
                            name = c.getString(nameI) ?: "Video",
                            dateMs = c.getLong(dateI) * 1000L,
                            bucket = c.getString(bucketI) ?: "Unknown",
                            relativePath = if (pathI >= 0) c.getString(pathI) ?: "" else "",
                            isVideo = true, durationMs = c.getLong(durI),
                            sizeBytes = c.getLong(sizeI),
                            width = if (wI >= 0) c.getInt(wI) else 0,
                            height = if (hI >= 0) c.getInt(hI) else 0,
                            mimeType = if (mimeI >= 0) c.getString(mimeI) ?: "video/*" else "video/*",
                            latitude = if (latI >= 0) c.getDouble(latI) else 0.0,
                            longitude = if (lonI >= 0) c.getDouble(lonI) else 0.0
                        )
                    )
                }
            }
        } catch (e: Exception) { }
        return list
    }
}
