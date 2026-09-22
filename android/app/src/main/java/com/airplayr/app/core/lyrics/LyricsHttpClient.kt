package com.airplayr.app.core.lyrics

import android.util.Log
import com.airplayr.app.BuildConfig
import org.json.JSONArray
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

/** Small, dependency-free HTTP layer shared by lyrics providers. */
class LyricsHttpClient {
    companion object { const val TAG = "LyricsHttp" }

    fun getText(url: String, headers: Map<String, String> = emptyMap()): String? {
        val connection = open(url, headers) ?: return null
        return try {
            connection.requestMethod = "GET"
            read(connection)
        } finally { connection.disconnect() }
    }

    fun getJson(url: String, headers: Map<String, String> = emptyMap()): JSONObject? =
        getText(url, headers)?.let { runCatching { JSONObject(it) }.onFailure { e -> debug("JSON object parse failed for ${base(url)}: ${e.javaClass.simpleName}: ${e.message.orEmpty()}") }.getOrNull() }

    fun getJsonArray(url: String, headers: Map<String, String> = emptyMap()): JSONArray? =
        getText(url, headers)?.let { runCatching { JSONArray(it) }.onFailure { e -> debug("JSON array parse failed for ${base(url)}: ${e.javaClass.simpleName}: ${e.message.orEmpty()}") }.getOrNull() }

    fun postJson(url: String, body: String, headers: Map<String, String> = emptyMap()): JSONObject? {
        val connection = open(url, headers) ?: return null
        return try {
            connection.requestMethod = "POST"
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { it.write(body) }
            read(connection)?.let { runCatching { JSONObject(it) }.getOrNull() }
        } catch (e: Exception) {
            debug("POST failed for ${base(url)}: ${e.javaClass.simpleName}: ${e.message.orEmpty()}")
            null
        } finally { connection.disconnect() }
    }

    fun params(values: Map<String, String>): String = values
        .filterValues { it.isNotBlank() }
        .entries.joinToString("&") { "${enc(it.key)}=${enc(it.value)}" }

    fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun open(url: String, headers: Map<String, String>): HttpURLConnection? = runCatching {
        (URL(url).openConnection() as HttpURLConnection).apply {
            connectTimeout = 5_000
            readTimeout = 7_000
            instanceFollowRedirects = true
            useCaches = true
            setRequestProperty("User-Agent", "AirPlayr/1.0 (lyrics)")
            setRequestProperty("Accept", "application/json,text/plain,application/xml,*/*")
            setRequestProperty("Accept-Language", "en-US,en;q=0.8")
            headers.forEach { (key, value) -> setRequestProperty(key, value) }
        }
    }.onFailure { e -> debug("connection open failed for ${base(url)}: ${e.javaClass.simpleName}: ${e.message.orEmpty()}") }.getOrNull()

    private fun read(connection: HttpURLConnection): String? {
        return try {
            val status = connection.responseCode
            debug("HTTP $status ${connection.requestMethod} ${base(connection.url.toString())}")
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            if (stream == null) return null
            stream.bufferedReader(Charsets.UTF_8).use { it.readText() }.takeIf { it.isNotBlank() && status in 200..299 }
        } catch (e: Exception) {
            debug("HTTP request failed: ${e.javaClass.simpleName}: ${e.message.orEmpty()}")
            null
        }
    }

    private fun base(url: String): String = url.substringBefore('?')
    private fun debug(message: String) { if (BuildConfig.DEBUG) Log.d(TAG, message) }
}
