package io.github.romanvht.mtgandroid.data

enum class TransportMode(val value: String) {
    WebSocket("websocket"),
    MtgLegacy("mtg");

    companion object {
        fun fromValue(value: String): TransportMode {
            return entries.firstOrNull { it.value == value } ?: WebSocket
        }
    }
}
