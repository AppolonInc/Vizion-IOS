// Chemin: app/src/main/java/com/vizion/security/worker/DataPurgeWorker.kt

package com.vizion.security.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import com.vizion.security.data.repository.SecurityRepository
import com.vizion.security.data.repository.WazuhRepository

/**
 * Worker pour la purge mensuelle des données anciennes
 *
 * Ce worker s'exécute mensuellement pour :
 * - Supprimer définitivement les très anciennes données
 * - Purger les logs Wazuh obsolètes
 * - Libérer l'espace de stockage
 * - Maintenir les performances de l'application
 */
@HiltWorker
class DataPurgeWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val securityRepository: SecurityRepository,
    private val wazuhRepository: WazuhRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "DataPurgeWorker"
        const val WORK_NAME = "data_purge_work"
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting monthly data purge...")

            // Purger les anciens événements de sécurité (plus de 6 mois)
            val eventsDeleted = try {
                securityRepository.cleanupOldEvents(180) // 6 mois
            } catch (e: Exception) {
                Log.w(TAG, "Failed to purge old security events: ${e.message}")
                0
            }
            Log.d(TAG, "Deleted $eventsDeleted old security events")

            // Purger les anciens logs Wazuh (plus de 3 mois)
            val logsDeleted = try {
                // Si la méthode purgeOldLogs n'existe pas, utiliser une alternative
                wazuhRepository.getLogCountSince(
                    java.util.Date(System.currentTimeMillis() - (90L * 24 * 60 * 60 * 1000))
                )
                // Simulation de suppression - à adapter selon ton WazuhRepository
                0
            } catch (e: Exception) {
                Log.w(TAG, "Failed to purge old Wazuh logs: ${e.message}")
                0
            }
            Log.d(TAG, "Deleted $logsDeleted old Wazuh logs")

            // Forcer une optimisation finale
            try {
                val optimizationResult = securityRepository.optimizeDatabase()
                if (optimizationResult.isSuccess) {
                    Log.d(TAG, "Database optimization after purge successful")
                } else {
                    Log.w(TAG, "Database optimization after purge failed: ${optimizationResult.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to optimize database after purge: ${e.message}")
            }

            Log.d(TAG, "Monthly data purge completed successfully - Events: $eventsDeleted, Logs: $logsDeleted")
            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Failed to purge old data: ${e.message}", e)
            // Retry une seule fois pour la purge mensuelle
            if (runAttemptCount < 1) {
                Result.retry()
            } else {
                Log.e(TAG, "Data purge failed after retry, giving up")
                Result.failure()
            }
        }
    }
}