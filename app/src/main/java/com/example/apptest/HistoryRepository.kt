package com.example.apptest

import kotlinx.coroutines.flow.Flow

class HistoryRepository(private val historyDao: HistoryDao) {
    val allHistory: Flow<List<ClassificationHistory>> = historyDao.getAllHistory()

    suspend fun insert(history: ClassificationHistory) {
        historyDao.insert(history)
    }

    suspend fun delete(history: ClassificationHistory) {
        historyDao.delete(history)
    }

    suspend fun deleteAll() {
        historyDao.deleteAll()
    }

    suspend fun getCount(): Int {
        return historyDao.getCount()
    }
}