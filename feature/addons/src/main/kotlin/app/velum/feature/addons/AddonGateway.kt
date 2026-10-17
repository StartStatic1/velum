package app.velum.feature.addons

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import app.velum.core.model.AddonManifest
import app.velum.core.model.MediaTitle
import app.velum.core.model.PreparedTitles
import app.velum.core.model.addonManifestUrl
import app.velum.core.model.catalogRequestUrl
import app.velum.core.model.isPublicHostName
import app.velum.core.model.metaRequestUrl
import app.velum.core.model.parseAddonDetail
import app.velum.core.model.parseAddonManifest
import app.velum.core.model.parseAddonMetas
import app.velum.core.model.parseAddonStreams
import app.velum.core.model.streamRequestUrl
import app.velum.core.model.validatePlaybackUrl
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URI
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

data class AddonShelf(
    val name: String,
    val titles: List<MediaTitle>,
)

private val Context.addonStore by preferencesDataStore(name = "velum-addons")
private val addonUrlsKey = stringSetPreferencesKey("urls")
private const val maxAddons = 8
private const val maxBytes = 1_000_000

class AddonGateway(context: Context) {
    private val store = context.applicationContext.addonStore

    val urls: Flow<List<String>> = store.data.map { prefs ->
        prefs[addonUrlsKey].orEmpty().sorted()
    }

    suspend fun add(rawUrl: String) {
        val manifestUrl = addonManifestUrl(rawUrl)
        val manifest = withContext(Dispatchers.IO) {
            parseAddonManifest(httpGet(manifestUrl), manifestUrl)
        }
        store.edit { prefs ->
            val current = prefs[addonUrlsKey].orEmpty()
            if (manifest.transportUrl !in current && current.size >= maxAddons) {
                throw IllegalArgumentException("Dá para guardar até $maxAddons addons.")
            }
            prefs[addonUrlsKey] = current + manifest.transportUrl
        }
    }

    suspend fun remove(manifestUrl: String) {
        store.edit { prefs ->
            prefs[addonUrlsKey] = prefs[addonUrlsKey].orEmpty() - manifestUrl
        }
    }

    suspend fun loadShelves(urls: List<String>): List<AddonShelf> = withContext(Dispatchers.IO) {
        urls.mapNotNull { url ->
            try {
                val manifest = parseAddonManifest(httpGet(url), url)
                val titles = manifest.catalogs.take(2).flatMap { catalog ->
                    val body = httpGet(catalogRequestUrl(manifest.transportUrl, catalog.type, catalog.id))
                    parseAddonMetas(body, manifest.transportUrl, catalog.type).take(20)
                }.distinctBy { it.id }
                if (titles.isEmpty()) null else AddonShelf(manifest.name, titles)
            } catch (_: Exception) {
                null
            }
        }
    }

    suspend fun resolve(titleId: String): MediaTitle? {
        val known = PreparedTitles.title(titleId)
        val pointer = PreparedTitles.pointer(titleId) ?: return known
        return withContext(Dispatchers.IO) {
            val detail = try {
                parseAddonDetail(
                    httpGet(metaRequestUrl(pointer.manifestUrl, pointer.type, pointer.itemId)),
                    pointer.manifestUrl,
                    pointer.type,
                )
            } catch (_: Exception) {
                known
            }
            val streams = try {
                parseAddonStreams(httpGet(streamRequestUrl(pointer.manifestUrl, pointer.type, pointer.itemId)))
            } catch (_: Exception) {
                emptyList()
            }
            val base = detail ?: known ?: return@withContext null
            val ready = base.copy(id = titleId, streams = streams)
            PreparedTitles.remember(ready, pointer)
            ready
        }
    }

    suspend fun manifests(urls: List<String>): List<AddonManifest> = withContext(Dispatchers.IO) {
        urls.mapNotNull { url ->
            try {
                parseAddonManifest(httpGet(url), url)
            } catch (_: Exception) {
                AddonManifest(
                    id = url,
                    name = url,
                    version = "",
                    description = "",
                    transportUrl = url,
                    catalogs = emptyList(),
                )
            }
        }
    }
}

internal fun httpGet(url: String): String {
    validatePlaybackUrl(url)
    var current = url
    repeat(4) {
        assertPublicAddress(current)
        val connection = (URL(current).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = false
            connectTimeout = 12_000
            readTimeout = 20_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "Velum/0.2")
        }
        try {
            val code = connection.responseCode
            if (code in 300..399) {
                val next = connection.getHeaderField("Location")
                    ?: throw IllegalArgumentException("O addon redirecionou sem destino.")
                current = if (next.startsWith("https://")) {
                    next
                } else {
                    URI(current).resolve(next).toString()
                }
                validatePlaybackUrl(current)
                return@repeat
            }
            if (code !in 200..299) {
                throw IllegalArgumentException("O addon respondeu $code.")
            }
            val bytes = connection.inputStream.use { input ->
                val buffer = ByteArray(maxBytes + 1)
                var read = 0
                while (read < buffer.size) {
                    val count = input.read(buffer, read, buffer.size - read)
                    if (count < 0) break
                    read += count
                }
                require(read <= maxBytes) { "A resposta do addon é grande demais." }
                buffer.copyOf(read)
            }
            return bytes.toString(Charsets.UTF_8)
        } finally {
            connection.disconnect()
        }
    }
    throw IllegalArgumentException("O addon redirecionou demais.")
}

private fun assertPublicAddress(url: String) {
    val host = URI(url).host?.lowercase()?.removeSuffix(".")
        ?: throw IllegalArgumentException("Servidor ausente.")
    require(isPublicHostName(host)) { "Esse servidor não pode ser usado." }
    val addresses = InetAddress.getAllByName(host)
    require(addresses.isNotEmpty()) { "Não achei o servidor do addon." }
    require(addresses.all { address -> !address.isBlocked() }) { "Esse servidor não pode ser usado." }
}

private fun InetAddress.isBlocked(): Boolean {
    if (isAnyLocalAddress || isLoopbackAddress || isLinkLocalAddress || isSiteLocalAddress || isMulticastAddress) {
        return true
    }
    val bytes = address
    return bytes.size == 16 && (bytes[0].toInt() and 0xFE) == 0xFC
}
