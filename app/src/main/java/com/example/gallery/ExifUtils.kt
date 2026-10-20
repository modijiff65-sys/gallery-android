package com.example.gallery

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface

object ExifUtils {

    /** Returns label -> value pairs for the info sheet. Never throws. */
    fun read(ctx: Context, uri: Uri): List<Pair<String, String>> {
        val out = ArrayList<Pair<String, String>>()
        try {
            ctx.contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)

                exif.getAttribute(ExifInterface.TAG_MAKE)?.let { out.add("Camera" to it) }
                exif.getAttribute(ExifInterface.TAG_MODEL)?.let { out.add("Model" to it) }

                val iso = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)
                    ?: exif.getAttribute(ExifInterface.TAG_ISO_SPEED_RATINGS)
                iso?.let { out.add("ISO" to it) }

                val exposure = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)
                exposure?.let { out.add("Shutter" to formatExposure(it)) }

                val aperture = exif.getAttribute(ExifInterface.TAG_F_NUMBER)
                aperture?.let { out.add("Aperture" to "f/$it") }

                val focal = exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)
                focal?.let { out.add("Focal length" to formatFocal(it)) }

                val w = exif.getAttribute(ExifInterface.TAG_IMAGE_WIDTH)
                val h = exif.getAttribute(ExifInterface.TAG_IMAGE_LENGTH)
                if (w != null && h != null) out.add("EXIF size" to "$w × $h")
            }
        } catch (e: Exception) {
            // ignore — some files have no readable EXIF
        }
        return out
    }

    private fun formatExposure(raw: String): String {
        // ExifInterface returns something like "1/250" or "0.008"
        return try {
            if (raw.contains("/")) {
                val parts = raw.split("/")
                val v = parts[0].toDouble() / parts[1].toDouble()
                if (v in 0.0001..1.0) "1/" + (1 / v).toInt() + " s" else raw + " s"
            } else {
                val v = raw.toDouble()
                if (v in 0.0001..1.0) "1/" + (1 / v).toInt() + " s" else raw + " s"
            }
        } catch (e: Exception) {
            raw
        }
    }

    private fun formatFocal(raw: String): String {
        return try {
            if (raw.contains("/")) {
                val parts = raw.split("/")
                val v = parts[0].toDouble() / parts[1].toDouble()
                String.format(java.util.Locale.US, "%.1f mm", v)
            } else {
                String.format(java.util.Locale.US, "%.1f mm", raw.toDouble())
            }
        } catch (e: Exception) {
            raw
        }
    }
}
