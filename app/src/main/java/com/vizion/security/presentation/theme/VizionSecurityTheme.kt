package com.vizion.security.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Système de thème complet pour Vizion Security
 * 
 * Ce fichier définit l'identité visuelle complète de l'application :
 * - Palette de couleurs premium moderne
 * - Schémas de couleurs sombre et clair
 * - Couleurs spécialisées pour la sécurité
 * - Couleurs de statut et d'état
 * - Dégradés et effets visuels
 */

// Palette de couleurs premium moderne
object VizionColors {
    // === COULEURS PRINCIPALES BASÉES SUR LE LOGO ===
    val PrimaryOrange = Color(0xFFFF6B35)
    val PrimaryOrangeLight = Color(0xFFFF8A65)
    val PrimaryOrangeDark = Color(0xFFE55100)
    
    val PrimaryBlue = Color(0xFF1976D2)
    val PrimaryBlueLight = Color(0xFF42A5F5)
    val PrimaryBlueDark = Color(0xFF0D47A1)
    
    val PrimaryBlack = Color(0xFF1A1A1A)
    val PrimaryBlackLight = Color(0xFF2D2D2D)
    val PrimaryBlackDark = Color(0xFF000000)
    
    // === COULEURS SECONDAIRES HARMONIEUSES ===
    val SecondaryAmber = Color(0xFFFFC107)
    val SecondaryAmberLight = Color(0xFFFFD54F)
    val SecondaryAmberDark = Color(0xFFFF8F00)
    
    val SecondaryGray = Color(0xFF607D8B)
    val SecondaryGrayLight = Color(0xFF90A4AE)
    val SecondaryGrayDark = Color(0xFF455A64)
    
    // === COULEURS D'ACCENT POUR LA SÉCURITÉ ===
    val AccentGreen = Color(0xFF4CAF50)
    val AccentGreenLight = Color(0xFF81C784)
    val AccentGreenDark = Color(0xFF388E3C)
    
    val AccentRed = Color(0xFFF44336)
    val AccentRedLight = Color(0xFFEF5350)
    val AccentRedDark = Color(0xFFD32F2F)
    
    val AccentBlue = Color(0xFF2196F3)
    val AccentBlueLight = Color(0xFF64B5F6)
    val AccentBlueDark = Color(0xFF1976D2)
    
    // === COULEURS DE FOND MODERNES ===
    val BackgroundDark = Color(0xFF121212)
    val BackgroundDarkSecondary = Color(0xFF1E1E1E)
    val SurfaceDark = Color(0xFF2D2D2D)
    val SurfaceDarkElevated = Color(0xFF383838)
    
    val BackgroundLight = Color(0xFFFAFAFA)
    val BackgroundLightSecondary = Color(0xFFFFFFFF)
    val SurfaceLight = Color(0xFFFFFFFF)
    val SurfaceLightElevated = Color(0xFFFFF8F5)
    
    // === COULEURS DE TEXTE OPTIMISÉES ===
    val TextPrimaryDark = Color(0xFFFFFFFF)
    val TextSecondaryDark = Color(0xFFE0E0E0)
    val TextTertiaryDark = Color(0xFFBDBDBD)
    
    val TextPrimaryLight = Color(0xFF1A1A1A)
    val TextSecondaryLight = Color(0xFF424242)
    val TextTertiaryLight = Color(0xFF757575)
    
    // === COULEURS DE STATUT SÉCURITÉ ===
    val StatusSuccess = Color(0xFF4CAF50)
    val StatusWarning = Color(0xFFFF9800)
    val StatusError = Color(0xFFF44336)
    val StatusInfo = Color(0xFF2196F3)
    val StatusSecure = Color(0xFF4CAF50)
    val StatusVulnerable = Color(0xFFFF5722)
    val StatusScanning = Color(0xFFFF9800)
    val StatusProtected = Color(0xFF8BC34A)
    
    // === COULEURS SPÉCIALISÉES SÉCURITÉ ===
    val ThreatCritical = Color(0xFFD32F2F)
    val ThreatHigh = Color(0xFFFF5722)
    val ThreatMedium = Color(0xFFFF9800)
    val ThreatLow = Color(0xFFFFC107)
    val ThreatNone = Color(0xFF4CAF50)
    
    // === COULEURS DE DÉGRADÉS SIGNATURE ===
    val GradientOrangeToAmber = listOf(PrimaryOrange, SecondaryAmber)
    val GradientBlackToGray = listOf(PrimaryBlack, SecondaryGray)
    val GradientOrangeToRed = listOf(PrimaryOrange, AccentRed)
    val GradientAmberToOrange = listOf(SecondaryAmber, PrimaryOrange)
}

// Schéma de couleurs pour le thème sombre
private val DarkColorScheme = darkColorScheme(
    primary = VizionColors.PrimaryOrange,
    onPrimary = Color.White,
    primaryContainer = VizionColors.PrimaryOrangeDark,
    onPrimaryContainer = Color.White,
    
    secondary = VizionColors.SecondaryAmber,
    onSecondary = Color.White,
    secondaryContainer = VizionColors.SecondaryAmberDark,
    onSecondaryContainer = Color.White,
    
    tertiary = VizionColors.AccentBlue,
    onTertiary = Color.White,
    tertiaryContainer = VizionColors.AccentBlueDark,
    onTertiaryContainer = Color.White,
    
    background = VizionColors.BackgroundDark,
    onBackground = VizionColors.TextPrimaryDark,
    
    surface = VizionColors.SurfaceDark,
    onSurface = VizionColors.TextPrimaryDark,
    surfaceVariant = VizionColors.SurfaceDarkElevated,
    onSurfaceVariant = VizionColors.TextSecondaryDark,
    
    error = VizionColors.AccentRed,
    onError = Color.White,
    
    outline = VizionColors.TextTertiaryDark,
    outlineVariant = VizionColors.SecondaryGrayDark
)

// Schéma de couleurs pour le thème clair
private val LightColorScheme = lightColorScheme(
    primary = VizionColors.PrimaryOrange,
    onPrimary = Color.White,
    primaryContainer = VizionColors.PrimaryOrangeLight,
    onPrimaryContainer = VizionColors.PrimaryBlack,
    
    secondary = VizionColors.SecondaryAmber,
    onSecondary = VizionColors.PrimaryBlack,
    secondaryContainer = VizionColors.SecondaryAmberLight,
    onSecondaryContainer = VizionColors.PrimaryBlack,
    
    tertiary = VizionColors.AccentBlue,
    onTertiary = Color.White,
    tertiaryContainer = VizionColors.AccentBlueLight,
    onTertiaryContainer = VizionColors.PrimaryBlack,
    
    background = VizionColors.BackgroundLight,
    onBackground = VizionColors.TextPrimaryLight,
    
    surface = VizionColors.SurfaceLight,
    onSurface = VizionColors.TextPrimaryLight,
    surfaceVariant = VizionColors.SurfaceLightElevated,
    onSurfaceVariant = VizionColors.TextSecondaryLight,
    
    error = VizionColors.AccentRed,
    onError = Color.White,
    
    outline = VizionColors.TextTertiaryLight,
    outlineVariant = VizionColors.SecondaryGrayLight
)

/**
 * Composant principal du thème Vizion Security
 */
@Composable
fun VizionSecurityTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}