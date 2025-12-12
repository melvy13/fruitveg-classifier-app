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
    val servingDescription: String,

    val caloriesPerServing: Double,
    val waterPerServing: Double,
    val proteinPerServing: Double,
    val fatPerServing: Double,
    val totalCarbsPerServing: Double,
    val fiberPerServing: Double,
    val sugarPerServing: Double,
    val vitaminCPerServing: Double,

    val caloriesPer100g: Double,
    val waterPer100g: Double,
    val proteinPer100g: Double,
    val fatPer100g: Double,
    val totalCarbsPer100g: Double,
    val fiberPer100g: Double,
    val sugarPer100g: Double,
    val vitaminCPer100g: Double,

    val topPredictions: String
)
