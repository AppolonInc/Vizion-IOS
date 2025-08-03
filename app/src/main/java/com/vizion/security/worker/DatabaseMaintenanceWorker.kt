// Chemin: app/src/main/java/com/vizion/security/worker/DatabaseMaintenanceWorker.kt

package com.vizion.security.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import com.vizion.security.di.DatabaseMaintenanceManager

/**
 * Worker pour la maintenance périodique de la base de données
 *
 * Ce worker s'exécute quotidiennement pour :
 * - Nettoyer les anciens événements
 * - Optimiser les index de la base de données
 * - Compacter la base de données
 * - Générer des statistiques de maintenance
 */
@HiltWorker
class DatabaseMaintenanceWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val maintenanceManager: DatabaseMaintenanceManager
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "DatabaseMaintenanceWorker"
        const val WORK_NAME = "database_maintenance_work"
    }

    override suspend fun doWork(): Result {
        return try {
            Log.i(TAG, "Starting periodic database maintenance")

            // Effectuer la maintenance forcée
            maintenanceManager.forceMaintenance()

            // Obtenir les statistiques post-maintenance
            val stats = maintenanceManager.getDatabaseStats()
            Log.i(TAG, "Database maintenance completed successfully - Size: ${stats.databaseSizeFormatted}, Events: ${stats.totalEvents}")

            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Database maintenance failed: ${e.message}", e)
            // Retry jusqu'à 3 fois avant d'abandonner
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Log.e(TAG, "Database maintenance failed after 3 attempts, giving up")
                Result.failure()
            }
        }
    }
}
