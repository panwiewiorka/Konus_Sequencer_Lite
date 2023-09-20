package com.example.mysequencer01ui.data

import androidx.room.*

@Dao
interface SeqDao {
    /** Patterns **/
    @Insert
    suspend fun saveNoteToPattern(patterns: Patterns)

    @Query("SELECT * from Patterns WHERE pattern = :pattern AND channel = :channel AND noteIndex = :index")
    suspend fun loadNoteFromPattern(pattern: Int, channel: Int, index: Int): Patterns

    @Query("DELETE FROM Patterns WHERE pattern = :pattern")
    suspend fun deletePattern(pattern: Int)

    @Query("SELECT MAX(noteIndex) FROM Patterns WHERE pattern = :pattern AND channel = :channel")
    suspend fun getLastIndex(pattern: Int, channel: Int): Int?

    /** Settings **/
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun populateSettings(settings: Settings)

    @Upsert
    suspend fun saveSettings(settings: Settings)

    @Query("SELECT * from Settings WHERE id = 1")
    fun loadSettings(): Settings

    /** PadPitch **/
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun populatePadPitch(padPitch: PadPitches)

    @Upsert
    suspend fun savePadPitch(padPitch: PadPitches)

    @Query("SELECT pitch from PadPitches WHERE id = :channel")
    fun loadPadPitch(channel: Int): Int
}