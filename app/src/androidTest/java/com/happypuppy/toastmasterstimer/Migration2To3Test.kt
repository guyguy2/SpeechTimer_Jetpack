package com.happypuppy.toastmasterstimer

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.happypuppy.toastmasterstimer.data.database.repository.AppDatabase
import com.happypuppy.toastmasterstimer.data.database.repository.MIGRATION_2_3
import com.happypuppy.toastmasterstimer.data.database.repository.Speaker
import com.happypuppy.toastmasterstimer.data.database.repository.UNKNOWN_SPEECH_TYPE_ID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

@RunWith(AndroidJUnit4::class)
class Migration2To3Test {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    @Test
    fun migrate2To3_backfillsKnownSpeechTypesAndAllowsNewV3Rows() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        context.deleteDatabase(TEST_DB)

        helper.createDatabase(TEST_DB, 2).use { db ->
            val rows = listOf(
                Triple("Alex", "TABLE TOPICS", "01:30"),
                Triple("Blair", "EVALUATION", "02:30"),
                Triple("Casey", "ICE BREAKER", "05:00"),
                Triple("Devon", "SPEECH", "06:15"),
                Triple("Erin", "Custom Speech", "08:45"),
                Triple("Frank", "Legacy Custom", "03:10")
            )
            rows.forEachIndexed { index, (name, speechType, duration) ->
                val values = ContentValues().apply {
                    put("name", name)
                    put("speech_type", speechType)
                    put("duration", duration)
                    put("created_at", 1_000L + index)
                }
                db.insert("speakers", SQLiteDatabase.CONFLICT_ABORT, values)
            }
        }

        val migratedDb = helper.runMigrationsAndValidate(TEST_DB, 3, true, MIGRATION_2_3)
        migratedDb.use { db ->
            db.query(
                "SELECT name, speech_type_id, min_seconds, max_seconds, elapsed_seconds, phase_reached " +
                    "FROM speakers ORDER BY created_at ASC"
            ).use { cursor ->
                val expected = listOf(
                    listOf("Alex", "table_topics", 60, 120, null, null),
                    listOf("Blair", "evaluation", 120, 180, null, null),
                    listOf("Casey", "ice_breaker", 240, 360, null, null),
                    listOf("Devon", "speech", 300, 420, null, null),
                    listOf("Erin", "custom", null, null, null, null),
                    listOf("Frank", UNKNOWN_SPEECH_TYPE_ID, null, null, null, null)
                )
                var index = 0
                while (cursor.moveToNext()) {
                    val row = expected[index++]
                    assertEquals(row[0], cursor.getString(0))
                    assertEquals(row[1], cursor.getString(1))
                    if (row[2] == null) assertEquals(true, cursor.isNull(2)) else assertEquals(row[2], cursor.getInt(2))
                    if (row[3] == null) assertEquals(true, cursor.isNull(3)) else assertEquals(row[3], cursor.getInt(3))
                    assertEquals(true, cursor.isNull(4))
                    assertEquals(true, cursor.isNull(5))
                }
                assertEquals(expected.size, index)
            }
        }

        val appDb = Room.databaseBuilder(context, AppDatabase::class.java, TEST_DB)
            .addMigrations(MIGRATION_2_3)
            .build()
        try {
            val dao = appDb.speakerDao()
            val migratedRows = dao.getAllSpeakers().first()
            assertEquals(6, migratedRows.size)

            val alex = migratedRows.first { it.name == "Alex" }
            assertEquals("table_topics", alex.speechTypeId)
            assertEquals(60, alex.minSeconds)
            assertEquals(120, alex.maxSeconds)
            assertNull(alex.elapsedSeconds)
            assertNull(alex.phaseReached)

            val frank = migratedRows.first { it.name == "Frank" }
            assertEquals(UNKNOWN_SPEECH_TYPE_ID, frank.speechTypeId)
            assertNull(frank.minSeconds)
            assertNull(frank.maxSeconds)

            dao.insertSpeaker(
                Speaker(
                    name = "Grace",
                    speechType = "SPEECH",
                    duration = "06:00",
                    createdAt = Date(2_000L),
                    speechTypeId = "speech",
                    minSeconds = 300,
                    maxSeconds = 420,
                    elapsedSeconds = 360,
                    phaseReached = "YELLOW"
                )
            )
            val allRows = dao.getAllSpeakers().first()
            assertEquals(7, allRows.size)
            val grace = allRows.first { it.name == "Grace" }
            assertEquals("speech", grace.speechTypeId)
            assertEquals(300, grace.minSeconds)
            assertEquals(420, grace.maxSeconds)
            assertEquals(360, grace.elapsedSeconds)
            assertEquals("YELLOW", grace.phaseReached)
        } finally {
            appDb.close()
            context.deleteDatabase(TEST_DB)
        }
    }

    private companion object {
        const val TEST_DB = "migration-test"
    }
}
