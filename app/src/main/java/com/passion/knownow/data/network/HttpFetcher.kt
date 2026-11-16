package com.passion.knownow.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.CacheControl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException

/** Non-2xx response. Separate type so callers can tell "page is gone" from "network broke". */
class HttpStatusException(val code: Int, val path: String) : IOException("HTTP $code for $path")

class HttpFetcher(private val client: OkHttpClient) {

    suspend fun get(url: String, forceRefresh: Boolean = false): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .apply {
                if (forceRefresh) cacheControl(CacheControl.Builder().noCache().build())
            }
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw HttpStatusException(response.code, request.url.encodedPath)
            }
            response.body?.string() ?: ""
        }
    }

    /**
     * Streams [url] into [target]. Writes to a sibling `.part` file first so an interrupted
     * download never leaves a truncated document that would later be parsed as valid.
     * Returns the number of bytes written.
     */
    suspend fun downloadToFile(
        url: String,
        target: File,
        forceRefresh: Boolean = false,
        onBytes: (Long) -> Unit = {},
    ): Long = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .apply {
                if (forceRefresh) cacheControl(CacheControl.Builder().noCache().build())
            }
            .build()
        target.parentFile?.mkdirs()
        val part = File(target.parentFile, "${target.name}.part")
        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw HttpStatusException(response.code, request.url.encodedPath)
                }
                val body = response.body
                    ?: throw IllegalStateException("Empty body for ${request.url.encodedPath}")
                var written = 0L
                part.outputStream().use { out ->
                    body.byteStream().use { input ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val read = input.read(buffer)
                            if (read == -1) break
                            out.write(buffer, 0, read)
                            written += read
                            onBytes(written)
                        }
                    }
                }
                if (target.exists()) target.delete()
                if (!part.renameTo(target)) {
                    part.copyTo(target, overwrite = true)
                    part.delete()
                }
                written
            }
        } catch (e: Throwable) {
            // Never leave a truncated .part behind to be mistaken for a good file later.
            part.delete()
            throw e
        }
    }
}
