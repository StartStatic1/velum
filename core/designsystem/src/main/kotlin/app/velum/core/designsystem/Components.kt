package app.velum.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        modifier = modifier.semantics { heading() },
        style = MaterialTheme.typography.labelLarge,
        color = VelumAmber,
    )
}

@Composable
fun VelumPoster(
    letter: String,
    title: String,
    year: String,
    sizeLabel: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(132.dp)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(VelumInkRaised)
            .border(1.dp, VelumLine, MaterialTheme.shapes.extraSmall)
            .clickable(onClick = onClick)
            .padding(12.dp)
            .heightIn(min = 188.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = letter.take(1).uppercase(),
            style = MaterialTheme.typography.displayLarge,
            color = VelumAmber,
            fontSize = 42.sp,
        )
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = VelumMist,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "$year · $sizeLabel",
                style = MaterialTheme.typography.bodyMedium,
                color = VelumMistDim,
                maxLines = 1,
            )
        }
    }
}

@Composable
fun VelumButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = true,
) {
    if (primary) {
        Button(
            onClick = onClick,
            modifier = modifier.heightIn(min = 48.dp),
            shape = MaterialTheme.shapes.extraSmall,
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = VelumAmber,
                contentColor = VelumInk,
            ),
        ) {
            Text(text = text, style = MaterialTheme.typography.titleMedium)
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier.heightIn(min = 48.dp),
            shape = MaterialTheme.shapes.extraSmall,
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = VelumMist),
        ) {
            Text(text = text, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0D10)
@Composable
private fun PosterPreview() {
    VelumTheme {
        Box(
            modifier = Modifier
                .background(VelumInk)
                .padding(16.dp)
                .fillMaxWidth(),
        ) {
            VelumPoster(
                letter = "S",
                title = "Sintel",
                year = "2010",
                sizeLabel = "7,6 MB",
                onClick = {},
            )
        }
    }
}
