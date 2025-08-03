// Chemin: app/src/main/java/com/vizion/security/worker/SyncWorker.kt

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
 * Worker pour la synchronisation périodique avec Wazuh
 *
 * Ce worker s'exécute toutes les heures pour :
 * - Synchroniser les événements non synchronisés avec Wazuh
 * - Vérifier la connectivité Wazuh
 * - Traiter la queue de synchronisation
 * - Gérer les retry en cas d'échec
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val securityRepository: SecurityRepository,
    private val wazuhRepository: WazuhRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        private const val TAG = "SyncWorker"
        const val WORK_NAME = "sync_work"
    }

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting periodic Wazuh synchronization...")

            // Vérifier la connectivité Wazuh
            val isConnected = wazuhRepository.isConnected()
            if (!isConnected) {
                Log.w(TAG, "Wazuh not connected, attempting reconnection...")
                val reconnected = wazuhRepository.connectToWazuh()
                if (!reconnected) {
                    Log.e(TAG, "Failed to reconnect to Wazuh, will retry later")
                    return Result.retry()
                }
            }

            // Synchroniser les événements en attente
            val syncResult = securityRepository.syncPendingEventsWithWazuh()

            if (syncResult.isSuccess) {
                val syncedCount = syncResult.getOrNull() ?: 0
                Log.i(TAG, "Wazuh synchronization completed successfully - Synced $syncedCount events")
                Result.success()
            } else {
                Log.e(TAG, "Wazuh synchronization failed: ${syncResult.exceptionOrNull()?.message}")
                Result.retry()
            }

        } catch (e: Exception) {
            Log.e(TAG, "Sync worker failed: ${e.message}", e)
            Result.retry()
        }
    }
}