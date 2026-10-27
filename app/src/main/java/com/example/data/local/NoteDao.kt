package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    @Query("SELECT * FROM encrypted_notes WHERE userId = :userId AND isArchived = 0 ORDER BY isPinned DESC, createdAt DESC")
    fun getActiveNotes(userId: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM encrypted_notes WHERE userId = :userId AND isArchived = 1 ORDER BY updatedAt DESC")
    fun getArchivedNotes(userId: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM encrypted_notes WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getNoteById(id: String, userId: String): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteEntity)

    @Update
    suspend fun updateNote(note: NoteEntity)

    @Query("DELETE FROM encrypted_notes WHERE id = :id AND userId = :userId")
    suspend fun deleteNoteById(id: String, userId: String)

    @Query("UPDATE encrypted_notes SET isPinned = :isPinned WHERE id = :id AND userId = :userId")
    suspend fun updatePinStatus(id: String, userId: String, isPinned: Boolean)

    @Query("UPDATE encrypted_notes SET isArchived = :isArchived WHERE id = :id AND userId = :userId")
    suspend fun updateArchiveStatus(id: String, userId: String, isArchived: Boolean)
}
