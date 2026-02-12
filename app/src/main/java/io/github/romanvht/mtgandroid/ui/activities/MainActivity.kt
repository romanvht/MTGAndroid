package io.github.romanvht.mtgandroid.ui.activities

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.romanvht.mtgandroid.R
import io.github.romanvht.mtgandroid.data.SENDER
import io.github.romanvht.mtgandroid.data.SERVICE_FAILED_BROADCAST
import io.github.romanvht.mtgandroid.data.SERVICE_STARTED_BROADCAST
import io.github.romanvht.mtgandroid.data.SERVICE_STOPPED_BROADCAST
import io.github.romanvht.mtgandroid.data.Sender
import io.github.romanvht.mtgandroid.databinding.ActivityMainBinding
import io.github.romanvht.mtgandroid.service.MtgProxyService
import io.github.romanvht.mtgandroid.service.ServiceManager
import io.github.romanvht.mtgandroid.utils.*

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var proxyService: MtgProxyService? = null
    private var serviceConnection: ServiceConnection? = null

    companion object {
        private val TAG: String = MainActivity::class.java.simpleName
        private const val NOTIFICATION_PERMISSION_REQUEST_CODE = 100
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            Log.d(TAG, "Received intent: ${intent?.action}")

            if (intent == null) {
                Log.w(TAG, "Received null intent")
                return
            }

            val senderOrd = intent.getIntExtra(SENDER, -1)
            val sender = Sender.entries.getOrNull(senderOrd)
            if (sender == null) {
                Log.w(TAG, "Received intent with unknown sender: $senderOrd")
                return
            }

            when (intent.action) {
                SERVICE_STARTED_BROADCAST -> updateUI()
                SERVICE_STOPPED_BROADCAST -> updateUI()
                SERVICE_FAILED_BROADCAST -> {
                    MaterialAlertDialogBuilder(this@MainActivity)
                        .setTitle(getString(R.string.error))
                        .setMessage(getString(R.string.error_start_failed))
                        .setPositiveButton(getString(R.string.ok), null)
                        .show()
                    updateUI()
                }
                else -> Log.w(TAG, "Unknown action: ${intent.action}")
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        BroadcastUtils.registerServiceReceiver(this, receiver)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }

        loadSettings()
        setupClickListeners()
        bindToService()
    }

    override fun onDestroy() {
        super.onDestroy()
        unbindFromService()
        BroadcastUtils.unregisterReceiver(this, receiver)
    }

    override fun onResume() {
        super.onResume()
        loadSettings()
        updateUI()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                if (proxyService?.isRunning() == true) {
                    Toast.makeText(this, R.string.settings_unavailable, Toast.LENGTH_SHORT).show()
                } else {
                    startActivity(Intent(this, SettingsActivity::class.java))
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadSettings() {
        val ip = PreferencesUtils.getIpAddress(this)
        val port = PreferencesUtils.getPort(this)
        val secret = PreferencesUtils.getSecret(this)

        binding.ipText.text = ip
        binding.portText.text = port
        binding.secretText.text = if (secret.isNotEmpty()) {
            secret.take(16) + if (secret.length > 16) "..." else ""
        } else {
            getString(R.string.error_empty_secret)
        }

        updateTelegramLink(ip, port, secret)
    }

    private fun updateTelegramLink(ip: String, port: String, secret: String) {
        val link = FormatUtils.generateTelegramLink(ip, port, secret)

        if (link.isEmpty()) {
            binding.telegramLinkInput.setText(getString(R.string.error_empty_secret))
        } else {
            binding.telegramLinkInput.setText(link)
        }
    }

    private fun setupClickListeners() {
        binding.connectButton.setOnClickListener {
            if (proxyService?.isRunning() == true) {
                stopProxy()
            } else {
                startProxy()
            }
        }

        binding.telegramLinkInput.isFocusable = false
        binding.telegramLinkInput.isClickable = true

        binding.telegramLinkInput.setOnClickListener {
            copyLinkToClipboard()
        }
    }

    private fun startProxy() {
        ServiceManager.startService(this)

        if (serviceConnection == null) {
            bindToService()
        }
    }

    private fun stopProxy() {
        ServiceManager.stopService(this)
        updateUI()
    }

    private fun bindToService() {
        serviceConnection = ServiceManager.bindService(this, object : ServiceManager.ServiceCallback {
            override fun onServiceConnected(service: MtgProxyService) {
                proxyService = service
                updateUI()
            }

            override fun onServiceDisconnected() {
                proxyService = null
                updateUI()
            }
        })
    }

    private fun unbindFromService() {
        serviceConnection?.let {
            ServiceManager.unbindService(this, it)
            serviceConnection = null
        }
    }

    private fun updateUI() {
        val isRunning = proxyService?.isRunning() ?: false
        updateUIState(isRunning)
    }

    private fun copyLinkToClipboard() {
        val text = binding.telegramLinkInput.text.toString()

        if (text.isNotEmpty() && !text.contains(getString(R.string.error_empty_secret))) {
            ClipboardUtils.copyToClipboard(
                context = this,
                text = text,
                label = "MTG Proxy Link",
                toastMessage = getString(R.string.copied_to_clipboard)
            )
        }
    }

    private fun updateUIState(isRunning: Boolean) {
        if (isRunning) {
            binding.statusText.text = getString(R.string.status_running)
            binding.connectButton.text = getString(R.string.disconnect)
            binding.connectButton.setIconResource(R.drawable.ic_stop)
        } else {
            binding.statusText.text = getString(R.string.status_stopped)
            binding.connectButton.text = getString(R.string.connect)
            binding.connectButton.setIconResource(R.drawable.ic_play)
        }
    }
}
