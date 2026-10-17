package app.velum.feature.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import app.velum.core.designsystem.SectionLabel
import app.velum.core.designsystem.VelumAmber
import app.velum.core.designsystem.VelumButton
import app.velum.core.designsystem.VelumInk
import app.velum.core.designsystem.VelumLine
import app.velum.core.designsystem.VelumMist
import app.velum.core.designsystem.VelumMistDim
import app.velum.core.designsystem.VelumPoster
import app.velum.core.model.DemoCatalog
import app.velum.core.model.MediaTitle
import app.velum.core.model.formatByteSize
import app.velum.feature.addons.AddonGateway
import app.velum.feature.addons.AddonShelf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUi(
    val query: String,
    val featured: MediaTitle?,
    val results: List<MediaTitle>,
    val shelves: List<AddonShelf>,
)

class HomeViewModel(
    gateway: AddonGateway,
) : ViewModel() {
    private val query = MutableStateFlow("")
    private val shelves = MutableStateFlow<List<AddonShelf>>(emptyList())

    val ui: StateFlow<HomeUi> = combine(query, shelves) { raw, addonShelves ->
        val trimmed = raw.trim()
        val results = if (trimmed.isEmpty()) {
            DemoCatalog.titles
        } else {
            DemoCatalog.titles.filter { title ->
                title.name.contains(trimmed, ignoreCase = true) ||
                    title.synopsis.contains(trimmed, ignoreCase = true) ||
                    title.kindLabel.contains(trimmed, ignoreCase = true)
            }
        }
        val visibleShelves = if (trimmed.isEmpty()) {
            addonShelves
        } else {
            addonShelves.mapNotNull { shelf ->
                val matched = shelf.titles.filter { it.name.contains(trimmed, ignoreCase = true) }
                if (matched.isEmpty()) null else shelf.copy(titles = matched)
            }
        }
        HomeUi(
            query = raw,
            featured = if (trimmed.isEmpty()) DemoCatalog.featured else null,
            results = results,
            shelves = visibleShelves,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        HomeUi("", DemoCatalog.featured, DemoCatalog.titles, emptyList()),
    )

    init {
        viewModelScope.launch {
            gateway.urls.collect { urls ->
                shelves.value = gateway.loadShelves(urls)
            }
        }
    }

    fun onQuery(value: String) {
        query.value = value
    }
}

@Composable
fun HomeRoute(
    gateway: AddonGateway,
    onOpenDetails: (String) -> Unit,
    onPlay: (String) -> Unit,
    onOpenAddons: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: HomeViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return HomeViewModel(gateway) as T
            }
        },
    )
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    HomeScreen(
        ui = ui,
        onQuery = viewModel::onQuery,
        onOpenDetails = onOpenDetails,
        onPlay = onPlay,
        onOpenAddons = onOpenAddons,
        modifier = modifier,
    )
}

@Composable
fun HomeScreen(
    ui: HomeUi,
    onQuery: (String) -> Unit,
    onOpenDetails: (String) -> Unit,
    onPlay: (String) -> Unit,
    onOpenAddons: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VelumInk)
            .verticalScroll(rememberScrollState())
            .padding(bottom = 28.dp),
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 20.dp)) {
            SectionLabel("Velum")
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Catálogo claro.",
                style = MaterialTheme.typography.headlineMedium,
                color = VelumMist,
            )
            Text(
                text = "Filmes livres da Blender, e os addons do seu servidor.",
                style = MaterialTheme.typography.bodyMedium,
                color = VelumMistDim,
            )
            Spacer(modifier = Modifier.height(12.dp))
            VelumButton(text = "Addons", onClick = onOpenAddons, primary = false)
            Spacer(modifier = Modifier.height(18.dp))
            OutlinedTextField(
                value = ui.query,
                onValueChange = onQuery,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Buscar no catálogo") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VelumAmber,
                    unfocusedBorderColor = VelumLine,
                    cursorColor = VelumAmber,
                    focusedTextColor = VelumMist,
                    unfocusedTextColor = VelumMist,
                    focusedPlaceholderColor = VelumMistDim,
                    unfocusedPlaceholderColor = VelumMistDim,
                ),
            )
        }

        val featured = ui.featured
        if (featured != null) {
            FeaturedBlock(
                title = featured,
                onPlay = { onPlay(featured.id) },
                onDetails = { onOpenDetails(featured.id) },
            )
        }

        Spacer(modifier = Modifier.height(22.dp))
        SectionLabel(
            text = if (ui.query.isBlank()) "No catálogo" else "Resultados",
            modifier = Modifier.padding(horizontal = 20.dp),
        )
        Spacer(modifier = Modifier.height(12.dp))
        if (ui.results.isEmpty()) {
            Text(
                text = "Nada com esse nome no catálogo de demonstração.",
                modifier = Modifier.padding(horizontal = 20.dp),
                color = VelumMistDim,
                style = MaterialTheme.typography.bodyLarge,
            )
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(ui.results, key = { it.id }) { title ->
                    VelumPoster(
                        letter = title.name,
                        title = title.name,
                        year = title.year.toString(),
                        sizeLabel = formatByteSize(title.streams.first().bytes),
                        onClick = { onOpenDetails(title.id) },
                    )
                }
            }
        }
        ui.shelves.forEach { shelf ->
            Spacer(modifier = Modifier.height(22.dp))
            SectionLabel(text = shelf.name, modifier = Modifier.padding(horizontal = 20.dp))
            Spacer(modifier = Modifier.height(12.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(shelf.titles, key = { it.id }) { title ->
                    VelumPoster(
                        letter = title.name,
                        title = title.name,
                        year = if (title.year == 0) "Addon" else title.year.toString(),
                        sizeLabel = title.kindLabel,
                        onClick = { onOpenDetails(title.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun FeaturedBlock(
    title: MediaTitle,
    onPlay: () -> Unit,
    onDetails: () -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(20.dp),
    ) {
        SectionLabel(title.kindLabel)
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = title.name,
            style = MaterialTheme.typography.displayLarge,
            color = VelumMist,
        )
        Text(
            text = "${title.year} · ${formatByteSize(title.streams.first().bytes)} · ${title.credit}",
            style = MaterialTheme.typography.bodyMedium,
            color = VelumMistDim,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = title.synopsis,
            style = MaterialTheme.typography.bodyLarge,
            color = VelumMist,
            maxLines = 4,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            VelumButton(text = "Assistir", onClick = onPlay)
            VelumButton(text = "Detalhes", onClick = onDetails, primary = false)
        }
    }
}
