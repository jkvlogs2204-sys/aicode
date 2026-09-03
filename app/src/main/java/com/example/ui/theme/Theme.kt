package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = EcoPrimary,
    onPrimary = Color(0xFF003820),
    primaryContainer = EcoPrimaryContainer,
    onPrimaryContainer = EcoOnPrimaryContainer,
    secondary = EcoSecondary,
    onSecondary = EcoTextPrimary,
    secondaryContainer = EcoSecondaryContainer,
    onSecondaryContainer = EcoAccent,
    tertiary = EcoAccent,
    onTertiary = Color(0xFF003820),
    tertiaryContainer = EcoSecondaryContainer,
    onTertiaryContainer = EcoAccent,
    background = EcoBackground,
    onBackground = EcoTextPrimary,
    surface = EcoCard,
    onSurface = EcoTextPrimary,
    surfaceVariant = EcoSecondaryContainer,
    onSurfaceVariant = EcoTextSecondary,
    outline = EcoBorder,
    outlineVariant = EcoBorder.copy(alpha = 0.5f),
    error = EcoError,
    onError = Color.White
  )

private val LightColorScheme = DarkColorScheme

@Composable
fun EcoMindTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme =
    when {
      dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
        val context = LocalContext.current
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
      }

      darkTheme -> DarkColorScheme
      else -> LightColorScheme
    }

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
