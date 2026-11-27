package com.mpaznotifica.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Paleta de la marca (logo e ícono). Se mantiene igual a res/values/colors.xml.
private val AzulMPAZ = Color(0xFF0D47A1)
private val AzulMPAZClaro = Color(0xFF1565C0)
private val NaranjoAlerta = Color(0xFFEF6C00)
private val NaranjoSuave = Color(0xFFFFE0B2)
private val NaranjoOscuro = Color(0xFF3E2000)

private val LightColors = lightColorScheme(
    primary = AzulMPAZ,
    onPrimary = Color.White,
    secondary = NaranjoAlerta,
    onSecondary = Color.White,
    tertiary = NaranjoAlerta,
    onTertiary = Color.White,
    tertiaryContainer = NaranjoSuave,
    onTertiaryContainer = NaranjoOscuro
)

private val DarkColors = darkColorScheme(
    primary = AzulMPAZClaro,
    onPrimary = Color.White,
    secondary = NaranjoAlerta,
    onSecondary = Color.White,
    tertiary = NaranjoAlerta,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF5C3300),
    onTertiaryContainer = NaranjoSuave
)

@Suppress("DEPRECATION")
@Composable
fun MpazNotificaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // false = siempre usar los colores de la app
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography,
        content = content
    )
}
