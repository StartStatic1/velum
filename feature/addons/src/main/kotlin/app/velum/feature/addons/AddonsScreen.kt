package app.velum.feature.addons

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.velum.core.designsystem.SectionLabel
import app.velum.core.designsystem.VelumAmber
import app.velum.core.designsystem.VelumButton
import app.velum.core.designsystem.VelumInk
import app.velum.core.designsystem.VelumLine
import app.velum.core.designsystem.VelumMist
import app.velum.core.designsystem.VelumMistDim
import kotlinx.coroutines.launch

@Composable
fun AddonsRoute(
    gateway: AddonGateway,
    onBack: () -> Unit,
) {
    val urls by gateway.urls.collectAsStateWithLifecycle(emptyList())
    var draft by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VelumInk)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        VelumButton(text = "Voltar", onClick = onBack, primary = false)
        SectionLabel("Addons")
        Text(
            text = "Cole o endereço do manifesto. O Velum não tem servidor: ele só fala com o seu.",
            style = MaterialTheme.typography.bodyLarge,
            color = VelumMist,
        )
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("https://seu-servidor/manifest.json") },
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
        VelumButton(
            text = if (busy) "Conferindo..." else "Adicionar",
            onClick = {
                if (busy) return@VelumButton
                busy = true
                message = null
                scope.launch {
                    try {
                        gateway.add(draft)
                        draft = ""
                        message = "Addon adicionado."
                    } catch (error: Exception) {
                        message = error.message ?: "Não deu para adicionar esse addon."
                    } finally {
                        busy = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        if (message != null) {
            Text(text = message.orEmpty(), color = VelumMist, style = MaterialTheme.typography.bodyMedium)
        }
        if (urls.isEmpty()) {
            Text(
                text = "Nenhum addon ainda. Os três filmes da Blender continuam no início.",
                color = VelumMistDim,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        urls.forEach { url ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = url,
                    modifier = Modifier.weight(1f),
                    color = VelumMist,
                    style = MaterialTheme.typography.bodyMedium,
                )
                VelumButton(
                    text = "Tirar",
                    onClick = { scope.launch { gateway.remove(url) } },
                    primary = false,
                )
            }
        }
    }
}
