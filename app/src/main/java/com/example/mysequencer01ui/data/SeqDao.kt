package com.example.mysequencer01ui.data

import androidx.room.*

@Dao
interface SeqDao {
//    @Insert(onConflict = OnConflictStrategy.IGNORE)
//    suspend fun populateSettings(patterns: Patterns)

    @Insert
    suspend fun savePattern(patterns: Patterns)

    @Query("SELECT * from Patterns WHERE pattern = :pattern AND channel = :channel AND time > :advanceTime")
    suspend fun loadPattern(pattern: Int, channel: Int, advanceTime: Int): Patterns

    @Query("DELETE FROM Patterns WHERE pattern = :pattern")
    suspend fun deletePattern(pattern: Int)

    @Query("SELECT COUNT(*) FROM Patterns WHERE pattern = :pattern AND channel = :channel")
    suspend fun countNotes(pattern: Int, channel: Int): Int
}