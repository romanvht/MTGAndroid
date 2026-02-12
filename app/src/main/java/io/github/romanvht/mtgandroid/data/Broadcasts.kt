package io.github.romanvht.mtgandroid.data

import io.github.romanvht.mtgandroid.BuildConfig

const val SERVICE_STARTED_BROADCAST = "${BuildConfig.APPLICATION_ID}.SERVICE_STARTED"
const val SERVICE_STOPPED_BROADCAST = "${BuildConfig.APPLICATION_ID}.SERVICE_STOPPED"
const val SERVICE_FAILED_BROADCAST = "${BuildConfig.APPLICATION_ID}.SERVICE_FAILED"

const val SENDER = "sender"

enum class Sender(val senderName: String) {
    Service("Service")
}
