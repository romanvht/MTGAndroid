package io.github.romanvht.mtgandroid.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.core.content.ContextCompat
import io.github.romanvht.mtgandroid.data.START_ACTION
import io.github.romanvht.mtgandroid.data.STOP_ACTION

object ServiceManager {

    interface ServiceCallback {
        fun onServiceConnected(service: MtgProxyService)
        fun onServiceDisconnected()
    }

    fun startService(context: Context) {
        val intent = Intent(context, MtgProxyService::class.java)
        intent.action = START_ACTION
        ContextCompat.startForegroundService(context, intent)
    }

    fun stopService(context: Context) {
        val intent = Intent(context, MtgProxyService::class.java)
        intent.action = STOP_ACTION
        ContextCompat.startForegroundService(context, intent)
    }

    fun bindService(context: Context, callback: ServiceCallback): ServiceConnection {
        val connection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                val binder = service as MtgProxyService.LocalBinder
                callback.onServiceConnected(binder.getService())
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                callback.onServiceDisconnected()
            }
        }

        val intent = Intent(context, MtgProxyService::class.java)
        context.bindService(intent, connection, Context.BIND_AUTO_CREATE)

        return connection
    }

    fun unbindService(context: Context, connection: ServiceConnection) {
        try {
            context.unbindService(connection)
        } catch (e: IllegalArgumentException) {
            // Service not registered
        }
    }
}
