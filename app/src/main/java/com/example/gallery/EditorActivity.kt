package com.example.gallery

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Matrix
import android.graphics.Paint
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.exifinterface.media.ExifInterface
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.lifecycle.lifecycleScope

class EditorActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_URI = "uri"
        private const val EXTRA_NAME = "name"

        fun start(ctx: Context, item: MediaItem) {
            val i = Intent(ctx, EditorActivity::class.java)
            i.putExtra(EXTRA_URI, item.uriString)
            i.putExtra(EXTRA_NAME, item.name)
            ctx.startActivity(i)
        }
    }

    private lateinit var preview: ImageView
    private lateinit var filterChips: ChipGroup
    private var original: Bitmap? = null
    private var rotationDeg = 0
    private var flipX = false
    private var filterIndex = 0

    private val filterNames = arrayOf("None", "Mono", "Sepia", "Warm", "Cool", "Vivid")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)

        preview = findViewById(R.id.preview)
        filterChips = findViewById(R.id.filterChips)

        buildFilterChips()

        findViewById<View>(R.id.btnClose).setOnClickListener { finish() }
        findViewById<View>(R.id.btnRotateLeft).setOnClickListener {
            rotationDeg = (rotationDeg + 270) % 360; renderPreview()
        }
        findViewById<View>(R.id.btnRotateRight).setOnClickListener {
            rotationDeg = (rotationDeg + 90) % 360; renderPreview()
        }
        findViewById<View>(R.id.btnFlip).setOnClickListener {
            flipX = !flipX; renderPreview()
        }
        findViewById<View>(R.id.btnReset).setOnClickListener {
            rotationDeg = 0; flipX = false; filterIndex = 0
            if (filterChips.childCount > 0) (filterChips.getChildAt(0) as Chip).isChecked = true
            renderPreview()
        }
        findViewById<View>(R.id.btnSave).setOnClickListener { save() }

        val uri = intent.getStringExtra(EXTRA_URI)?.let { android.net.Uri.parse(it) }
        if (uri == null) { finish(); return }

        lifecycleScope.launch {
            val bmp = withContext(Dispatchers.IO) { loadBitmap(uri) }
            if (bmp == null) {
                toast("Image load nahi ho payi")
                finish()
                return@launch
            }
            original = bmp
            renderPreview()
        }
    }

    private fun buildFilterChips() {
        for (i in filterNames.indices) {
            val chip = Chip(this)
            chip.text = filterNames[i]
            chip.isCheckable = true
            chip.isChecked = i == 0
            chip.chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                if (i == 0) android.graphics.Color.parseColor("#6D5EFC") else android.graphics.Color.parseColor("#33FFFFFF")
            )
            chip.setTextColor(android.graphics.Color.WHITE)
            chip.setOnClickListener {
                filterIndex = i
                for (j in 0 until filterChips.childCount) {
                    val c = filterChips.getChildAt(j) as Chip
                    c.isChecked = j == i
                    c.chipBackgroundColor = android.content.res.ColorStateList.valueOf(
                        if (j == i) android.graphics.Color.parseColor("#6D5EFC")
                        else android.graphics.Color.parseColor("#33FFFFFF")
                    )
                }
                renderPreview()
            }
            filterChips.addView(chip)
        }
    }

    private fun loadBitmap(uri: android.net.Uri): Bitmap? {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (bounds.outWidth / sample > 1600 || bounds.outHeight / sample > 1600) sample *= 2
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bmp = contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            } ?: return null

            val orientation = try {
                contentResolver.openInputStream(uri)?.use { s ->
                    ExifInterface(s).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL
                    )
                } ?: ExifInterface.ORIENTATION_NORMAL
            } catch (e: Exception) {
                ExifInterface.ORIENTATION_NORMAL
            }
            applyOrientation(bmp, orientation)
        } catch (e: Exception) {
            null
        }
    }

    private fun applyOrientation(bmp: Bitmap, orientation: Int): Bitmap {
        val m = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.postScale(1f, -1f)
            else -> return bmp
        }
        return try {
            Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true)
        } catch (e: Exception) {
            bmp
        }
    }

    private fun transformed(): Bitmap? {
        val src = original ?: return null
        val m = Matrix()
        if (rotationDeg != 0) m.postRotate(rotationDeg.toFloat())
        if (flipX) m.postScale(-1f, 1f)
        return if (rotationDeg == 0 && !flipX) src
        else try {
            Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true)
        } catch (e: Exception) {
            src
        }
    }

    private fun filterMatrix(index: Int): ColorMatrix? = when (index) {
        1 -> ColorMatrix().apply { setSaturation(0f) }
        2 -> ColorMatrix(
            floatArrayOf(
                0.393f, 0.769f, 0.189f, 0f, 0f,
                0.349f, 0.686f, 0.168f, 0f, 0f,
                0.272f, 0.534f, 0.131f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        3 -> ColorMatrix(
            floatArrayOf(
                1.15f, 0f, 0f, 0f, 12f,
                0f, 1.05f, 0f, 0f, 4f,
                0f, 0f, 0.9f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        4 -> ColorMatrix(
            floatArrayOf(
                0.9f, 0f, 0f, 0f, 0f,
                0f, 1.0f, 0f, 0f, 4f,
                0f, 0f, 1.2f, 0f, 12f,
                0f, 0f, 0f, 1f, 0f
            )
        )
        5 -> ColorMatrix().apply { setSaturation(1.6f) }
        else -> null
    }

    private fun renderPreview() {
        val bmp = transformed() ?: return
        preview.setImageBitmap(bmp)
        val cm = filterMatrix(filterIndex)
        preview.colorFilter = if (cm != null) ColorMatrixColorFilter(cm) else null
    }

    private fun save() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            toast("Saving ke liye Android 10+ chahiye")
            return
        }
        val bmp = transformed()
        if (bmp == null) {
            toast("Kuch save karne ko nahi")
            return
        }
        try {
            val out = Bitmap.createBitmap(bmp.width, bmp.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(out)
            val paint = Paint(Paint.FILTER_BITMAP_FLAG)
            val cm = filterMatrix(filterIndex)
            if (cm != null) paint.colorFilter = ColorMatrixColorFilter(cm)
            canvas.drawBitmap(bmp, 0f, 0f, paint)

            val name = "GAL_${System.currentTimeMillis()}.jpg"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, name)
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Gallery")
            }
            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
            if (uri == null) {
                toast("Save nahi ho paya")
                return
            }
            contentResolver.openOutputStream(uri)?.use { stream ->
                out.compress(Bitmap.CompressFormat.JPEG, 92, stream)
            }
            toast("Nayi copy save ho gayi")
            finish()
        } catch (e: Exception) {
            toast("Save nahi ho paya")
        }
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
