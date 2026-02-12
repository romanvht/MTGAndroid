package io.github.romanvht.mtgandroid.utils

object FormatUtils {

    fun generateTelegramLink(ip: String, port: String, secret: String): String {
        return if (secret.isNotBlank()) {
            "tg://proxy?server=$ip&port=$port&secret=$secret"
        } else {
            ""
        }
    }

    fun cleanDomain(domain: String): String {
        return domain
            .removePrefix("http://")
            .removePrefix("https://")
            .removePrefix("www.")
            .split("/")[0]
            .trim()
    }

    fun normalizeIpAddress(ip: String): String {
        return ip.trim()
    }

    fun normalizePort(port: String): String {
        return port.trim().filter { it.isDigit() }
    }
}
