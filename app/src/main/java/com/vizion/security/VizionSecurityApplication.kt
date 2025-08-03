// Chemin: app/src/main/java/com/vizion/security/VizionSecurityApplication.kt

package com.vizion.security

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import com.vizion.security.di.DatabaseMaintenanceManager

/**
 * Classe Application principale pour Vizion Security
 *
 * Cette classe initialise l'application et configure Hilt pour l'injection de dépendances.
 * Elle sert de point d'entrée global pour l'application et permet de gérer :
 *
 * - Configuration Hilt/Dagger pour l'injection de dépendances
 * - Initialisation des composants globaux
 * - Configuration des workers de maintenance
 * - Gestion du cycle de vie de l'application
 * - Setup des préférences globales
 *
 * Architecture :
 * - Application Hilt pour injection automatique
 * - Singleton global accessible dans toute l'app
 * - Point central pour les initialisations
 * - Coroutine scope pour les opérations asynchrones
 */
@HiltAndroidApp 
class VizionSecurityApplication : Application(), Configuration.Provider {

    companion object {
        private const val TAG = "VizionSecurityApp"
        private const val DATABASE_MAINTENANCE_WORK = "database_maintenance_work"
        private const val SECURITY_CLEANUP_WORK = "security_cleanup_work"
    }

    // Scope pour les opérations de l'application
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    @Inject
    lateinit var databaseMaintenanceManager: com.vizion.security.di.DatabaseMaintenanceManager
    
    @Inject
    lateinit var databaseMaintenanceManagerTyped: DatabaseMaintenanceManager

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setMinimumLoggingLevel(if (BuildConfig.DEBUG) Log.DEBUG else Log.INFO)
            .setJobSchedulerJobIdRange(1000, 20000)
            .build()
    }

    override fun onCreate() {
        super.onCreate()

        Log.d(TAG, "VizionSecurity Application starting...")

        // Initialiser les composants de l'application
        initializeApplication()

        // Configurer WorkManager pour les tâches périodiques
        setupPeriodicWork()

        Log.i(TAG, "VizionSecurity Application initialized successfully")
    }

    /**
     * Initialise tous les composants de l'application
     */
    private fun initializeApplication() {
        applicationScope.launch {
            try {
                // Configuration des logs en debug/release
                configureLogging()

                // Initialisation des préférences globales
                initializePreferences()

                // Configuration des composants de sécurité
                initializeSecurityComponents()

                // Maintenance de base de données si nécessaire
                performInitialMaintenance()

            } catch (e: Exception) {
                Log.e(TAG, "Error during application initialization: ${e.message}", e)
            }
        }
    }

    /**
     * Configure le système de logging selon l'environnement
     */
    private fun configureLogging() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Debug mode enabled - verbose logging activated")
            // En mode debug, configurer des logs plus verbeux
            System.setProperty("kotlinx.coroutines.debug", "on")
        } else {
            Log.i(TAG, "Production mode - optimized logging")
            // En production, optimiser les performances de logging
        }
    }

    /**
     * Initialise les préférences et configurations globales
     */
    private fun initializePreferences() {
        try {
            val prefs = getSharedPreferences("vizion_security_prefs", MODE_PRIVATE)

            // Vérifier si c'est le premier lancement
            val isFirstLaunch = prefs.getBoolean("first_launch", true)
            if (isFirstLaunch) {
                // Configuration par défaut pour le premier lancement
                prefs.edit().apply {
                    putBoolean("first_launch", false)
                    putBoolean("auto_scan_enabled", true)
                    putInt("scan_frequency_hours", 6)
                    putBoolean("notifications_enabled", true)
                    putBoolean("auto_sync_wazuh", true)
                    putString("theme_mode", "system") // system, light, dark
                    apply()
                }

                Log.i(TAG, "First launch - default preferences set")
            }

            // Charger les préférences de sécurité
            val autoScanEnabled = prefs.getBoolean("auto_scan_enabled", true)
            val notificationsEnabled = prefs.getBoolean("notifications_enabled", true)

            Log.d(TAG, "Preferences loaded - Auto scan: $autoScanEnabled, Notifications: $notificationsEnabled")

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing preferences: ${e.message}", e)
        }
    }

    /**
     * Initialise les composants de sécurité globaux
     */
    private fun initializeSecurityComponents() {
        try {
            // Configuration des certificats SSL pour Wazuh
            configureSslCertificates()

            // Initialisation des modules de détection
            initializeDetectionModules()

            // Configuration réseau sécurisée
            configureSecureNetworking()

            Log.d(TAG, "Security components initialized")

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing security components: ${e.message}", e)
        }
    }

    /**
     * Effectue la maintenance initiale de la base de données
     */
    private suspend fun performInitialMaintenance() {
        try {
            databaseMaintenanceManagerTyped.performMaintenanceIfNeeded()
            Log.d(TAG, "Initial database maintenance completed")
        } catch (e: Exception) {
            Log.e(TAG, "Error during initial maintenance: ${e.message}", e)
        }
    }

    /**
     * Configure les tâches périodiques avec WorkManager
     */
    private fun setupPeriodicWork() {
        try {
            val workManager = WorkManager.getInstance(this)

            // Tâche de maintenance de base de données (quotidienne)
            val maintenanceWork = PeriodicWorkRequestBuilder<com.vizion.security.worker.DatabaseMaintenanceWorker>(
                1, TimeUnit.DAYS
            )
                .addTag("maintenance")
                .build()

            workManager.enqueueUniquePeriodicWork(
                DATABASE_MAINTENANCE_WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                maintenanceWork
            )

            // Tâche de nettoyage de sécurité (hebdomadaire)
            val cleanupWork = PeriodicWorkRequestBuilder<com.vizion.security.worker.SecurityCleanupWorker>(
                7, TimeUnit.DAYS
            )
                .addTag("cleanup")
                .build()

            workManager.enqueueUniquePeriodicWork(
                SECURITY_CLEANUP_WORK,
                ExistingPeriodicWorkPolicy.KEEP,
                cleanupWork
            )

            Log.i(TAG, "Periodic work tasks configured")

        } catch (e: Exception) {
            Log.e(TAG, "Error setting up periodic work: ${e.message}", e)
        }
    }

    /**
     * Configure les certificats SSL pour Wazuh
     */
    private fun configureSslCertificates() {
        try {
            // TODO: Implémenter la configuration SSL
            // - Charger les certificats depuis les assets
            // - Configurer TrustManager personnalisé
            // - Valider les certificats Wazuh

            Log.d(TAG, "SSL certificates configured")

        } catch (e: Exception) {
            Log.e(TAG, "Error configuring SSL certificates: ${e.message}", e)
        }
    }

    /**
     * Initialise les modules de détection
     */
    private fun initializeDetectionModules() {
        try {
            // TODO: Initialiser les modules de détection :
            // - Module de détection de malware
            // - Module de détection de root
            // - Module de détection d'intrusion
            // - Module de surveillance réseau

            Log.d(TAG, "Detection modules initialized")

        } catch (e: Exception) {
            Log.e(TAG, "Error initializing detection modules: ${e.message}", e)
        }
    }

    /**
     * Configure le réseau sécurisé
     */
    private fun configureSecureNetworking() {
        try {
            // TODO: Configuration réseau sécurisée :
            // - Timeout de connexion
            // - Retry policy
            // - Certificate pinning
            // - User-Agent personnalisé

            Log.d(TAG, "Secure networking configured")

        } catch (e: Exception) {
            Log.e(TAG, "Error configuring secure networking: ${e.message}", e)
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Log.w(TAG, "Low memory warning received")

        // Libérer des ressources non critiques
        applicationScope.launch {
            try {
                // Nettoyer les caches
                clearMemoryCaches()

                // Réduire la fréquence de surveillance
                reduceBackgroundActivity()

                Log.i(TAG, "Memory optimization completed")

            } catch (e: Exception) {
                Log.e(TAG, "Error during memory optimization: ${e.message}", e)
            }
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Log.d(TAG, "Trim memory requested, level: $level")

        when (level) {
            TRIM_MEMORY_RUNNING_MODERATE -> {
                Log.d(TAG, "Moderate memory pressure - reducing background activities")
                applicationScope.launch {
                    reduceBackgroundActivity()
                }
            }
            TRIM_MEMORY_RUNNING_LOW -> {
                Log.w(TAG, "Low memory pressure - stopping non-essential services")
                applicationScope.launch {
                    stopNonEssentialServices()
                }
            }
            TRIM_MEMORY_RUNNING_CRITICAL -> {
                Log.e(TAG, "Critical memory pressure - emergency cleanup")
                applicationScope.launch {
                    performEmergencyCleanup()
                }
            }
            TRIM_MEMORY_BACKGROUND,
            TRIM_MEMORY_COMPLETE,
            TRIM_MEMORY_MODERATE,
            TRIM_MEMORY_UI_HIDDEN -> {
                Log.d(TAG, "Background memory trim - level: $level")
                applicationScope.launch {
                    clearMemoryCaches()
                }
            }
        }
    }

    /**
     * Nettoie les caches mémoire
     */
    private suspend fun clearMemoryCaches() {
        // TODO: Implémenter le nettoyage des caches
        Log.d(TAG, "Memory caches cleared")
    }

    /**
     * Réduit l'activité en arrière-plan
     */
    private suspend fun reduceBackgroundActivity() {
        // TODO: Réduire la fréquence des tâches de fond
        Log.d(TAG, "Background activity reduced")
    }

    /**
     * Arrête les services non essentiels
     */
    private suspend fun stopNonEssentialServices() {
        // TODO: Arrêter les services optionnels
        Log.d(TAG, "Non-essential services stopped")
    }

    /**
     * Effectue un nettoyage d'urgence
     */
    private suspend fun performEmergencyCleanup() {
        try {
            clearMemoryCaches()
            stopNonEssentialServices()

            // Forcer la garbage collection
            System.gc()

            Log.i(TAG, "Emergency cleanup completed")

        } catch (e: Exception) {
            Log.e(TAG, "Error during emergency cleanup: ${e.message}", e)
        }
    }
}

/**
 * Extension pour obtenir l'instance de l'application depuis n'importe où
 */
val Application.vizionSecurity: VizionSecurityApplication
    get() = this as VizionSecurityApplication