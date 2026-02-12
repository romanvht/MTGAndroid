package io.github.romanvht.mtgandroid.utils

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import io.github.romanvht.mtgandroid.data.*

object BroadcastUtils {

    fun sendServiceStarted(context: Context, sender: Sender = Sender.Service) {
        val intent = Intent(SERVICE_STARTED_BROADCAST).apply {
            setPackage(context.packageName)
            putExtra(SENDER, sender.ordinal)
        }
        context.sendBroadcast(intent)
    }

    fun sendServiceStopped(context: Context, sender: Sender = Sender.Service) {
        val intent = Intent(SERVICE_STOPPED_BROADCAST).apply {
            setPackage(context.packageName)
            putExtra(SENDER, sender.ordinal)
        }
        context.sendBroadcast(intent)
    }

    fun sendServiceFailed(context: Context, sender: Sender = Sender.Service) {
        val intent = Intent(SERVICE_FAILED_BROADCAST).apply {
            setPackage(context.packageName)
            putExtra(SENDER, sender.ordinal)
        }
        context.sendBroadcast(intent)
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    fun registerServiceReceiver(context: Context, receiver: BroadcastReceiver) {
        val intentFilter = IntentFilter().apply {
            addAction(SERVICE_STARTED_BROADCAST)
            addAction(SERVICE_STOPPED_BROADCAST)
            addAction(SERVICE_FAILED_BROADCAST)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, intentFilter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, intentFilter)
        }
    }

    fun unregisterReceiver(context: Context, receiver: BroadcastReceiver?) {
        try {
            receiver?.let { context.unregisterReceiver(it) }
        } catch (e: IllegalArgumentException) {
            // Receiver was not registered or already unregistered
        }
    }
}
