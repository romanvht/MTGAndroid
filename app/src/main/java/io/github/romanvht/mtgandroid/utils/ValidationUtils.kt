package io.github.romanvht.mtgandroid.utils

import android.util.Patterns
import io.github.romanvht.mtgandroid.data.ValidationResult
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

    private fun isValidSecret(secret: String): Boolean {
        return secret.isNotBlank() && secret.length > 10
    }

    fun validateProxySettings(ip: String, port: String, secret: String): ValidationResult {
        if (!isValidIpAddress(ip)) {
            return ValidationResult(false, "Неверный IP адрес")
        }

        if (!isValidPort(port)) {
            return ValidationResult(false, "Порт должен быть от 1 до 65535")
        }

        if (!isNonPrivilegedPort(port)) {
            return ValidationResult(false, "Порт должен быть больше 1024")
        }

        if (!isValidSecret(secret)) {
            return ValidationResult(false, "Секрет не сгенерирован или некорректен")
        }

        return ValidationResult(true)
    }
}
