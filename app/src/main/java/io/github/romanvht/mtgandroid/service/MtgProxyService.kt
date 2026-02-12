package io.github.romanvht.mtgandroid.service

import android.content.Intent
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import io.github.romanvht.mtgandroid.R
import io.github.romanvht.mtgandroid.data.START_ACTION
import io.github.romanvht.mtgandroid.data.STOP_ACTION
import io.github.romanvht.mtgandroid.utils.BroadcastUtils
import io.github.romanvht.mtgandroid.utils.MtgWrapper
import io.github.romanvht.mtgandroid.utils.PreferencesUtils
import io.github.romanvht.mtgandroid.utils.createServiceNotification
import io.github.romanvht.mtgandroid.utils.registerNotificationChannel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class MtgProxyService : LifecycleService() {

    private val binder = LocalBinder()
    private val mutex = Mutex()

    companion object {
        private const val TAG = "MtgProxyService"
        private const val FOREGROUND_SERVICE_ID = 2
        private const val NOTIFICATION_CHANNEL_ID = "MTG Proxy"

        private enum class ServiceStatus { Connected, Disconnected, Failed }
        private var status: ServiceStatus = ServiceStatus.Disconnected
    }

    inner class LocalBinder : Binder() {
        fun getService(): MtgProxyService = this@MtgProxyService
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onCreate() {
        super.onCreate()
        registerNotificationChannel(
            this,
            NOTIFICATION_CHANNEL_ID,
            R.string.notification_channel_name
        )
        Log.d(TAG, "Service created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        Log.d(TAG, "onStartCommand: action=${intent?.action}, status=$status")

        when (intent?.action) {
            START_ACTION -> lifecycleScope.launch { start() }
            STOP_ACTION  -> lifecycleScope.launch { stop() }
            else -> Log.w(TAG, "Unknown action: ${intent?.action}")
        }

        return START_STICKY
    }

    fun isRunning(): Boolean = status == ServiceStatus.Connected

    private suspend fun start() {
        Log.i(TAG, "Starting")

        if (status == ServiceStatus.Connected) {
            Log.w(TAG, "Proxy already connected")
            return
        }

        try {
            startForegroundService()

            mutex.withLock {
                if (status == ServiceStatus.Connected) return

                val secret = PreferencesUtils.getSecret(this)
                val bindAddress = PreferencesUtils.getBindAddress(this)

                if (secret.isEmpty()) {
                    throw IllegalStateException("Secret is empty")
                }

                val success = withContext(Dispatchers.IO) {
                    MtgWrapper.startProxy(this@MtgProxyService, bindAddress, secret)
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

    private fun startForegroundService() {
        val ip = PreferencesUtils.getIpAddress(this)
        val port = PreferencesUtils.getPortInt(this)

        val notification = createServiceNotification(
            this,
            NOTIFICATION_CHANNEL_ID,
            ip,
            port
        )

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
            }
            updateStatus(ServiceStatus.Disconnected)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
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
    }
}
