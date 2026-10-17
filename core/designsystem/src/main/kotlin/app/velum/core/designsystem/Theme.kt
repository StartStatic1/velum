package app.velum.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val VelumInk = Color(0xFF0B0D10)
val VelumInkRaised = Color(0xFF161A20)
val VelumAmber = Color(0xFFE8A54B)
val VelumMist = Color(0xFFE7E4DC)
val VelumMistDim = Color(0xFFA39E92)
val VelumLine = Color(0xFF2C3138)
val VelumDanger = Color(0xFFE36B6B)

private val velumColors = darkColorScheme(
    primary = VelumAmber,
    onPrimary = VelumInk,
    background = VelumInk,
    onBackground = VelumMist,
    surface = VelumInkRaised,
    onSurface = VelumMist,
    surfaceVariant = Color(0xFF22262C),
    onSurfaceVariant = VelumMistDim,
    outline = VelumLine,
    error = VelumDanger,
    onError = VelumInk,
)

private val velumTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 44.sp,
        lineHeight = 48.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 32.sp,
        lineHeight = 36.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Normal,
        fontSize = 24.sp,
        lineHeight = 28.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 1.2.sp,
    ),
)

@Composable
fun VelumTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = velumColors,
        typography = velumTypography,
        content = content,
    )
}
