package io.github.romanvht.mtgandroid.utils

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.core.content.edit

object PreferencesUtils {

    private const val KEY_DOMAIN = "domain"
    private const val KEY_IP = "ip_address"
    private const val KEY_PORT = "port"
    private const val KEY_SECRET = "secret"

    private const val KEY_CONCURRENCY = "concurrency"
    private const val KEY_TCP_BUFFER = "tcp_buffer"
    private const val KEY_DOH_IP = "doh_ip"
    private const val KEY_TIMEOUT = "timeout"
    private const val KEY_ANTIREPLAY_CACHE = "antireplay_cache"

    private const val DEFAULT_DOMAIN = "www.google.com"
    private const val DEFAULT_IP = "127.0.0.1"
    private const val DEFAULT_PORT = "3128"
    private const val DEFAULT_SECRET = ""

    private const val DEFAULT_CONCURRENCY = 8192
    private const val DEFAULT_TCP_BUFFER_KB = 4
    private const val DEFAULT_DOH_IP = "1.1.1.1"
    private const val DEFAULT_TIMEOUT = 10
    private const val DEFAULT_ANTIREPLAY_CACHE_MB = 1

    fun getDomain(context: Context): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString(KEY_DOMAIN, DEFAULT_DOMAIN) ?: DEFAULT_DOMAIN
    }

    fun setDomain(context: Context, domain: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit { putString(KEY_DOMAIN, domain) }
    }

    fun getIpAddress(context: Context): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString(KEY_IP, DEFAULT_IP) ?: DEFAULT_IP
    }

    fun setIpAddress(context: Context, ip: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit { putString(KEY_IP, ip) }
    }

    fun getPort(context: Context): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString(KEY_PORT, DEFAULT_PORT) ?: DEFAULT_PORT
    }

    fun getPortInt(context: Context): Int {
        return getPort(context).toIntOrNull() ?: DEFAULT_PORT.toInt()
    }

    fun setPort(context: Context, port: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit { putString(KEY_PORT, port) }
    }

    fun getSecret(context: Context): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString(KEY_SECRET, DEFAULT_SECRET) ?: DEFAULT_SECRET
    }

    fun setSecret(context: Context, secret: String) {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        prefs.edit { putString(KEY_SECRET, secret) }
    }

    fun getBindAddress(context: Context): String {
        return "${getIpAddress(context)}:${getPort(context)}"
    }

    fun getConcurrency(context: Context): Int {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString(KEY_CONCURRENCY, DEFAULT_CONCURRENCY.toString())?.toIntOrNull()?: DEFAULT_CONCURRENCY
    }

    fun getDohIp(context: Context): String {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString(KEY_DOH_IP, DEFAULT_DOH_IP) ?: DEFAULT_DOH_IP
    }

    fun getTimeout(context: Context): Int {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString(KEY_TIMEOUT, DEFAULT_TIMEOUT.toString())?.toIntOrNull() ?: DEFAULT_TIMEOUT
    }

    fun getAntiReplayCache(context: Context): Int {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        return prefs.getString(KEY_ANTIREPLAY_CACHE, DEFAULT_ANTIREPLAY_CACHE_MB.toString())?.toIntOrNull()?: DEFAULT_ANTIREPLAY_CACHE_MB
    }
}