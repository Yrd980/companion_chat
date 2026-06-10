package com.companion.chat.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ColorDarkMutedText = Color(0xFFC5CDBE)
private val ColorDarkOutline = Color(0xFF3A4234)
private val ColorErrorContainerLight = Color(0xFFFFDAD8)
private val ColorErrorContainerDark = Color(0xFF5C1118)
private val ColorWarningContainerLight = Color(0xFFFFE1B8)
private val ColorWarningContainerDark = Color(0xFF4E2E00)

private val DarkColorScheme = darkColorScheme(
    primary = CompanionGreen,
    onPrimary = PanelWhite,
    primaryContainer = GreenSoftFill,
    onPrimaryContainer = CharcoalInk,
    secondary = MutedOliveGray,
    onSecondary = PanelWhite,
    secondaryContainer = DarkField,
    onSecondaryContainer = PanelWhite,
    tertiary = WarningAmber,
    onTertiary = PanelWhite,
    tertiaryContainer = ColorWarningContainerDark,
    onTertiaryContainer = PanelWhite,
    error = AlertRed,
    onError = PanelWhite,
    errorContainer = ColorErrorContainerDark,
    onErrorContainer = PanelWhite,
    background = DarkCanvas,
    onBackground = PanelWhite,
    surface = DarkPanel,
    onSurface = PanelWhite,
    surfaceVariant = DarkField,
    onSurfaceVariant = ColorDarkMutedText,
    surfaceContainer = DarkPanel,
    surfaceContainerHigh = DarkField,
    outline = ColorDarkOutline,
    outlineVariant = ColorDarkOutline
)

private val LightColorScheme = lightColorScheme(
    primary = CompanionGreen,
    onPrimary = PanelWhite,
    primaryContainer = GreenSoftFill,
    onPrimaryContainer = CharcoalInk,
    secondary = MutedOliveGray,
    onSecondary = PanelWhite,
    secondaryContainer = SoftField,
    onSecondaryContainer = CharcoalInk,
    tertiary = WarningAmber,
    onTertiary = PanelWhite,
    tertiaryContainer = ColorWarningContainerLight,
    onTertiaryContainer = CharcoalInk,
    error = AlertRed,
    onError = PanelWhite,
    errorContainer = ColorErrorContainerLight,
    onErrorContainer = CharcoalInk,
    background = CanvasWhite,
    onBackground = CharcoalInk,
    surface = PanelWhite,
    onSurface = CharcoalInk,
    surfaceVariant = SoftField,
    onSurfaceVariant = MutedOliveGray,
    surfaceContainer = PanelWhite,
    surfaceContainerHigh = SoftField,
    outline = LineGray,
    outlineVariant = LineGray
)

@Composable
fun CompanionChatTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
