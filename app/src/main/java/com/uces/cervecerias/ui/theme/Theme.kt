package com.uces.cervecerias.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Los mismos valores que colors.xml: una sola paleta para toda la aplicacion,
// la use la parte en XML o la parte en Compose.
val Ink = Color(0xFF13161A)
val Surface = Color(0xFF1B1F24)
val SurfaceHigh = Color(0xFF222730)
val Hairline = Color(0xFF292E34)
val HairlineSoft = Color(0xFF21252B)
val TextPrimary = Color(0xFFF2EFE9)
val TextSecondary = Color(0xFF8F979F)
val TextTertiary = Color(0xFF616971)
val Signal = Color(0xFFE85D3D)
val Pending = Color(0xFFE2A33C)

private val DarkColors = darkColorScheme(
    primary = Signal,
    onPrimary = Ink,
    background = Ink,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceHigh,
    outline = Hairline
)

private val AppTypography = Typography(
    headlineLarge = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold),
    titleMedium = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Normal),
    labelSmall = TextStyle(
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.6.sp
    ),
    bodyMedium = TextStyle(fontSize = 14.sp)
)

/**
 * La aplicacion es siempre oscura: se usa de noche, en la calle, buscando
 * adonde ir. Un tema claro ahi encandila.
 */
@Composable
fun CerveceriasTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = AppTypography,
        content = content
    )
}
