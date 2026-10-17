package app.velum.feature.player

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import app.velum.core.model.MediaTitle
import app.velum.core.model.PlaybackProgress
import app.velum.core.model.StreamSource
import app.velum.core.model.resumePositionMs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerUi(
    val title: String,
    val positionMs: Long,
    val durationMs: Long,
    val isPlaying: Boolean,
    val isBuffering: Boolean,
    val error: String?,
    val sources: List<StreamSource>,
    val selectedSourceId: String,
)

@OptIn(UnstableApi::class)
class PlayerViewModel(
    application: Application,
    private val title: MediaTitle,
    private val progress: PlaybackProgress,
) : AndroidViewModel(application) {
    val player: ExoPlayer = ExoPlayer.Builder(application).build()

    private val firstSource = title.streams.first()
    private val uiState = MutableStateFlow(
        PlayerUi(
            title = title.name,
            positionMs = 0L,
            durationMs = 0L,
            isPlaying = false,
            isBuffering = true,
            error = null,
            sources = title.streams,
            selectedSourceId = firstSource.id,
        ),
    )
    val ui: StateFlow<PlayerUi> = uiState

    private var released = false
    private var resumeAdjusted = false
    private var saveJob: Job? = null

    init {
        player.addListener(
            object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) {
                    if (released) return
                    uiState.update { it.copy(error = playbackErrorText(error), isBuffering = false) }
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (released) return
                    if (playbackState == Player.STATE_READY && !resumeAdjusted) {
                        resumeAdjusted = true
                        val duration = player.duration
                        val knownDuration = if (duration > 0L) duration else null
                        val adjusted = resumePositionMs(player.currentPosition, knownDuration)
                        if (adjusted != player.currentPosition) {
                            player.seekTo(adjusted)
                        }
                    }
                    if (playbackState == Player.STATE_ENDED) {
                        persist(0L)
                    }
                    publish()
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    if (released) return
                    if (!isPlaying) persist(player.currentPosition)
                    publish()
                }
            },
        )
        viewModelScope.launch {
            val saved = progress.observe(title.id).first()
            if (released) return@launch
            open(firstSource, resumePositionMs(saved, null))
        }
        viewModelScope.launch {
            while (isActive) {
                publish()
                delay(400)
            }
        }
    }

    fun toggle() {
        if (released) return
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekTo(positionMs: Long) {
        if (released) return
        player.seekTo(positionMs.coerceAtLeast(0L))
        publish()
    }

    fun selectSource(sourceId: String) {
        if (released) return
        val source = title.streams.firstOrNull { it.id == sourceId } ?: return
        val position = player.currentPosition.coerceAtLeast(0L)
        uiState.update { it.copy(selectedSourceId = source.id, error = null, isBuffering = true) }
        resumeAdjusted = true
        open(source, position)
    }

    fun retry() {
        if (released) return
        uiState.update { it.copy(error = null, isBuffering = true) }
        player.prepare()
        player.playWhenReady = true
    }

    override fun onCleared() {
        val position = if (player.playbackState == Player.STATE_ENDED) {
            0L
        } else {
            player.currentPosition
        }
        released = true
        persist(position)
        player.release()
        super.onCleared()
    }

    private fun open(source: StreamSource, startMs: Long) {
        val item = MediaItem.Builder()
            .setMediaId(source.id)
            .setUri(source.url)
            .build()
        player.setMediaItem(item, startMs.coerceAtLeast(0L))
        player.prepare()
        player.playWhenReady = true
        publish()
    }

    private fun publish() {
        if (released) return
        val duration = player.duration
        uiState.update { current ->
            current.copy(
                positionMs = player.currentPosition.coerceAtLeast(0L),
                durationMs = if (duration > 0L) duration else 0L,
                isPlaying = player.isPlaying,
                isBuffering = player.playbackState == Player.STATE_BUFFERING && current.error == null,
            )
        }
    }

    private fun persist(positionMs: Long) {
        val safe = positionMs.coerceAtLeast(0L)
        val id = title.id
        saveJob?.cancel()
        saveJob = progressWriteScope.launch {
            runCatching { progress.write(id, safe) }
        }
    }
}

private val progressWriteScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

internal fun playbackErrorText(error: PlaybackException): String {
    return when (error.errorCode) {
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
        -> "Sem conexão. Tente de novo quando a internet voltar."
        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
        -> "O arquivo oficial não foi encontrado no servidor."
        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
        PlaybackException.ERROR_CODE_DECODING_FAILED,
        -> "Este aparelho não conseguiu decodificar o vídeo."
        else -> "Não foi possível reproduzir este vídeo."
    }
}

class PlayerViewModelFactory(
    private val application: Application,
    private val title: MediaTitle,
    private val progress: PlaybackProgress,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlayerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlayerViewModel(application, title, progress) as T
        }
        error("ViewModel desconhecido.")
    }
}
