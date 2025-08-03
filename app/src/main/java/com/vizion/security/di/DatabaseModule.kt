// Chemin: app/src/main/java/com/vizion/security/di/DatabaseModule.kt

package com.vizion.security.di

import androidx.core.content.edit
import android.content.Context
import androidx.room.Room
import com.vizion.security.data.local.dao.SecurityEventDao
import com.vizion.security.data.local.database.VizionSecurityDatabase
import com.vizion.security.data.local.database.DatabaseConfig
import com.vizion.security.data.local.dao.WazuhLogDao
import com.vizion.security.data.local.dao.AppPermissionDao
import com.vizion.security.data.repository.SecurityRepository
import com.vizion.security.data.repository.SecurityRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideVizionSecurityDatabase(
        @ApplicationContext context: Context
    ): VizionSecurityDatabase {
        return Room.databaseBuilder(
            context,
            VizionSecurityDatabase::class.java,
            "vizion_security_database"
        )
            .apply {
                addCallback(object : androidx.room.RoomDatabase.Callback() {
                    override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        super.onCreate(db)

                        try {
                            DatabaseConfig.CUSTOM_INDEXES.forEach { indexQuery ->
                                try {
                                    db.execSQL(indexQuery)
                                } catch (e: Exception) {
                                    android.util.Log.w("DatabaseModule", "Failed to create index: $indexQuery", e)
                                }
                            }

                            DatabaseConfig.CUSTOM_VIEWS.values.forEach { viewQuery ->
                                try {
                                    db.execSQL(viewQuery)
                                } catch (e: Exception) {
                                    android.util.Log.w("DatabaseModule", "Failed to create view: $viewQuery", e)
                                }
                            }

                            DatabaseConfig.CUSTOM_TRIGGERS.forEach { triggerQuery ->
                                try {
                                    db.execSQL(triggerQuery)
                                } catch (e: Exception) {
                                    android.util.Log.w("DatabaseModule", "Failed to create trigger: $triggerQuery", e)
                                }
                            }

                            android.util.Log.i("DatabaseModule", "VizionSecurity database created with custom optimizations")

                        } catch (e: Exception) {
                            android.util.Log.e("DatabaseModule", "Error during database initialization: ${e.message}", e)
                        }
                    }

                    override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                        super.onOpen(db)

                        try {
                            db.execSQL("PRAGMA foreign_keys=ON")
                            db.execSQL("PRAGMA journal_mode=WAL")
                            db.execSQL("PRAGMA synchronous=NORMAL")
                            db.execSQL("PRAGMA cache_size=${DatabaseConfig.CACHE_SIZE}")
                            db.execSQL("PRAGMA temp_store=MEMORY")
                            db.execSQL("PRAGMA busy_timeout=${DatabaseConfig.BUSY_TIMEOUT_MS}")

                            android.util.Log.d("DatabaseModule", "Database optimizations applied")
                        } catch (e: Exception) {
                            android.util.Log.w("DatabaseModule", "Failed to apply database optimizations", e)
                        }
                    }
                })

                fallbackToDestructiveMigration()
            }
            .build()
    }

    @Provides
    fun provideSecurityEventDao(database: VizionSecurityDatabase): SecurityEventDao {
        return database.securityEventDao()
    }

    @Provides
    fun provideWazuhLogDao(database: VizionSecurityDatabase): WazuhLogDao {
        return database.wazuhLogDao()
    }

    @Provides
    fun provideAppPermissionDao(database: VizionSecurityDatabase): AppPermissionDao {
        return database.appPermissionDao()
    }

    @Provides
    @Singleton
    fun provideSecurityRepository(
        securityEventDao: SecurityEventDao,
        @ApplicationContext context: Context
    ): SecurityRepository {
        return SecurityRepositoryImpl(securityEventDao, context)
    }

    @Provides
    @Singleton
    fun provideDatabaseMaintenanceManager(
        database: VizionSecurityDatabase,
        @ApplicationContext context: Context
    ): DatabaseMaintenanceManager {
        return DatabaseMaintenanceManager(database, context)
    }
}
