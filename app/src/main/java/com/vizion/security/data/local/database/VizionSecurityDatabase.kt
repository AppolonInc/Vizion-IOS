// Chemin: app/src/main/java/com/vizion/security/data/local/database/VizionSecurityDatabase.kt

package com.vizion.security.data.local.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.vizion.security.data.local.entity.SecurityEventEntity
import com.vizion.security.data.local.dao.SecurityEventDao
import com.vizion.security.data.local.entity.WazuhLogEntity
import com.vizion.security.data.local.entity.AppPermissionEntity
import com.vizion.security.data.local.dao.WazuhLogDao
import com.vizion.security.data.local.dao.AppPermissionDao
import com.vizion.security.data.local.converter.DateConverter

/**
 * Base de données Room principale pour Vizion Security
 *
 * Cette base de données centralise toutes les données de sécurité locales
 * avec support pour les migrations, l'optimisation des performances
 * et la cohérence des données.
 *
 * Architecture :
 * - Room Database avec entities et DAOs
 * - Migrations versionnées pour la compatibilité
 * - Index optimisés pour les requêtes fréquentes
 * - Configuration singleton thread-safe
 * - Support backup et restauration
 */
@Database(
    entities = [
        SecurityEventEntity::class,
        WazuhLogEntity::class,
        AppPermissionEntity::class
    ],
    version = 1,
    exportSchema = false // Désactiver l'export du schéma pour éviter l'erreur
)
@TypeConverters(DateConverter::class)
abstract class VizionSecurityDatabase : RoomDatabase() {

    /**
     * DAO pour les événements de sécurité
     */
    abstract fun securityEventDao(): SecurityEventDao
    
    /**
     * DAO pour les logs Wazuh
     */
    abstract fun wazuhLogDao(): WazuhLogDao
    
    /**
     * DAO pour les permissions d'applications
     */
    abstract fun appPermissionDao(): AppPermissionDao

    companion object {
        private const val DATABASE_NAME = "vizion_security_database"

        /**
         * Instance singleton de la base de données
         * Pas nécessaire avec Hilt, mais utile pour référence
         */
        @Volatile
        private var INSTANCE: VizionSecurityDatabase? = null

        /**
         * Crée ou récupère l'instance de la base de données
         * Cette méthode n'est pas utilisée avec Hilt mais reste disponible
         */
        fun getDatabase(context: Context): VizionSecurityDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VizionSecurityDatabase::class.java,
                    DATABASE_NAME
                )
                    .addCallback(DatabaseCallback())
                    .addMigrations(MIGRATION_1_2) // Exemple de migration future
                    .build()

                INSTANCE = instance
                instance
            }
        }

        /**
         * Migration exemple pour versions futures
         * Actuellement pas nécessaire car version 1
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Exemple de migration pour version 2
                // db.execSQL("ALTER TABLE security_events ADD COLUMN new_field TEXT")
            }
        }

        /**
         * Migration 2 vers 3 - Exemple pour ajout d'index
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Ajout d'index pour optimiser les performances
                db.execSQL("CREATE INDEX IF NOT EXISTS index_security_events_correlation_id ON security_events(correlation_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_security_events_risk_score ON security_events(risk_score)")
            }
        }

        /**
         * Callback pour les opérations de base de données
         */
        private class DatabaseCallback : RoomDatabase.Callback() {

            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)

                // Configuration initiale de la base de données
                db.execSQL("""
                    CREATE TRIGGER IF NOT EXISTS update_security_events_timestamp 
                    AFTER UPDATE ON security_events 
                    BEGIN 
                        UPDATE security_events SET updated_at = strftime('%s', 'now') * 1000 
                        WHERE id = NEW.id;
                    END
                """.trimIndent())

                // Index supplémentaires pour optimisation
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_security_events_timestamp_severity ON security_events(timestamp, severity)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_security_events_source_type ON security_events(source, event_type)")

                // Vue pour les statistiques rapides
                db.execSQL("""
                    CREATE VIEW IF NOT EXISTS security_stats_view AS
                    SELECT 
                        severity,
                        COUNT(*) as event_count,
                        AVG(risk_score) as avg_risk_score,
                        MAX(timestamp) as last_event_time,
                        COUNT(CASE WHEN is_resolved = 1 THEN 1 END) as resolved_count
                    FROM security_events 
                    GROUP BY severity
                """.trimIndent())
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)

                // Configuration à chaque ouverture
                db.execSQL("PRAGMA foreign_keys=ON") // Activer les clés étrangères
                db.execSQL("PRAGMA journal_mode=WAL") // Mode WAL pour les performances
                db.execSQL("PRAGMA synchronous=NORMAL") // Synchronisation normale
                db.execSQL("PRAGMA cache_size=10000") // Cache plus important
                db.execSQL("PRAGMA temp_store=MEMORY") // Stockage temporaire en mémoire
            }
        }
    }
}

/**
 * Configuration personnalisée pour la base de données
 */
object DatabaseConfig {

    /**
     * Constantes de configuration
     */
    const val MAX_EVENTS_TO_KEEP = 10000
    const val CLEANUP_INTERVAL_HOURS = 24
    const val BACKUP_INTERVAL_DAYS = 7

    /**
     * Paramètres de performance
     */
    const val CACHE_SIZE = 10000
    const val BUSY_TIMEOUT_MS = 30000
    const val PAGE_SIZE = 4096

    /**
     * Requêtes de maintenance
     */
    val MAINTENANCE_QUERIES = listOf(
        "VACUUM", // Compacter la base de données
        "REINDEX", // Reconstruire les index
        "ANALYZE", // Mettre à jour les statistiques du query planner
        "PRAGMA optimize" // Optimisation automatique
    )

    /**
     * Index personnalisés pour l'optimisation
     */
    val CUSTOM_INDEXES = listOf(
        "CREATE INDEX IF NOT EXISTS idx_events_compound ON security_events(severity, timestamp, is_resolved)",
        "CREATE INDEX IF NOT EXISTS idx_events_ml ON security_events(confidence_score, risk_score)",
        "CREATE INDEX IF NOT EXISTS idx_events_sync ON security_events(is_synced, sync_timestamp)",
        "CREATE INDEX IF NOT EXISTS idx_events_correlation ON security_events(correlation_id, parent_event_id)"
    )

    /**
     * Vues pour les requêtes complexes
     */
    val CUSTOM_VIEWS = mapOf(
        "recent_critical_events" to """
            CREATE VIEW IF NOT EXISTS recent_critical_events AS
            SELECT * FROM security_events 
            WHERE severity = 'CRITICAL' 
            AND timestamp > (strftime('%s', 'now') - 86400) * 1000
            ORDER BY timestamp DESC
        """.trimIndent(),

        "unsynced_events_summary" to """
            CREATE VIEW IF NOT EXISTS unsynced_events_summary AS
            SELECT 
                source,
                COUNT(*) as unsynced_count,
                MIN(timestamp) as oldest_event,
                MAX(risk_score) as max_risk
            FROM security_events 
            WHERE is_synced = 0
            GROUP BY source
        """.trimIndent(),

        "daily_event_stats" to """
            CREATE VIEW IF NOT EXISTS daily_event_stats AS
            SELECT 
                date(timestamp/1000, 'unixepoch') as event_date,
                severity,
                COUNT(*) as event_count,
                AVG(risk_score) as avg_risk
            FROM security_events 
            WHERE timestamp > (strftime('%s', 'now') - 2592000) * 1000
            GROUP BY event_date, severity
            ORDER BY event_date DESC
        """.trimIndent()
    )

    /**
     * Triggers pour la maintenance automatique
     */
    val CUSTOM_TRIGGERS = listOf(
        """
        CREATE TRIGGER IF NOT EXISTS auto_cleanup_old_events
        AFTER INSERT ON security_events
        WHEN (SELECT COUNT(*) FROM security_events) > $MAX_EVENTS_TO_KEEP
        BEGIN
            DELETE FROM security_events 
            WHERE id IN (
                SELECT id FROM security_events 
                WHERE is_resolved = 1 
                ORDER BY timestamp ASC 
                LIMIT 1000
            );
        END
        """.trimIndent(),

        """
        CREATE TRIGGER IF NOT EXISTS update_correlation_on_insert
        AFTER INSERT ON security_events
        WHEN NEW.correlation_id IS NULL
        BEGIN
            UPDATE security_events 
            SET correlation_id = (
                CASE 
                    WHEN EXISTS (
                        SELECT 1 FROM security_events 
                        WHERE event_type = NEW.event_type 
                        AND source = NEW.source 
                        AND timestamp BETWEEN NEW.timestamp - 900000 AND NEW.timestamp + 900000
                        AND id != NEW.id
                    ) 
                    THEN NEW.event_type || '_' || NEW.source || '_' || (NEW.timestamp / 900000)
                    ELSE NULL
                END
            )
            WHERE id = NEW.id;
        END
        """.trimIndent()
    )
}