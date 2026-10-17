package app.velum.core.model

import java.net.URI
import kotlinx.coroutines.flow.Flow

private val titleIdPattern = Regex("^[a-z0-9-]{1,64}$")

private val allowedStreamHosts = setOf("download.blender.org")

private val allowedExtensions = setOf(".mp4", ".m4v", ".mov")

fun requireTitleId(id: String) {
    require(titleIdPattern.matches(id)) { "Id de título inválido." }
}

fun validateStreamUrl(url: String) {
    val uri = URI(url)
    require(uri.scheme == "https") { "A fonte precisa usar HTTPS." }
    val host = uri.host
    require(host != null && host in allowedStreamHosts) { "Servidor de vídeo não autorizado." }
    require(uri.userInfo == null) { "A URL da fonte não pode levar usuário ou senha." }
    require(uri.rawQuery == null && uri.fragment == null) { "A URL da fonte não pode levar parâmetros." }
    val path = uri.path ?: ""
    require(allowedExtensions.any { path.endsWith(it) }) { "Formato de arquivo não suportado nesta versão." }
}

interface PlaybackProgress {
    fun observe(titleId: String): Flow<Long>

    suspend fun write(titleId: String, positionMs: Long)
}

/**
 * Posição usada ao reabrir o vídeo.
 * Se a duração é conhecida e falta pouco para o fim, a reprodução recomeça.
 */
fun resumePositionMs(savedMs: Long, durationMs: Long?): Long {
    if (savedMs <= 0L) return 0L
    if (durationMs == null || durationMs <= 0L) return savedMs
    val endGuardMs = 5_000L
    return if (savedMs >= durationMs - endGuardMs) 0L else savedMs
}

fun formatClock(positionMs: Long): String {
    val totalSeconds = positionMs.coerceAtLeast(0L) / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        "$hours:${minutes.twoDigits()}:${seconds.twoDigits()}"
    } else {
        "$minutes:${seconds.twoDigits()}"
    }
}

fun formatByteSize(bytes: Long): String {
    require(bytes >= 0L)
    if (bytes < 1_000_000L) return "${bytes / 1_000L} KB"
    val tenths = (bytes + 50_000L) / 100_000L
    val whole = tenths / 10L
    val fraction = tenths % 10L
    return if (whole >= 10L) "$whole MB" else "$whole,$fraction MB"
}

private fun Long.twoDigits(): String = toString().padStart(2, '0')
