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
import com.happypuppy.toastmasterstimer.data.repository.DateConverters
import java.util.Date

@Entity(tableName = "speakers")
data class Speaker(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "speech_type") val speechType: String,
    @ColumnInfo(name = "duration") val duration: String,
    @ColumnInfo(name = "created_at") val createdAt: Date = Date()
)

@Dao
interface SpeakerDao {
    @Query("SELECT * FROM speakers ORDER BY created_at DESC")
    suspend fun getAllSpeakers(): List<Speaker>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpeaker(speaker: Speaker)

    @Delete
    suspend fun deleteSpeaker(speaker: Speaker)

    @Query("DELETE FROM speakers")
    suspend fun deleteAllSpeakers()
}

@Database(entities = [Speaker::class], version = 2, exportSchema = false)
@androidx.room.TypeConverters(DateConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun speakerDao(): SpeakerDao
}

class SpeakerRepository(private val speakerDao: SpeakerDao) {
    suspend fun getAllSpeakers() = speakerDao.getAllSpeakers()
    suspend fun insertSpeaker(speaker: Speaker) = speakerDao.insertSpeaker(speaker)
    suspend fun deleteSpeaker(speaker: Speaker) = speakerDao.deleteSpeaker(speaker)
    suspend fun deleteAllSpeakers() = speakerDao.deleteAllSpeakers()
}