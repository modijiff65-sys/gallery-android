package com.example.gallery

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HiddenActivity : AppCompatActivity() {

    private lateinit var repo: MediaRepository
    private lateinit var adapter: MediaGridAdapter
    private lateinit var grid: RecyclerView
    private lateinit var lockedView: View
    private lateinit var emptyView: View
    private lateinit var sub: TextView

    private var unlocked = false
    private var hiddenItems: List<MediaItem> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        ThemeUtil.apply(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_hidden)

        repo = MediaRepository(this)
        grid = findViewById(R.id.grid)
        lockedView = findViewById(R.id.lockedView)
        emptyView = findViewById(R.id.emptyView)
        sub = findViewById(R.id.hiddenSub)

        adapter = MediaGridAdapter(
            onClick = { item ->
                val list = hiddenItems
                val idx = list.indexOfFirst { it.uriString == item.uriString }.coerceAtLeast(0)
                ViewerActivity.start(this, ArrayList(list), idx)
            },
            onLongClick = { item ->
                MaterialAlertDialogBuilder(this)
                    .setTitle(item.name)
                    .setItems(arrayOf("Unhide")) { _, _ -> unhide(item) }
                    .show()
            }
        )
        grid.layoutManager = GridLayoutManager(this, 3)
        grid.adapter = adapter

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<View>(R.id.btnUnlock).setOnClickListener { authenticate() }
        findViewById<View>(R.id.btnLock).setOnClickListener { lock() }

        lock()
        authenticate()
    }

    private fun lock() {
        unlocked = false
        grid.visibility = View.GONE
        emptyView.visibility = View.GONE
        lockedView.visibility = View.VISIBLE
    }

    private fun authenticate() {
        val manager = BiometricManager.from(this)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_WEAK or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        val can = manager.canAuthenticate(authenticators)
        if (can != BiometricManager.BIOMETRIC_SUCCESS) {
            // No biometrics / lock screen set up — open it so the feature is still usable.
            toast("Device lock set nahi hai, folder khul raha hai")
            unlock()
            return
        }
        val prompt = BiometricPrompt(
            this,
            ContextCompat.getMainExecutor(this),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    unlock()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    toast(errString.toString())
                }
            }
        )
        val info = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.hidden))
            .setSubtitle("Dekhne ke liye unlock karein")
            .setAllowedAuthenticators(authenticators)
            .build()
        prompt.authenticate(info)
    }

    private fun unlock() {
        unlocked = true
        lockedView.visibility = View.GONE
        loadItems()
    }

    private fun loadItems() {
        lifecycleScope.launch {
            val all = withContext(Dispatchers.IO) { repo.loadAll() }
            val hidden = Prefs.hidden(this@HiddenActivity)
            hiddenItems = all.filter { hidden.contains(it.uriString) }
            sub.text = "${hiddenItems.size} items"
            adapter.submit(hiddenItems.map { GridItem.Media(it) })
            val isEmpty = hiddenItems.isEmpty()
            grid.visibility = if (isEmpty) View.GONE else View.VISIBLE
            emptyView.visibility = if (isEmpty) View.VISIBLE else View.GONE
        }
    }

    private fun unhide(item: MediaItem) {
        val hidden = Prefs.hidden(this)
        hidden.remove(item.uriString)
        Prefs.setHidden(this, hidden)
        toast("Unhide ho gaya")
        loadItems()
    }

    private fun toast(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}
