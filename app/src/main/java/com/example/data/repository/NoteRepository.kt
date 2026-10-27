package com.example.data.repository

import com.example.data.ai.GeminiAiService
import com.example.data.crypto.CryptoHelper
import com.example.data.local.NoteDao
import com.example.data.local.NoteEntity
import com.example.data.model.Note
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray

class NoteRepository(private val noteDao: NoteDao) {

    fun getActiveNotes(userId: String): Flow<List<Note>> {
        return noteDao.getActiveNotes(userId).map { entities ->
            entities.map { entity -> decryptEntity(entity) }
        }
    }

    fun getArchivedNotes(userId: String): Flow<List<Note>> {
        return noteDao.getArchivedNotes(userId).map { entities ->
            entities.map { entity -> decryptEntity(entity) }
        }
    }

    suspend fun getNoteById(id: String, userId: String): Note? {
        val entity = noteDao.getNoteById(id, userId) ?: return null
        return decryptEntity(entity)
    }

    suspend fun saveNote(note: Note, autoAiEnrich: Boolean = true): Note {
        var enrichedSummary = note.aiSummary
        var enrichedTags = note.tags
        var enrichedSentiment = note.sentiment

        if (autoAiEnrich && (enrichedSummary.isNullOrBlank() || enrichedTags.isEmpty())) {
            val aiResult = GeminiAiService.analyzeAndEnrichNote(note.title, note.content)
            enrichedSummary = aiResult.summary
            enrichedTags = if (note.tags.isNotEmpty()) (note.tags + aiResult.tags).distinct() else aiResult.tags
            enrichedSentiment = aiResult.sentiment
        }

        val encryptedTitle = CryptoHelper.encrypt(note.title)
        val encryptedContent = CryptoHelper.encrypt(note.content)

        val tagsJson = JSONArray().apply {
            enrichedTags.forEach { put(it) }
        }.toString()

        val entity = NoteEntity(
            id = note.id,
            userId = note.userId,
            encryptedTitle = encryptedTitle.ciphertext,
            titleIv = encryptedTitle.iv,
            encryptedContent = encryptedContent.ciphertext,
            contentIv = encryptedContent.iv,
            aiSummary = enrichedSummary,
            tagsJson = tagsJson,
            sentiment = enrichedSentiment,
            isPinned = note.isPinned,
            isArchived = note.isArchived,
            colorHex = note.colorHex,
            audioDurationSeconds = note.audioDurationSeconds,
            createdAt = note.createdAt,
            updatedAt = System.currentTimeMillis()
        )

        noteDao.insertNote(entity)

        return note.copy(
            aiSummary = enrichedSummary,
            tags = enrichedTags,
            sentiment = enrichedSentiment,
            updatedAt = entity.updatedAt
        )
    }

    suspend fun deleteNote(id: String, userId: String) {
        noteDao.deleteNoteById(id, userId)
    }

    suspend fun togglePin(id: String, userId: String, currentPinned: Boolean) {
        noteDao.updatePinStatus(id, userId, !currentPinned)
    }

    suspend fun toggleArchive(id: String, userId: String, currentArchived: Boolean) {
        noteDao.updateArchiveStatus(id, userId, !currentArchived)
    }

    private fun decryptEntity(entity: NoteEntity): Note {
        val plainTitle = CryptoHelper.decrypt(entity.encryptedTitle, entity.titleIv)
        val plainContent = CryptoHelper.decrypt(entity.encryptedContent, entity.contentIv)

        val tagsList = mutableListOf<String>()
        try {
            val jsonArr = JSONArray(entity.tagsJson)
            for (i in 0 until jsonArr.length()) {
                tagsList.add(jsonArr.getString(i))
            }
        } catch (e: Exception) {
            // Ignore parse errors
        }

        return Note(
            id = entity.id,
            userId = entity.userId,
            title = plainTitle,
            content = plainContent,
            aiSummary = entity.aiSummary,
            tags = tagsList,
            sentiment = entity.sentiment,
            isPinned = entity.isPinned,
            isArchived = entity.isArchived,
            colorHex = entity.colorHex,
            audioDurationSeconds = entity.audioDurationSeconds,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }
}
