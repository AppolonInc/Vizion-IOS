package com.vizion.security.presentation.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vizion.security.presentation.screen.AlertsScreen
import com.vizion.security.presentation.screen.LoginScreen
import com.vizion.security.presentation.screen.DashboardScreen
import com.vizion.security.presentation.screen.PrivacyScreen
import com.vizion.security.presentation.screen.SettingsScreen
import com.vizion.security.presentation.screen.WazuhScreen
import com.vizion.security.presentation.theme.VizionColors
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Classe de données représentant un élément de navigation
 *
 * Cette data class encapsule toutes les informations nécessaires pour
 * créer un onglet de navigation dans la barre de navigation inférieure.
 *
 * @param title Le texte affiché sous l'icône dans la navigation
 * @param icon L'icône Material Design affichée pour cet onglet
 * @param screen Le composable Compose qui sera affiché quand cet onglet est sélectionné
 */
data class NavigationItem(
    val title: String,
    val icon: ImageVector,
    val screen: @Composable () -> Unit
)

/**
 * Composant principal de l'application Vizion Security
 *
 * Ce composant gère la structure générale de l'application avec:
 * - Une barre de navigation inférieure avec 5 onglets
 * - Un système de navigation entre les différents écrans
 * - Des animations de transition entre les écrans
 *
 * Architecture:
 * - Utilise Scaffold pour la structure générale (Material Design)
 * - Gère l'état de navigation avec remember et mutableIntStateOf
 * - Applique des animations fluides entre les écrans
 */
@Composable
fun VizionSecurityApp(
) {
    // Pour l'instant, afficher directement l'application principale
    // TODO: Implémenter la logique d'authentification
    MainApp()
}

@Composable
private fun MainApp() {
    // Définition des 5 écrans principaux de l'application
    // Chaque élément contient le titre, l'icône et le composant d'écran correspondant
    val navigationItems = listOf(
        // Écran principal avec vue d'ensemble de la sécurité
        NavigationItem("Dashboard", Icons.Default.Dashboard) { DashboardScreen() },
        // Écran des alertes de sécurité en temps réel
        NavigationItem("Alertes", Icons.Default.Notifications) { AlertsScreen() },
        // Écran d'analyse de la confidentialité des applications
        NavigationItem("Confidentialité", Icons.Default.PrivacyTip) { PrivacyScreen() },
        // Écran de monitoring du serveur Wazuh SIEM
        NavigationItem("Wazuh", Icons.Default.Security) { WazuhScreen() },
        // Écran des paramètres et configuration
        NavigationItem("Paramètres", Icons.Default.Settings) { SettingsScreen() }
    )

    // État local pour suivre quel onglet est actuellement sélectionné
    // remember permet de conserver l'état lors des recompositions
    // mutableIntStateOf crée un état observable pour un entier
    var selectedItem by remember { mutableIntStateOf(0) }

    // Scaffold fournit la structure de base Material Design
    // Il gère automatiquement la disposition des éléments (contenu + navigation)
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        // Barre de navigation inférieure personnalisée
        bottomBar = {
            ModernNavigationBar(
                items = navigationItems,
                selectedItem = selectedItem,
                // Callback appelé quand l'utilisateur sélectionne un onglet
                onItemSelected = { selectedItem = it }
            )
        }
    ) { innerPadding ->
        // Box contient le contenu principal de l'écran sélectionné
        Box(
            modifier = Modifier
                .fillMaxSize()
                // innerPadding évite que le contenu soit masqué par la navigation
                .padding(innerPadding)
        ) {
            // AnimatedContent gère les transitions animées entre les écrans
            // Quand selectedItem change, il anime la transition vers le nouvel écran
            AnimatedContent(
                targetState = selectedItem, // L'état cible pour l'animation
                // Configuration des animations d'entrée et de sortie
                transitionSpec = {
                    // fadeIn: animation d'apparition en fondu (300ms)
                    // fadeOut: animation de disparition en fondu (300ms)
                    // togetherWith: les deux animations se déroulent simultanément
                    fadeIn(animationSpec = tween(300)) togetherWith
                            fadeOut(animationSpec = tween(300))
                },
                label = "screen_transition", // Label pour le débogage des animations
                modifier = Modifier.fillMaxSize()
            ) { targetState ->
                // Affichage de l'écran correspondant à l'onglet sélectionné
                navigationItems[targetState].screen()
            }
        }
    }
}

/**
 * Barre de navigation moderne et personnalisée
 *
 * Ce composant crée une barre de navigation inférieure avec un design premium:
 * - Icônes avec arrière-plan circulaire pour l'élément sélectionné
 * - Dégradés de couleurs pour les éléments actifs
 * - Animations fluides lors des changements de sélection
 *
 * @param items Liste des éléments de navigation à afficher
 * @param selectedItem Index de l'élément actuellement sélectionné
 * @param onItemSelected Callback appelé lors de la sélection d'un élément
 */
@Composable
fun ModernNavigationBar(
    items: List<NavigationItem>,
    selectedItem: Int,
    onItemSelected: (Int) -> Unit
) {
    // NavigationBar est le composant Material 3 pour la navigation inférieure
    NavigationBar(
        // Couleur de fond de la barre de navigation
        containerColor = MaterialTheme.colorScheme.surface,
        // Élévation pour créer une ombre subtile
        tonalElevation = 16.dp
    ) {
        // Itération sur chaque élément de navigation avec son index
        items.forEachIndexed { index, item ->
            // Détermine si cet élément est actuellement sélectionné
            val isSelected = selectedItem == index

            // NavigationBarItem représente un onglet individuel
            NavigationBarItem(
                // Configuration de l'icône avec design personnalisé
                icon = {
                    // Box circulaire contenant l'icône
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            // Forme circulaire pour l'arrière-plan
                            .clip(CircleShape)
                            // Arrière-plan avec dégradé si sélectionné, transparent sinon
                            .background(
                                if (isSelected) {
                                    // Dégradé bleu pour l'élément sélectionné
                                    Brush.linearGradient(
                                        colors = listOf(
                                            VizionColors.PrimaryBlue,
                                            VizionColors.PrimaryBlueLight
                                        )
                                    )
                                } else {
                                    // Arrière-plan transparent pour les éléments non sélectionnés
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Transparent
                                        )
                                    )
                                }
                            ),
                        // Centrage de l'icône dans le cercle
                        contentAlignment = Alignment.Center
                    ) {
                        // Icône Material Design
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.title,
                            // Couleur blanche si sélectionné, grise sinon
                            tint = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                // Label textuel sous l'icône
                label = {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (isSelected) VizionColors.PrimaryBlue else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                },
                selected = isSelected,
                // Action à exécuter lors du clic sur cet onglet
                onClick = { onItemSelected(index) },
                // Personnalisation des couleurs de l'élément de navigation
                colors = NavigationBarItemDefaults.colors(
                    // Couleurs pour l'état sélectionné
                    selectedIconColor = Color.White,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    selectedTextColor = VizionColors.PrimaryBlue,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}