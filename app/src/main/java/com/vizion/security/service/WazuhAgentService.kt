package com.vizion.security.service

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import com.vizion.security.data.repository.WazuhRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Service Android pour l'agent Wazuh SIEM
 * 
 * Ce service foreground maintient une connexion permanente avec le serveur Wazuh
 * et gère la transmission en temps réel des logs de sécurité.
 * 
 * Fonctionnalités principales:
 * - Connexion TCP persistante au serveur Wazuh (159.65.120.14:1514)
 * - Envoi de heartbeat périodique (toutes les 30 secondes)
 * - Synchronisation des logs non envoyés (toutes les minutes)
 * - Notification permanente indiquant l'état de connexion
 * - Reconnexion automatique en cas de perte de réseau
 * 
 * Architecture:
 * - Service foreground pour fonctionner en arrière-plan
 * - Utilise les coroutines pour les opérations asynchrones
 * - Injection de dépendances avec Hilt
 * - Gestion automatique du cycle de vie
 */
@AndroidEntryPoint
class WazuhAgentService : Service() {
    
    // Repository injecté pour la communication Wazuh
    // TODO: Réactiver quand WazuhRepository sera disponible
    // @Inject
    // lateinit var wazuhRepository: WazuhRepository
    
    // Scope pour les coroutines du service (lié au cycle de vie du service)
    private val serviceScope = CoroutineScope(Dispatchers.IO + Job())
    // Job pour la gestion du heartbeat périodique
    private var heartbeatJob: Job? = null
    // Job pour la synchronisation périodique des logs
    private var syncJob: Job? = null
    
    // === CONSTANTES DE CONFIGURATION ===
    companion object {
        private const val TAG = "WazuhAgentService"
        // ID unique pour la notification foreground
        private const val NOTIFICATION_ID = 1002
        // Canal de notification pour Android 8+
        private const val CHANNEL_ID = "wazuh_agent"
        // Intervalle entre les heartbeats (30 secondes)
        private const val HEARTBEAT_INTERVAL = 30000L // 30 seconds
        // Intervalle de synchronisation des logs (1 minute)
        private const val SYNC_INTERVAL = 60000L // 1 minute
    }
    
    /**
     * Méthode appelée lors de la création du service
     * Initialise le canal de notification nécessaire pour le service foreground
     */
    override fun onCreate() {
        super.onCreate()
        // Création du canal de notification (requis pour Android 8+)
        createNotificationChannel()
        Log.d(TAG, "WazuhAgentService created")
    }
    
    /**
     * Méthode appelée lors du démarrage du service
     * 
     * @param intent Intent qui a déclenché le démarrage
     * @param flags Flags de démarrage
     * @param startId ID unique de cette instance de démarrage
     * @return START_STICKY pour redémarrage automatique si tué par le système
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "WazuhAgentService starting...")
        // Démarrage en mode foreground avec notification
        startForeground(NOTIFICATION_ID, createNotification("Démarrage..."))
        // Lancement de l'agent Wazuh
        startWazuhAgent()
        // START_STICKY = redémarrage automatique si le service est tué
        return START_STICKY
    }
    
    /**
     * Ce service ne supporte pas le binding
     * @return null car pas de binding supporté
     */
    override fun onBind(intent: Intent?): IBinder? = null
    
    /**
     * Crée le canal de notification pour Android 8.0+
     * 
     * Les canaux de notification permettent à l'utilisateur de contrôler
     * les notifications par catégorie dans les paramètres Android.
     */
    private fun createNotificationChannel() {
        // Vérification de la version Android (canaux requis depuis API 26)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Wazuh Agent", // Nom affiché à l'utilisateur
                NotificationManager.IMPORTANCE_LOW // Importance faible (pas de son/vibration)
            ).apply {
                description = "Service agent Wazuh SIEM"
                // Pas de badge sur l'icône de l'app
                setShowBadge(false)
            }
            
            // Enregistrement du canal auprès du système
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Crée une notification pour le service foreground
     * 
     * Cette notification informe l'utilisateur que l'agent Wazuh est actif
     * et affiche l'état actuel de la connexion.
     * 
     * @param status Texte d'état à afficher (ex: "Connecté", "Déconnecté")
     * @return Notification configurée pour le service foreground
     */
    private fun createNotification(status: String = "Actif"): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Agent Wazuh")
            .setContentText("Monitoring SIEM - $status")
            // Icône système (à remplacer par une icône personnalisée en production)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            // Priorité basse pour ne pas déranger l'utilisateur
            .setPriority(NotificationCompat.PRIORITY_LOW)
            // Notification persistante (ne peut pas être balayée)
            .setOngoing(true)
            .build()
    }
    
    /**
     * Met à jour le texte de la notification avec un nouveau statut
     * 
     * @param status Nouveau statut à afficher
     */
    private fun updateNotification(status: String) {
        // Vérifier la permission de notification pour Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                == PackageManager.PERMISSION_GRANTED) {
                val notificationManager = getSystemService(NotificationManager::class.java)
                notificationManager.notify(NOTIFICATION_ID, createNotification(status))
            }
        } else {
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.notify(NOTIFICATION_ID, createNotification(status))
        }
    }
    
    /**
     * Démarre l'agent Wazuh et établit la connexion
     * 
     * Cette méthode:
     * 1. Tente de se connecter au serveur Wazuh
     * 2. Lance les tâches périodiques si la connexion réussit
     * 3. Programme une nouvelle tentative si la connexion échoue
     */
    private fun startWazuhAgent() {
        // Lancement dans une coroutine pour éviter de bloquer le thread principal
        serviceScope.launch {
            try {
                Log.d(TAG, "Attempting to connect to Wazuh server...")
                updateNotification("Connexion...")
                
                // Tentative de connexion via le repository
                // TODO: Réimplémenter quand WazuhRepository sera disponible
                val connected = false // Temporaire
                if (connected) {
                    Log.i(TAG, "Successfully connected to Wazuh server")
                    updateNotification("Connecté")
                    
                    // Enregistrement de l'événement de démarrage
                    Log.d(TAG, "Would send to Wazuh: Wazuh agent service started successfully")
                    
                    // Démarrage des tâches périodiques
                    startHeartbeat()
                    startPeriodicSync()
                } else {
                    Log.e(TAG, "Failed to connect to Wazuh server")
                    updateNotification("Déconnecté")
                    
                    // Nouvelle tentative après délai
                    delay(30000) // 30 secondes
                    startWazuhAgent()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error starting Wazuh agent: ${e.message}", e)
                updateNotification("Erreur")
            }
        }
    }
    
    /**
     * Démarre la tâche de heartbeat périodique
     * 
     * Le heartbeat informe le serveur Wazuh que l'agent mobile est toujours actif.
     * Il est envoyé toutes les 30 secondes et permet de détecter les déconnexions.
     */
    private fun startHeartbeat() {
        // Annulation du job précédent s'il existe
        heartbeatJob?.cancel()
        heartbeatJob = serviceScope.launch {
            // Boucle infinie pour le heartbeat périodique
            while (true) {
                try {
                    // Attente de l'intervalle configuré
                    delay(HEARTBEAT_INTERVAL)
                    
                    // Envoi du heartbeat via le repository
                    // TODO: Réimplémenter quand WazuhRepository sera disponible
                    val success = false // Temporaire
                    if (success) {
                        Log.d(TAG, "Heartbeat sent successfully")
                        updateNotification("Connecté")
                    } else {
                        Log.w(TAG, "Heartbeat failed")
                        updateNotification("Reconnexion...")
                        
                        // Tentative de reconnexion si le heartbeat échoue
                        // TODO: Réimplémenter quand WazuhRepository sera disponible
                        val reconnected = false // Temporaire
                        if (!reconnected) {
                            updateNotification("Déconnecté")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Heartbeat error: ${e.message}", e)
                    updateNotification("Erreur")
                }
            }
        }
    }
    
    /**
     * Démarre la synchronisation périodique des logs
     * 
     * Cette tâche vérifie régulièrement s'il y a des logs non envoyés
     * dans la base de données locale et tente de les transmettre au serveur.
     * Utile pour rattraper les logs accumulés pendant une déconnexion.
     */
    private fun startPeriodicSync() {
        // Annulation du job précédent s'il existe
        syncJob?.cancel()
        syncJob = serviceScope.launch {
            // Boucle infinie pour la synchronisation périodique
            while (true) {
                try {
                    // Attente de l'intervalle configuré
                    delay(SYNC_INTERVAL)
                    
                    // Synchronisation seulement si connecté
                    // TODO: Réimplémenter quand WazuhRepository sera disponible
                    if (false) { // Temporaire
                        val synced = false // Temporaire
                        if (synced) {
                            Log.d(TAG, "Logs synchronized successfully")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Sync error: ${e.message}", e)
                }
            }
        }
    }
    
    /**
     * Méthode appelée lors de la destruction du service
     * 
     * Nettoie proprement toutes les ressources:
     * - Annule les tâches périodiques
     * - Ferme la connexion Wazuh
     * - Enregistre l'événement d'arrêt
     */
    override fun onDestroy() {
        Log.d(TAG, "WazuhAgentService stopping...")
        
        // Enregistrement de l'événement d'arrêt
        serviceScope.launch {
            // TODO: Réimplémenter quand WazuhRepository sera disponible
            Log.d(TAG, "Would send to Wazuh: Wazuh agent service stopping")
        }
        
        // Annulation de toutes les tâches périodiques
        heartbeatJob?.cancel()
        syncJob?.cancel()
        // Fermeture de la connexion Wazuh
        // TODO: Réimplémenter quand WazuhRepository sera disponible
        Log.d(TAG, "Would disconnect from Wazuh")
        
        super.onDestroy()
        Log.d(TAG, "WazuhAgentService stopped")
    }
}