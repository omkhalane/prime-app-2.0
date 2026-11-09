package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

object FaviconManager {

    private const val TAG = "FaviconManager"

    /**
     * Clean raw website input into a normalized domain.
     * e.g., "https://www.Reddit.com/r/android" -> "reddit.com"
     */
    fun cleanDomain(input: String): String {
        var domain = input.trim().lowercase(Locale.ROOT)
        domain = domain.removePrefix("https://").removePrefix("http://")
        domain = domain.removePrefix("www.")
        domain = domain.substringBefore('/')
        domain = domain.substringBefore('?')
        domain = domain.substringBefore(':')
        return domain.trim()
    }

    /**
     * Validate whether a domain string is a valid web address.
     * Must contain at least one dot, no spaces, valid characters, and a valid TLD (>=2 letters).
     */
    fun isValidDomain(input: String): Boolean {
        val domain = cleanDomain(input)
        if (domain.isBlank() || domain.contains(" ") || domain.startsWith(".") || domain.endsWith(".")) {
            return false
        }
        val domainRegex = Regex("^[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(\\.[a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*\\.[a-zA-Z]{2,}$")
        return domain.matches(domainRegex)
    }

    /**
     * Get high-res Google Favicon API endpoint for domain.
     */
    fun getFaviconUrl(domain: String): String {
        val clean = cleanDomain(domain)
        return "https://www.google.com/s2/favicons?domain=$clean&sz=128"
    }

    /**
     * Get the dedicated local storage File for a domain's cached favicon.
     */
    fun getCachedFaviconFile(context: Context, domain: String): File {
        val clean = cleanDomain(domain).replace(".", "_")
        val dir = File(context.filesDir, "favicons")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return File(dir, "${clean}.png")
    }

    /**
     * Check if a valid local cached favicon exists for this domain.
     */
    fun hasLocalFavicon(context: Context, domain: String): Boolean {
        val file = getCachedFaviconFile(context, domain)
        return file.exists() && file.length() > 0
    }

    /**
     * Download the website favicon from Google Favicon API and write it to local storage.
     * Saved in context.filesDir/favicons/{domain}.png for instant future offline use.
     */
    suspend fun saveFaviconToLocalStorage(context: Context, domain: String): Boolean = withContext(Dispatchers.IO) {
        val clean = cleanDomain(domain)
        if (!isValidDomain(clean)) return@withContext false

        val targetFile = getCachedFaviconFile(context, clean)
        if (targetFile.exists() && targetFile.length() > 0) {
            return@withContext true
        }

        try {
            val faviconUrl = getFaviconUrl(clean)
            val connection = URL(faviconUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = 6000
            connection.readTimeout = 6000
            connection.instanceFollowRedirects = true
            connection.requestMethod = "GET"
            connection.connect()

            if (connection.responseCode in 200..299) {
                connection.inputStream.use { input ->
                    val bitmap = BitmapFactory.decodeStream(input)
                    if (bitmap != null) {
                        FileOutputStream(targetFile).use { out ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                            out.flush()
                        }
                        return@withContext true
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed downloading favicon for $clean: ${e.message}")
        }
        false
    }
}
