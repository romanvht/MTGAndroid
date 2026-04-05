package io.github.romanvht.mtgandroid.utils

import android.content.Context
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.Collections
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

/**
 * Experimental local MTProto-over-WebSocket proxy prototype.
 *
 * This implementation accepts local TCP connections from Telegram and tunnels
 * raw bytes through WebSocket endpoint selected by DC id extracted from the
 * client's initial packet.
 */
object WsProxyWrapper {
    private const val TAG = "WsProxyWrapper"

    private var serverSocket: ServerSocket? = null
    private var acceptThread: Thread? = null
    private val clientThreads = Collections.synchronizedList(mutableListOf<Thread>())

    @Volatile
    private var isRunning = false

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .retryOnConnectionFailure(true)
        .build()

    fun startProxy(context: Context, bindAddress: String, secret: String): Boolean {
        if (secret.isBlank()) {
            Log.e(TAG, "Secret is empty")
            return false
        }

        return try {
            stopProxy()

            val (host, port) = parseBindAddress(bindAddress)
            val server = ServerSocket().apply {
                reuseAddress = true
                bind(InetSocketAddress(host, port))
            }

            serverSocket = server
            isRunning = true

            acceptThread = thread(name = "ws-proxy-accept", isDaemon = true) {
                acceptLoop(context, secret)
            }

            Log.i(TAG, "WS proxy prototype listening on $host:$port")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start WS proxy", e)
            stopProxy()
            false
        }
    }

    fun stopProxy() {
        isRunning = false

        try {
            serverSocket?.close()
        } catch (e: Exception) {
            Log.w(TAG, "Error closing server socket", e)
        }

        serverSocket = null
        acceptThread = null

        synchronized(clientThreads) {
            clientThreads.forEach { it.interrupt() }
            clientThreads.clear()
        }
    }

    private fun acceptLoop(context: Context, secret: String) {
        while (isRunning) {
            try {
                val client = serverSocket?.accept() ?: break
                val handler = thread(name = "ws-proxy-client", isDaemon = true) {
                    handleClient(context, client, secret)
                }
                clientThreads.add(handler)
            } catch (e: IOException) {
                if (isRunning) {
                    Log.e(TAG, "Accept loop error", e)
                }
                break
            }
        }
    }

    private fun handleClient(context: Context, client: Socket, secret: String) {
        client.soTimeout = 20000

        val input = BufferedInputStream(client.getInputStream())
        val output = BufferedOutputStream(client.getOutputStream())

        var ws: WebSocket? = null

        try {
            val firstPacket = readFirstPacket(input)
            val dcId = extractDcId(firstPacket)
            val wsUrl = resolveWsUrl(context, dcId)

            Log.d(TAG, "Client connected. DC=$dcId URL=$wsUrl")

            val relay = SocketRelay(client, output)
            val request = Request.Builder()
                .url(wsUrl)
                .header("Origin", "https://web.telegram.org")
                .build()

            ws = httpClient.newWebSocket(request, relay)

            // Send first packet and continue forwarding client stream as WS binary frames.
            ws.send(firstPacket.toByteString())

            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (isRunning && !client.isClosed && !Thread.currentThread().isInterrupted) {
                val read = input.read(buffer)
                if (read <= 0) break
                if (!ws.send(buffer.toByteString(0, read))) break
            }
        } catch (e: Exception) {
            Log.w(TAG, "Client relay error", e)
        } finally {
            try {
                ws?.close(1000, "done")
            } catch (_: Exception) {
            }
            try {
                client.close()
            } catch (_: Exception) {
            }
        }
    }

    private fun resolveWsUrl(context: Context, dcId: Int): String {
        val template = PreferencesUtils.getWsTemplate(context).trim()
        if (template.contains("%d")) {
            return template.format(dcId.coerceIn(1, 5))
        }

        return template
    }

    private fun readFirstPacket(input: BufferedInputStream): ByteArray {
        val first = ByteArray(64)
        var offset = 0
        while (offset < first.size) {
            val read = input.read(first, offset, first.size - offset)
            if (read <= 0) throw IOException("Unexpected EOF while reading first packet")
            offset += read
        }
        return first
    }

    private fun extractDcId(firstPacket: ByteArray): Int {
        // MTProto proxy handshake carries DC id in little-endian int in the tail area.
        // Use offset 56 as prototype heuristic.
        return try {
            val raw = ByteBuffer.wrap(firstPacket, 56, 4)
                .order(ByteOrder.LITTLE_ENDIAN)
                .int

            when {
                raw in 1..5 -> raw
                raw in -5..-1 -> -raw
                else -> 2
            }
        } catch (e: Exception) {
            Log.w(TAG, "Cannot extract DC id, fallback to 2", e)
            2
        }
    }

    private fun parseBindAddress(bindAddress: String): Pair<String, Int> {
        val idx = bindAddress.lastIndexOf(':')
        require(idx > 0 && idx < bindAddress.length - 1) { "Invalid bind address: $bindAddress" }

        val host = bindAddress.substring(0, idx)
        val port = bindAddress.substring(idx + 1).toInt()

        return host to port
    }

    private class SocketRelay(
        private val clientSocket: Socket,
        private val output: BufferedOutputStream
    ) : WebSocketListener() {

        override fun onOpen(webSocket: WebSocket, response: Response) {
            Log.d(TAG, "WS opened: ${response.code}")
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            try {
                output.write(bytes.toByteArray())
                output.flush()
            } catch (e: Exception) {
                Log.e(TAG, "Error writing WS->TCP", e)
                closeBoth(webSocket)
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            closeBoth(webSocket)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            closeBoth(webSocket)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            Log.e(TAG, "WS failure", t)
            closeBoth(webSocket)
        }

        private fun closeBoth(webSocket: WebSocket) {
            try {
                webSocket.cancel()
            } catch (_: Exception) {
            }
            try {
                clientSocket.close()
            } catch (_: Exception) {
            }
        }
    }
}
