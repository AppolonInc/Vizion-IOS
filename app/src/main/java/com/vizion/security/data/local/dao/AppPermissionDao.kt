// ========================================

// Chemin: app/src/main/java/com/vizion/security/data/local/dao/AppPermissionDao.kt

package com.vizion.security.data.local.dao

import androidx.room.*
import com.vizion.security.data.local.entity.AppPermissionEntity

@Dao
interface AppPermissionDao {
    @Query("SELECT * FROM app_permissions ORDER BY timestamp DESC")
    suspend fun getAllPermissions(): List<AppPermissionEntity>

    @Insert
    suspend fun insertPermission(permission: AppPermissionEntity)

    @Delete
    suspend fun deletePermission(permission: AppPermissionEntity)
}
