package com.happypuppy.toastmasterstimer.data.database

import com.happypuppy.toastmasterstimer.data.database.repository.Speaker
import com.happypuppy.toastmasterstimer.data.database.repository.SpeakerDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update

/**
 * In-memory [SpeakerDao] that mimics Room's behaviour: the flow returned by
 * [getAllSpeakers] re-emits after every mutation and rows get auto-generated ids.
 */
class FakeSpeakerDao(initial: List<Speaker> = emptyList()) : SpeakerDao {
    private val speakers = MutableStateFlow(initial)
    private var nextId = (initial.maxOfOrNull { it.id } ?: 0) + 1

    /** When set, [getAllSpeakers] returns a flow that fails with this error. */
    var queryError: Throwable? = null

    /** When set, the next [insertSpeaker] call throws this error once. */
    var insertError: Throwable? = null

    /** When set, the next [updateSpeaker] call throws this error once. */
    var updateError: Throwable? = null

    /** When set, the next [deleteSpeaker] call throws this error once. */
    var deleteError: Throwable? = null

    /** When set, the next [deleteAllSpeakers] call throws this error once. */
    var deleteAllError: Throwable? = null

    val current: List<Speaker> get() = speakers.value

    override fun getAllSpeakers(): Flow<List<Speaker>> {
        val error = queryError
        return if (error != null) flow { throw error } else speakers
    }

    override suspend fun getAllSpeakersOnce(): List<Speaker> {
        queryError?.let { throw it }
        return speakers.value
    }

    override suspend fun insertSpeaker(speaker: Speaker) {
        insertError?.let {
            insertError = null
            throw it
        }
        val stored = if (speaker.id == 0) speaker.copy(id = nextId++) else speaker
        speakers.update { list -> list.filterNot { it.id == stored.id } + stored }
    }

    override suspend fun insertSpeakers(speakers: List<Speaker>) {
        insertError?.let {
            insertError = null
            throw it
        }
        this.speakers.update { list ->
            var updated = list
            for (speaker in speakers) {
                val stored = if (speaker.id == 0) speaker.copy(id = nextId++) else speaker
                updated = updated.filterNot { it.id == stored.id } + stored
            }
            updated
        }
    }

    override suspend fun updateSpeaker(speaker: Speaker) {
        updateError?.let {
            updateError = null
            throw it
        }
        speakers.update { list -> list.map { if (it.id == speaker.id) speaker else it } }
    }

    override suspend fun deleteSpeaker(speaker: Speaker) {
        deleteError?.let {
            deleteError = null
            throw it
        }
        speakers.update { list -> list.filterNot { it.id == speaker.id } }
    }

    override suspend fun deleteAllSpeakers() {
        deleteAllError?.let {
            deleteAllError = null
            throw it
        }
        speakers.value = emptyList()
    }
}
