// Chemin: app/src/main/java/com/vizion/security/data/local/dao/WazuhLogDao.kt

package com.vizion.security.data.local.dao

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.vizion.security.data.local.entity.WazuhLogEntity
import java.util.Date

@Dao
interface WazuhLogDao {
    @Query("SELECT * FROM wazuh_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<WazuhLogEntity>>
    
    @Query("SELECT * FROM wazuh_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 50): Flow<List<WazuhLogEntity>>
    
    @Query("SELECT * FROM wazuh_logs WHERE is_sent = 0 ORDER BY timestamp ASC")
    suspend fun getUnsentLogs(): List<WazuhLogEntity>

    @Insert
    suspend fun insertLog(log: WazuhLogEntity)
    
    @Update
    suspend fun updateLog(log: WazuhLogEntity)
    
    @Query("UPDATE wazuh_logs SET is_sent = 1 WHERE id IN (:logIds)")
    suspend fun markLogsAsSent(logIds: List<Long>)

    @Delete
    suspend fun deleteLog(log: WazuhLogEntity)
    
    @Query("DELETE FROM wazuh_logs WHERE timestamp < :cutoffDate")
    suspend fun deleteOldLogs(cutoffDate: Date): Int
    
    @Query("SELECT COUNT(*) FROM wazuh_logs WHERE timestamp >= :since")
    suspend fun getLogCountSince(since: Date): Int
}
