package app.velum.feature.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.velum.core.designsystem.SectionLabel
import app.velum.core.designsystem.VelumAmber
import app.velum.core.designsystem.VelumButton
import app.velum.core.designsystem.VelumInk
import app.velum.core.designsystem.VelumMist
import app.velum.core.designsystem.VelumMistDim
import app.velum.core.model.DemoCatalog
import app.velum.core.model.MediaTitle
import app.velum.core.model.PlaybackProgress
import app.velum.core.model.PreparedTitles
import app.velum.core.model.formatByteSize
import app.velum.core.model.formatClock
import app.velum.feature.addons.AddonGateway
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DetailsViewModel(
    titleId: String,
    progress: PlaybackProgress,
    gateway: AddonGateway,
) : ViewModel() {
    val title = MutableStateFlow(PreparedTitles.title(titleId))
    val status = MutableStateFlow<String?>(null)

    val resumeMs: StateFlow<Long> = progress.observe(titleId).stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        0L,
    )

    init {
        if (DemoCatalog.find(titleId) == null) {
            viewModelScope.launch {
                status.value = "Buscando no addon..."
                try {
                    val resolved = gateway.resolve(titleId)
                    title.value = resolved
                    status.value = when {
                        resolved == null -> "Não achei esse título."
                        resolved.streams.isEmpty() -> "Esse addon não entregou um link HTTPS direto."
                        else -> null
                    }
                } catch (error: Exception) {
                    status.value = error.message ?: "Não deu para abrir o addon."
                }
            }
        }
    }
}

class DetailsViewModelFactory(
    private val titleId: String,
    private val progress: PlaybackProgress,
    private val gateway: AddonGateway,
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DetailsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return DetailsViewModel(titleId, progress, gateway) as T
        }
        error("ViewModel desconhecido.")
    }
}

@Composable
fun DetailsRoute(
    titleId: String,
    progress: PlaybackProgress,
    gateway: AddonGateway,
    onBack: () -> Unit,
    onPlay: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: DetailsViewModel = viewModel(
        key = "details-$titleId",
        factory = DetailsViewModelFactory(titleId, progress, gateway),
    )
    val resumeMs by viewModel.resumeMs.collectAsStateWithLifecycle()
    val title by viewModel.title.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    DetailsScreen(
        title = title,
        status = status,
        resumeMs = resumeMs,
        onBack = onBack,
        onPlay = { item -> onPlay(item.id) },
        modifier = modifier,
    )
}

@Composable
fun DetailsScreen(
    title: MediaTitle?,
    status: String?,
    resumeMs: Long,
    onBack: () -> Unit,
    onPlay: (MediaTitle) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VelumInk)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        VelumButton(text = "Voltar", onClick = onBack, primary = false)
        if (title == null) {
            Text(
                text = "Esse título não está no catálogo de demonstração.",
                style = MaterialTheme.typography.headlineMedium,
                color = VelumMist,
            )
        } else {
            Text(
                text = title.name.take(1).uppercase(),
                color = VelumAmber,
                fontSize = 64.sp,
                style = MaterialTheme.typography.displayLarge,
            )
            SectionLabel(title.kindLabel)
            Text(
                text = title.name,
                style = MaterialTheme.typography.headlineMedium,
                color = VelumMist,
            )
            Text(
                text = "${title.year} · ${title.credit}",
                style = MaterialTheme.typography.bodyMedium,
                color = VelumMistDim,
            )
            Text(
                text = title.synopsis,
                style = MaterialTheme.typography.bodyLarge,
                color = VelumMist,
            )
            SectionLabel("Fontes")
            if (title.streams.isEmpty()) {
                Text(
                    text = status ?: "Sem fonte HTTPS direta.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VelumMist,
                )
            }
            title.streams.forEach { stream ->
                val size = if (stream.bytes > 0L) " · ${formatByteSize(stream.bytes)}" else ""
                Text(
                    text = "${stream.label}$size",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VelumMist,
                )
            }
            Text(
                text = if (title.license == "Addon") {
                    "O Velum não hospeda o vídeo. A reprodução usa o link HTTPS que o addon entregou."
                } else {
                    "Licença: ${title.license}. O Velum não hospeda o arquivo. A reprodução usa o endereço oficial em download.blender.org."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = VelumMistDim,
            )
            if (title.id == "tears-of-steel") {
                Text(
                    text = "Este arquivo tem cerca de 372 MB. Em rede móvel, prefira um trailer.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = VelumMist,
                )
            }
            if (title.streams.isNotEmpty()) {
                val action = if (resumeMs >= 5_000L) {
                    "Continuar de ${formatClock(resumeMs)}"
                } else {
                    "Assistir"
                }
                Spacer(modifier = Modifier.height(4.dp))
                VelumButton(
                    text = action,
                    onClick = { onPlay(title) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
