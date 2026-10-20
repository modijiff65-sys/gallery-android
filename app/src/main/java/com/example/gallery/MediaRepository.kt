package com.example.gallery

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore

class MediaRepository(private val ctx: Context) {

    fun loadAll(): List<MediaItem> {
        val out = ArrayList<MediaItem>()
        out.addAll(queryImages())
        out.addAll(queryVideos())
        out.sortByDescending { it.dateMs }
        return out
    }

    fun albums(items: List<MediaItem>): List<Album> =
        items.groupBy { it.bucket }
            .map { (name, list) ->
                Album(name, list.size, list.first().uriString, list.first().isVideo)
            }
            .sortedByDescending { it.count }

    private fun queryImages(): List<MediaItem> {
        val list = ArrayList<MediaItem>()
        val proj = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.SIZE,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT
        )
        try {
            ctx.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                proj, null, null,
                MediaStore.Images.Media.DATE_ADDED + " DESC"
            )?.use { c ->
                val idI = c.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameI = c.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val dateI = c.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val bucketI = c.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
                val sizeI = c.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val wI = c.getColumnIndex(MediaStore.Images.Media.WIDTH)
                val hI = c.getColumnIndex(MediaStore.Images.Media.HEIGHT)
                while (c.moveToNext()) {
                    val id = c.getLong(idI)
                    val uri = ContentUris.withAppendedId(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
                    )
                    list.add(
                        MediaItem(
                            id = id,
                            uriString = uri.toString(),
                            name = c.getString(nameI) ?: "Image",
                            dateMs = c.getLong(dateI) * 1000L,
                            bucket = c.getString(bucketI) ?: "Unknown",
                            isVideo = false,
                            durationMs = 0L,
                            sizeBytes = c.getLong(sizeI),
                            width = if (wI >= 0) c.getInt(wI) else 0,
                            height = if (hI >= 0) c.getInt(hI) else 0
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // ignore
        }
        return list
    }

    private fun queryVideos(): List<MediaItem> {
        val list = ArrayList<MediaItem>()
        val proj = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DATE_ADDED,
            MediaStore.Video.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.WIDTH,
            MediaStore.Video.Media.HEIGHT
        )
        try {
            ctx.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                proj, null, null,
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
                while (c.moveToNext()) {
                    val id = c.getLong(idI)
                    val uri = ContentUris.withAppendedId(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id
                    )
                    list.add(
                        MediaItem(
                            id = id,
                            uriString = uri.toString(),
                            name = c.getString(nameI) ?: "Video",
                            dateMs = c.getLong(dateI) * 1000L,
                            bucket = c.getString(bucketI) ?: "Unknown",
                            isVideo = true,
                            durationMs = c.getLong(durI),
                            sizeBytes = c.getLong(sizeI),
                            width = if (wI >= 0) c.getInt(wI) else 0,
                            height = if (hI >= 0) c.getInt(hI) else 0
                        )
                    )
                }
            }
        } catch (e: Exception) {
            // ignore
        }
        return list
    }
}
