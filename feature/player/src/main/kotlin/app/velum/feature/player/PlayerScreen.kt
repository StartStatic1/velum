package app.velum.feature.player

import android.app.Activity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.compose.PlayerSurface
import app.velum.core.designsystem.VelumAmber
import app.velum.core.designsystem.VelumButton
import app.velum.core.designsystem.VelumDanger
import app.velum.core.designsystem.VelumMist
import app.velum.core.model.DemoCatalog
import app.velum.core.model.PlaybackProgress
import app.velum.core.model.formatClock

@OptIn(UnstableApi::class)
@Composable
fun PlayerRoute(
    titleId: String,
    progress: PlaybackProgress,
    onBack: () -> Unit,
) {
    val title = DemoCatalog.find(titleId)
    if (title == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(text = "Título não encontrado.", color = VelumMist)
            VelumButton(text = "Voltar", onClick = onBack, primary = false)
        }
        return
    }
    val context = LocalContext.current
    val viewModel: PlayerViewModel = viewModel(
        key = "player-${title.id}",
        factory = PlayerViewModelFactory(
            application = context.applicationContext as android.app.Application,
            title = title,
            progress = progress,
        ),
    )
    PlayerScreen(viewModel = viewModel, onBack = onBack)
}

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel,
    onBack: () -> Unit,
) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val window = (context as? Activity)?.window
        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        PlayerSurface(
            player = viewModel.player,
            modifier = Modifier.fillMaxSize(),
        )
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.55f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            VelumButton(text = "Voltar", onClick = onBack, primary = false)
            Text(text = ui.title, color = VelumMist)
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.62f))
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (ui.isBuffering) {
                Text(text = "Carregando…", color = VelumMist)
            }
            ui.error?.let { message ->
                Text(text = message, color = VelumDanger)
                VelumButton(text = "Tentar de novo", onClick = viewModel::retry)
            }
            if (ui.sources.size > 1) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ui.sources.forEach { source ->
                        VelumButton(
                            text = source.label,
                            onClick = { viewModel.selectSource(source.id) },
                            primary = source.id == ui.selectedSourceId,
                        )
                    }
                }
            } else {
                Text(text = ui.sources.first().label, color = VelumMist)
            }
            if (ui.durationMs > 0L) {
                Slider(
                    value = ui.positionMs.coerceIn(0L, ui.durationMs).toFloat(),
                    onValueChange = { viewModel.seekTo(it.toLong()) },
                    valueRange = 0f..ui.durationMs.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = VelumAmber,
                        activeTrackColor = VelumAmber,
                    ),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                VelumButton(
                    text = if (ui.isPlaying) "Pausar" else "Reproduzir",
                    onClick = viewModel::toggle,
                )
                Text(
                    text = "${formatClock(ui.positionMs)} / ${formatClock(ui.durationMs)}",
                    color = VelumMist,
                )
            }
        }
    }
}
