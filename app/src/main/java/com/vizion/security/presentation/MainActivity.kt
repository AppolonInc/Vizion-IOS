package com.vizion.security.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.vizion.security.presentation.navigation.VizionSecurityApp
import com.vizion.security.presentation.theme.VizionSecurityTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Activité principale de l'application Vizion Security
 * 
 * Cette classe représente l'écran principal et le point d'entrée de l'interface utilisateur.
 * Elle utilise Jetpack Compose pour créer une interface moderne et réactive.
 * 
 * Annotations:
 * - @AndroidEntryPoint: Permet l'injection de dépendances Hilt dans cette Activity
 * 
 * Architecture:
 * - Utilise Jetpack Compose pour l'UI (pas de XML)
 * - Applique le thème personnalisé VizionSecurityTheme
 * - Charge l'application principale via VizionSecurityApp
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    /**
     * Méthode appelée lors de la création de l'activité
     * 
     * Cette méthode configure l'interface utilisateur en utilisant Jetpack Compose.
     * Elle applique le thème de l'application et charge le composant principal.
     * 
     * @param savedInstanceState État sauvegardé de l'activité (null au premier lancement)
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Configuration de l'interface utilisateur avec Jetpack Compose
        setContent {
            // Application du thème personnalisé de Vizion Security
            // Ce thème définit les couleurs, typographies et formes de l'app
            VizionSecurityTheme {
                // Surface principale qui occupe tout l'écran
                // Elle applique la couleur de fond définie dans le thème
                Surface(
                    modifier = Modifier.fillMaxSize(), // Remplit tout l'écran disponible
                    color = MaterialTheme.colorScheme.background // Couleur de fond du thème
                ) {
                    // Chargement du composant principal de navigation et d'interface
                    // Ce composant gère toute la navigation entre les écrans
                    VizionSecurityApp()
                }
            }
        }
    }
}