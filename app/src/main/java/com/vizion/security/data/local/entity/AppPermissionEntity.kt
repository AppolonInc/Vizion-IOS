// Chemin: app/src/main/java/com/vizion/security/data/local/entity/AppPermissionEntity.kt

package com.vizion.security.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_permissions")
data class AppPermissionEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "package_name")
    val packageName: String,

    @ColumnInfo(name = "permission_name")
    val permissionName: String,

    @ColumnInfo(name = "is_granted")
    val isGranted: Boolean,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long
)