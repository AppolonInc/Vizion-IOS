// Chemin: app/src/main/java/com/vizion/security/data/local/entity/WazuhLogEntity.kt

package com.vizion.security.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "wazuh_logs")
data class WazuhLogEntity(
    @PrimaryKey
    val id: Long = 0,

    @ColumnInfo(name = "timestamp")
    val timestamp: Date,

    @ColumnInfo(name = "level")
    val level: String,

    @ColumnInfo(name = "message")
    val message: String,
    
    @ColumnInfo(name = "source")
    val source: String,

    @ColumnInfo(name = "agent_id")
    val agentId: String? = null,
    
    @ColumnInfo(name = "is_sent")
    val isSent: Boolean = false
)