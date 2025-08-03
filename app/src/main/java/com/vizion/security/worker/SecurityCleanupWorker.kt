
// Chemin: app/src/main/java/com/vizion/security/worker/SecurityCleanupWorker.kt

package com.vizion.security.worker

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import com.vizion.security.data.repository.SecurityRepository

/**
 * Worker pour le nettoyage périodique des données de sécurité
 *
 * Ce worker s'exécute hebdomadairement pour :
 * - Nettoyer les anciens événements de sécurité
 * - Supprimer les événements résolus anciens
 * - Éliminer les doublons
 * - Optimiser la base de données
 */
@HiltWorker
class SecurityCleanupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val securityRepository: SecurityRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "SecurityCleanupWorker"
        const val WORK_NAME = "security_cleanup_work"
    }

    override suspend fun doWork(): Result {
        return try {
            Log.i(TAG, "Starting periodic security cleanup")

            // Nettoyer les anciens événements (90 jours)
            val cleanedOldEvents = securityRepository.cleanupOldEvents(90)

            // Nettoyer les événements résolus (30 jours)
            val cleanedResolvedEvents = securityRepository.cleanupResolvedEvents(30)

            // Supprimer les doublons
            val removedDuplicates = securityRepository.removeDuplicateEvents()

            // Optimiser la base de données
            val optimizationResult = securityRepository.optimizeDatabase()

            Log.i(TAG, "Security cleanup completed: $cleanedOldEvents old events, $cleanedResolvedEvents resolved events, $removedDuplicates duplicates removed")

            if (optimizationResult.isSuccess) {
                Log.i(TAG, "Database optimization successful: ${optimizationResult.getOrNull()}")
                Result.success()
            } else {
                Log.w(TAG, "Database optimization had issues: ${optimizationResult.exceptionOrNull()?.message}")
                // Continuer même si l'optimisation échoue
                Result.success()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Security cleanup failed: ${e.message}", e)
            // Retry jusqu'à 2 fois avant d'abandonner
            if (runAttemptCount < 2) {
                Result.retry()
            } else {
                Log.e(TAG, "Security cleanup failed after 2 attempts, giving up")
                Result.failure()
            }
        }
    }
}
