package io.github.romanvht.mtgandroid.service

import android.app.Notification
import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
import android.os.Build
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import io.github.romanvht.mtgandroid.R
import io.github.romanvht.mtgandroid.data.START_ACTION
import io.github.romanvht.mtgandroid.data.STOP_ACTION
import io.github.romanvht.mtgandroid.data.TransportMode
import io.github.romanvht.mtgandroid.utils.BroadcastUtils
import io.github.romanvht.mtgandroid.utils.MtgWrapper
import io.github.romanvht.mtgandroid.utils.PreferencesUtils
import io.github.romanvht.mtgandroid.utils.WsProxyWrapper
import io.github.romanvht.mtgandroid.utils.createServiceNotification
import io.github.romanvht.mtgandroid.utils.registerNotificationChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class MtgProxyService : LifecycleService() {

    private val mutex = Mutex()

    companion object {
        private const val TAG = "MtgProxyService"
        private const val FOREGROUND_SERVICE_ID = 2
        private const val NOTIFICATION_CHANNEL_ID = "MTG Proxy"

        private enum class ServiceStatus { Connected, Disconnected, Failed }
        private var status: ServiceStatus = ServiceStatus.Disconnected

        fun isRunning() = status == ServiceStatus.Connected
    }

    override fun onCreate() {
        super.onCreate()
        registerNotificationChannel(
            this,
            NOTIFICATION_CHANNEL_ID,
            R.string.notification_channel_name
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        startForeground()

        return when (val action = intent?.action) {
            START_ACTION -> {
                lifecycleScope.launch { start() }
                START_STICKY
            }
            STOP_ACTION -> {
                lifecycleScope.launch { stop() }
                START_NOT_STICKY
            }
            else -> {
                Log.w(TAG, "Unknown action: $action")
                START_NOT_STICKY
            }
        }
    }

    private suspend fun start() {
        Log.i(TAG, "Starting")

        if (status == ServiceStatus.Connected) {
            Log.w(TAG, "Proxy already connected")
            return
        }

        try {
            mutex.withLock {
                if (status == ServiceStatus.Connected) {
                    Log.w(TAG, "Proxy already connected")
                    return@withLock
                }

                val secret = PreferencesUtils.getSecret(this)
                val bindAddress = PreferencesUtils.getBindAddress(this)
                val transportMode = TransportMode.fromValue(PreferencesUtils.getTransportMode(this))

                if (secret.isEmpty()) {
                    throw IllegalStateException("Secret is empty")
                }

                val success = withContext(Dispatchers.IO) {
                    when (transportMode) {
                        TransportMode.WebSocket -> WsProxyWrapper.startProxy(this@MtgProxyService, bindAddress, secret)
                        TransportMode.MtgLegacy -> MtgWrapper.startProxy(this@MtgProxyService, bindAddress, secret)
                    }
                }

                if (!success) throw IllegalStateException("Native proxy failed")

                updateStatus(ServiceStatus.Connected)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start proxy", e)
            updateStatus(ServiceStatus.Failed)
            stop()
        }
    }

    private fun startForeground() {
        val notification: Notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                FOREGROUND_SERVICE_ID,
                notification,
                FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(FOREGROUND_SERVICE_ID, notification)
        }
    }

    private suspend fun stop() {
        Log.i(TAG, "Stopping")

        mutex.withLock {
            withContext(Dispatchers.IO) {
                MtgWrapper.stopProxy()
                WsProxyWrapper.stopProxy()
            }
            updateStatus(ServiceStatus.Disconnected)
        }

        stopSelf()
    }

    private fun updateStatus(newStatus: ServiceStatus) {
        Log.d(TAG, "Proxy status changed from $status to $newStatus")

        status = newStatus

        when (newStatus) {
            ServiceStatus.Connected    -> BroadcastUtils.sendServiceStarted(this)
            ServiceStatus.Disconnected -> BroadcastUtils.sendServiceStopped(this)
            ServiceStatus.Failed       -> BroadcastUtils.sendServiceFailed(this)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            QuickTileService.updateTile()
        }
    }

    private fun createNotification(): Notification {
        val ip = PreferencesUtils.getIpAddress(this)
        val port = PreferencesUtils.getPortInt(this)
        return createServiceNotification(this, NOTIFICATION_CHANNEL_ID, ip, port)
    }
}
