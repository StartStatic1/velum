package app.velum.core.model

import java.net.URI
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

data class AddonCatalogRef(
    val type: String,
    val id: String,
    val name: String,
)

data class AddonManifest(
    val id: String,
    val name: String,
    val version: String,
    val description: String,
    val transportUrl: String,
    val catalogs: List<AddonCatalogRef>,
)

data class AddonPointer(
    val manifestUrl: String,
    val type: String,
    val itemId: String,
)

object PreparedTitles {
    private val titles = ConcurrentHashMap<String, MediaTitle>()
    private val pointers = ConcurrentHashMap<String, AddonPointer>()

    fun remember(title: MediaTitle, pointer: AddonPointer? = null) {
        titles[title.id] = title
        if (pointer != null) pointers[title.id] = pointer
    }

    fun title(id: String): MediaTitle? = DemoCatalog.find(id) ?: titles[id]

    fun pointer(id: String): AddonPointer? = pointers[id]
}

fun addonTitleId(manifestUrl: String, type: String, itemId: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
        .digest("$manifestUrl\n$type\n$itemId".toByteArray())
    val hex = digest.take(12).joinToString("") { "%02x".format(it) }
    return "addon-$hex"
}

fun addonManifestUrl(input: String): String {
    var raw = input.trim()
    require(raw.isNotEmpty()) { "Cole o endereço do addon." }
    if (raw.startsWith("stremio://", ignoreCase = true)) {
        raw = "https://" + raw.substring("stremio://".length)
    }
    val uri = try {
        URI(raw)
    } catch (_: Exception) {
        throw IllegalArgumentException("Endereço do addon inválido.")
    }
    require(uri.scheme == "https") { "O addon precisa usar HTTPS." }
    require(uri.userInfo == null) { "O endereço do addon não pode levar usuário ou senha." }
    val host = uri.host?.lowercase()?.removeSuffix(".")
    require(!host.isNullOrBlank() && isPublicHostName(host)) { "Esse servidor não pode ser usado." }
    var path = uri.path ?: ""
    if (path.isEmpty() || path == "/") {
        path = "/manifest.json"
    } else if (!path.endsWith("manifest.json")) {
        path = if (path.endsWith("/")) "${path}manifest.json" else "$path/manifest.json"
    }
    val query = if (uri.rawQuery.isNullOrEmpty()) "" else "?${uri.rawQuery}"
    val port = if (uri.port != -1 && uri.port != 443) ":${uri.port}" else ""
    return "https://$host$port$path$query"
}

fun addonBaseUrl(manifestUrl: String): String {
    val marker = "/manifest.json"
    val cut = manifestUrl.indexOf(marker)
    require(cut > "https://".length) { "Endereço de addon sem manifesto." }
    return manifestUrl.substring(0, cut)
}

fun catalogRequestUrl(manifestUrl: String, type: String, catalogId: String): String {
    require(type.noneUnsafe() && catalogId.noneUnsafe()) { "Catálogo do addon inválido." }
    return "${addonBaseUrl(manifestUrl)}/catalog/$type/$catalogId.json"
}

fun metaRequestUrl(manifestUrl: String, type: String, itemId: String): String {
    require(type.noneUnsafe() && itemId.noneUnsafe()) { "Item do addon inválido." }
    return "${addonBaseUrl(manifestUrl)}/meta/$type/${encodePath(itemId)}.json"
}

fun streamRequestUrl(manifestUrl: String, type: String, itemId: String): String {
    require(type.noneUnsafe() && itemId.noneUnsafe()) { "Item do addon inválido." }
    return "${addonBaseUrl(manifestUrl)}/stream/$type/${encodePath(itemId)}.json"
}

fun parseAddonManifest(json: String, transportUrl: String): AddonManifest {
    val root = Json.parse(json).asObject() ?: throw IllegalArgumentException("O manifesto não é um objeto.")
    val id = root.string("id")?.trim().orEmpty()
    val name = root.string("name")?.trim().orEmpty()
    require(id.isNotEmpty() && name.isNotEmpty()) { "O manifesto não tem id ou nome." }
    val catalogs = root.array("catalogs").mapNotNull { entry ->
        val catalog = entry.asObject() ?: return@mapNotNull null
        val type = catalog.string("type")?.trim().orEmpty()
        val catalogId = catalog.string("id")?.trim().orEmpty()
        if (type.isEmpty() || catalogId.isEmpty() || !type.noneUnsafe() || !catalogId.noneUnsafe()) {
            null
        } else {
            AddonCatalogRef(
                type = type,
                id = catalogId,
                name = catalog.string("name")?.trim().orEmpty().ifEmpty { catalogId },
            )
        }
    }
    require(catalogs.isNotEmpty()) { "Esse addon não publica catálogo." }
    return AddonManifest(
        id = id,
        name = name,
        version = root.string("version")?.trim().orEmpty(),
        description = root.string("description")?.trim().orEmpty(),
        transportUrl = transportUrl,
        catalogs = catalogs,
    )
}

fun parseAddonMetas(json: String, manifestUrl: String, typeOverride: String? = null): List<MediaTitle> {
    val metas = Json.parse(json).asObject()?.array("metas").orEmpty()
    return metas.mapNotNull { entry ->
        val meta = entry.asObject() ?: return@mapNotNull null
        titleFromMeta(meta, manifestUrl, typeOverride)
    }
}

fun parseAddonDetail(json: String, manifestUrl: String, typeOverride: String): MediaTitle? {
    val meta = Json.parse(json).asObject()?.value("meta")?.asObject() ?: return null
    return titleFromMeta(meta, manifestUrl, typeOverride)
}

fun parseAddonStreams(json: String): List<StreamSource> {
    val streams = Json.parse(json).asObject()?.array("streams").orEmpty()
    return streams.mapIndexedNotNull { index, entry ->
        val stream = entry.asObject() ?: return@mapIndexedNotNull null
        val url = stream.string("url")?.trim().orEmpty()
        if (url.isEmpty()) return@mapIndexedNotNull null
        try {
            validatePlaybackUrl(url)
        } catch (_: IllegalArgumentException) {
            return@mapIndexedNotNull null
        }
        val label = stream.string("title")?.trim().orEmpty()
            .ifEmpty { stream.string("name")?.trim().orEmpty() }
            .ifEmpty { "Fonte ${index + 1}" }
        StreamSource(
            id = "src-$index",
            label = label.take(80),
            url = url,
            bytes = 0L,
        )
    }
}

fun validatePlaybackUrl(url: String) {
    val uri = try {
        URI(url)
    } catch (_: Exception) {
        throw IllegalArgumentException("Endereço de vídeo inválido.")
    }
    require(uri.scheme == "https") { "A fonte precisa usar HTTPS." }
    require(uri.userInfo == null) { "A fonte não pode levar usuário ou senha." }
    val host = uri.host?.lowercase()?.removeSuffix(".")
    require(!host.isNullOrBlank() && isPublicHostName(host)) { "Servidor de vídeo não autorizado." }
}

fun isPublicHostName(host: String): Boolean {
    val name = host.trim().lowercase().removeSuffix(".")
    if (name.isEmpty()) return false
    if (name == "localhost" || name.endsWith(".localhost") || name.endsWith(".local")) return false
    if (name == "metadata.google.internal" || name == "metadata.google.com") return false
    if (name.contains(':') || name.contains('%')) return false
    val ipv4 = parseIpv4(name) ?: return true
    return !isPrivateIpv4(ipv4)
}

private fun titleFromMeta(
    meta: JsonObject,
    manifestUrl: String,
    typeOverride: String?,
): MediaTitle? {
    val itemId = meta.string("id")?.trim().orEmpty()
    val type = typeOverride?.trim().orEmpty().ifEmpty { meta.string("type")?.trim().orEmpty() }.ifEmpty { "movie" }
    val name = meta.string("name")?.trim().orEmpty()
    if (itemId.isEmpty() || name.isEmpty() || !itemId.noneUnsafe() || !type.noneUnsafe()) return null
    val title = mediaFromMeta(meta, manifestUrl, type, itemId, name)
    PreparedTitles.remember(title, AddonPointer(manifestUrl, type, itemId))
    return title
}

private fun mediaFromMeta(
    meta: JsonObject,
    manifestUrl: String,
    type: String,
    itemId: String,
    name: String,
): MediaTitle {
    val year = meta.string("year")?.take(4)?.toIntOrNull()
        ?: meta.string("releaseInfo")?.take(4)?.toIntOrNull()
        ?: 0
    return MediaTitle(
        id = addonTitleId(manifestUrl, type, itemId),
        name = name,
        year = year,
        kindLabel = if (type == "series") "Série" else "Filme",
        synopsis = meta.string("description")?.trim().orEmpty().ifEmpty { "Sem sinopse no addon." },
        license = "Addon",
        credit = meta.string("releaseInfo")?.trim().orEmpty(),
        streams = emptyList(),
    )
}

private fun encodePath(value: String): String =
    value.map { char ->
        if (char.isLetterOrDigit() || char == '-' || char == '_' || char == '.') {
            char.toString()
        } else {
            char.code.toString(16).uppercase().padStart(2, '0').let { "%$it" }
        }
    }.joinToString("")

private fun String.noneUnsafe(): Boolean =
    isNotEmpty() && length <= 180 && none { it == '/' || it == '\\' || it == '?' || it == '#' || it.isISOControl() }

private fun parseIpv4(host: String): IntArray? {
    val parts = host.split('.')
    if (parts.size != 4) return null
    val numbers = parts.map { part -> part.toIntOrNull() ?: return null }
    if (numbers.any { it !in 0..255 }) return null
    return numbers.toIntArray()
}

private fun isPrivateIpv4(parts: IntArray): Boolean {
    val a = parts[0]
    val b = parts[1]
    if (a == 10 || a == 127 || a == 0) return true
    if (a == 169 && b == 254) return true
    if (a == 192 && b == 168) return true
    if (a == 172 && b in 16..31) return true
    if (a >= 224) return true
    return false
}

internal class Json internal constructor(private val value: Any?) {
    fun asObject(): JsonObject? = value as? JsonObject

    fun asArray(): List<Json>? {
        val items = value as? List<*> ?: return null
        return items.map { it as Json }
    }

    fun asString(): String? = when (value) {
        is String -> value
        is Long -> value.toString()
        is Double -> {
            if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
        }
        else -> null
    }

    companion object {
        fun parse(text: String): Json {
            val parser = Parser(text)
            val value = parser.parseValue()
            parser.skip()
            require(parser.done()) { "JSON incompleto." }
            return value
        }
    }
}

internal class JsonObject(private val map: Map<String, Json>) {
    fun string(name: String): String? = map[name]?.asString()

    fun value(name: String): Json? = map[name]

    fun array(name: String): List<Json> = map[name]?.asArray().orEmpty()
}

private class Parser(private val text: String) {
    private var index = 0

    fun done(): Boolean = index >= text.length

    fun skip() {
        while (index < text.length && text[index].isWhitespace()) index++
    }

    fun parseValue(): Json {
        skip()
        require(index < text.length) { "JSON vazio." }
        return when (val char = text[index]) {
            '{' -> parseObject()
            '[' -> parseArray()
            '"' -> Json(parseString())
            't' -> literal("true", true)
            'f' -> literal("false", false)
            'n' -> literal("null", null)
            else -> {
                require(char == '-' || char.isDigit()) { "JSON inválido." }
                Json(parseNumber())
            }
        }
    }

    private fun literal(token: String, value: Any?): Json {
        require(text.startsWith(token, index)) { "JSON inválido." }
        index += token.length
        return Json(value)
    }

    private fun parseObject(): Json {
        index++
        val map = linkedMapOf<String, Json>()
        skip()
        if (consume('}')) return Json(JsonObject(map))
        while (true) {
            skip()
            require(peek('"')) { "Chave JSON inválida." }
            val key = parseString()
            skip()
            require(consume(':')) { "JSON inválido." }
            map[key] = parseValue()
            skip()
            when {
                consume(',') -> Unit
                consume('}') -> return Json(JsonObject(map))
                else -> throw IllegalArgumentException("JSON inválido.")
            }
        }
    }

    private fun parseArray(): Json {
        index++
        val items = mutableListOf<Json>()
        skip()
        if (consume(']')) return Json(items)
        while (true) {
            items += parseValue()
            skip()
            when {
                consume(',') -> Unit
                consume(']') -> return Json(items)
                else -> throw IllegalArgumentException("JSON inválido.")
            }
        }
    }

    private fun parseString(): String {
        index++
        val out = StringBuilder()
        while (index < text.length) {
            val char = text[index++]
            when (char) {
                '"' -> return out.toString()
                '\\' -> {
                    require(index < text.length) { "Texto JSON cortado." }
                    when (val escaped = text[index++]) {
                        '"', '\\', '/' -> out.append(escaped)
                        'b' -> out.append('\b')
                        'f' -> out.append('\u000C')
                        'n' -> out.append('\n')
                        'r' -> out.append('\r')
                        't' -> out.append('\t')
                        'u' -> {
                            require(index + 4 <= text.length) { "Texto JSON cortado." }
                            val hex = text.substring(index, index + 4)
                            out.append(hex.toInt(16).toChar())
                            index += 4
                        }
                        else -> throw IllegalArgumentException("Escape JSON inválido.")
                    }
                }
                else -> out.append(char)
            }
        }
        throw IllegalArgumentException("Texto JSON cortado.")
    }

    private fun parseNumber(): Number {
        val start = index
        if (peek('-')) index++
        while (index < text.length && text[index].isDigit()) index++
        if (index < text.length && text[index] == '.') {
            index++
            while (index < text.length && text[index].isDigit()) index++
            return text.substring(start, index).toDouble()
        }
        return text.substring(start, index).toLong()
    }

    private fun consume(expected: Char): Boolean {
        if (index < text.length && text[index] == expected) {
            index++
            return true
        }
        return false
    }

    private fun peek(expected: Char): Boolean = index < text.length && text[index] == expected
}
