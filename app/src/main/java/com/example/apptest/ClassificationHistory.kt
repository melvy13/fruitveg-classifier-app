package com.example.apptest

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "classification_history")
data class ClassificationHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val displayName: String,
    val confidence: Float,
    val imagePath: String,
    val timestamp: Long,

    val calories: Double,
    val protein: Double,
    val fat: Double,
    val carbs: Double,
    val fiber: Double,
    val sugar: Double,
    val vitaminC: Double,
    val servingDescription: String
)
