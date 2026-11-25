package com.happypuppy.toastmasterstimer.data.database.repository

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.happypuppy.toastmasterstimer.data.repository.DateConverters
import kotlinx.coroutines.flow.Flow
import java.util.Date

const val UNKNOWN_SPEECH_TYPE_ID = "unknown"

@Entity(tableName = "speakers")
data class Speaker(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "speech_type") val speechType: String,
    @ColumnInfo(name = "duration") val duration: String,
    @ColumnInfo(name = "created_at") val createdAt: Date = Date(),
    @ColumnInfo(name = "speech_type_id", defaultValue = "'unknown'") val speechTypeId: String = UNKNOWN_SPEECH_TYPE_ID,
    @ColumnInfo(name = "min_seconds") val minSeconds: Int? = null,
    @ColumnInfo(name = "max_seconds") val maxSeconds: Int? = null,
    @ColumnInfo(name = "elapsed_seconds") val elapsedSeconds: Int? = null,
    @ColumnInfo(name = "phase_reached") val phaseReached: String? = null
)

@Dao
interface SpeakerDao {
    @Query("SELECT * FROM speakers ORDER BY created_at DESC")
    fun getAllSpeakers(): Flow<List<Speaker>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpeaker(speaker: Speaker)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpeakers(speakers: List<Speaker>)

    @Update
    suspend fun updateSpeaker(speaker: Speaker)

    @Delete
    suspend fun deleteSpeaker(speaker: Speaker)

    @Query("DELETE FROM speakers")
    suspend fun deleteAllSpeakers()
}

@Database(entities = [Speaker::class], version = 3, exportSchema = true)
@androidx.room.TypeConverters(DateConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun speakerDao(): SpeakerDao
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE speakers ADD COLUMN speech_type_id TEXT NOT NULL DEFAULT 'unknown'")
        db.execSQL("ALTER TABLE speakers ADD COLUMN min_seconds INTEGER")
        db.execSQL("ALTER TABLE speakers ADD COLUMN max_seconds INTEGER")
        db.execSQL("ALTER TABLE speakers ADD COLUMN elapsed_seconds INTEGER")
        db.execSQL("ALTER TABLE speakers ADD COLUMN phase_reached TEXT")
        db.execSQL(
            """
            UPDATE speakers
            SET speech_type_id = CASE TRIM(speech_type)
                    WHEN 'TABLE TOPICS' THEN 'table_topics'
                    WHEN 'EVALUATION' THEN 'evaluation'
                    WHEN 'ICE BREAKER' THEN 'ice_breaker'
                    WHEN 'SPEECH' THEN 'speech'
                    WHEN 'Custom Speech' THEN 'custom'
                    ELSE 'unknown'
                END,
                min_seconds = CASE TRIM(speech_type)
                    WHEN 'TABLE TOPICS' THEN 60
                    WHEN 'EVALUATION' THEN 120
                    WHEN 'ICE BREAKER' THEN 240
                    WHEN 'SPEECH' THEN 300
                    ELSE NULL
                END,
                max_seconds = CASE TRIM(speech_type)
                    WHEN 'TABLE TOPICS' THEN 120
                    WHEN 'EVALUATION' THEN 180
                    WHEN 'ICE BREAKER' THEN 360
                    WHEN 'SPEECH' THEN 420
                    ELSE NULL
                END
            """.trimIndent()
        )
    }
}

class SpeakerRepository(private val speakerDao: SpeakerDao) {
    fun getAllSpeakers(): Flow<List<Speaker>> = speakerDao.getAllSpeakers()
    suspend fun insertSpeaker(speaker: Speaker) = speakerDao.insertSpeaker(speaker)
    suspend fun insertSpeakers(speakers: List<Speaker>) = speakerDao.insertSpeakers(speakers)
    suspend fun updateSpeaker(speaker: Speaker) = speakerDao.updateSpeaker(speaker)
    suspend fun deleteSpeaker(speaker: Speaker) = speakerDao.deleteSpeaker(speaker)
    suspend fun deleteAllSpeakers() = speakerDao.deleteAllSpeakers()
}