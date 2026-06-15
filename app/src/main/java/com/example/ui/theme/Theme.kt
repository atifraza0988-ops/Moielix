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
    primary = PrimaryBeige,
    onPrimary = BottleGreenDark,
    primaryContainer = BottleGreenLight,
    onPrimaryContainer = PrimaryBeige,
    secondary = BottleGreenLight,
    onSecondary = PrimaryBeige,
    tertiary = CinematicGold,
    background = DarkAtmosphere,
    onBackground = TextLight,
    surface = SurfaceDark,
    onSurface = TextLight,
    error = AccentRed,
    onError = TextLight,
  )

private val LightColorScheme =
  lightColorScheme(
    primary = BottleGreen,
    onPrimary = PrimaryBeige,
    primaryContainer = SecondaryBeige,
    onPrimaryContainer = BottleGreenDark,
    secondary = BottleGreenLight,
    onSecondary = PrimaryBeige,
    tertiary = CinematicGold,
    background = PrimaryBeige,
    onBackground = BottleGreenDark,
    surface = Color(0xFFF9F9EC),
    onSurface = BottleGreenDark,
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force cinematic dark representation by default
  dynamicColor: Boolean = false, // Disable dynamic colors to enforce branding
  content: @Composable () -> Unit,
) {
  val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
