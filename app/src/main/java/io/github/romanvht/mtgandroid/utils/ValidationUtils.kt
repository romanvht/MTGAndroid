package io.github.romanvht.mtgandroid.utils

import android.util.Patterns
import java.net.InetAddress

object ValidationUtils {

    fun isValidIpAddress(ip: String): Boolean {
        if (ip.isEmpty()) return false

        return try {
            InetAddress.getByName(ip)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun isValidPort(port: String): Boolean {
        val portNum = port.toIntOrNull() ?: return false
        return portNum in 1..65535
    }

    fun isNonPrivilegedPort(port: String): Boolean {
        val portNum = port.toIntOrNull() ?: return false
        return portNum in 1024..65535
    }

    fun isValidDomain(domain: String): Boolean {
        if (domain.isEmpty()) return false

        return Patterns.DOMAIN_NAME.matcher(domain).matches() || Patterns.WEB_URL.matcher("http://$domain").matches()
    }

    fun isValidConcurrency(concurrency: String): Boolean {
        val value = concurrency.toIntOrNull() ?: return false
        return value in 1..65535
    }

    fun isValidTimeout(timeout: String): Boolean {
        val value = timeout.toIntOrNull() ?: return false
        return value in 1..30
    }

    fun isValidAntiReplayCache(cache: String): Boolean {
        val value = cache.toIntOrNull() ?: return false
        return value in 1..10
    }

}