package com.example.apptest

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Insert
    suspend fun insert(history: ClassificationHistory)

    @Query("SELECT * FROM classification_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<ClassificationHistory>>

    @Query("SELECT * FROM classification_history ORDER BY timestamp DESC")
    suspend fun getAllHistoryOnce(): List<ClassificationHistory>

    @Delete
    suspend fun delete(history: ClassificationHistory)

    @Query("DELETE FROM classification_history")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM classification_history")
    suspend fun getCount(): Int

}