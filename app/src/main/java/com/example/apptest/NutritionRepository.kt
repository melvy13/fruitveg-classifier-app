package com.example.apptest

import android.content.Context
import org.json.JSONObject

class NutritionRepository(private val context: Context) {
    private val nutritionDataMap = mutableMapOf<String, NutritionData>()

    init {
        loadNutritionData()
    }

    private fun loadNutritionData() {
        try {
            // Read JSON file
            val jsonString = context.assets.open("nutrition_data.json")
                .bufferedReader()
                .use { it.readText() }

            // Parse JSON
            val jsonObject = JSONObject(jsonString)

            // Iterate through each fruit/veg
            jsonObject.keys().forEach { key ->
                val itemJson = jsonObject.getJSONObject(key)

                val servingSizeJson = itemJson.getJSONObject("servingSize")
                val nutritionJson = itemJson.getJSONObject("nutritionPer100g")

                val nutritionData = NutritionData(
                    displayName = itemJson.getString("displayName"),
                    servingSize = ServingSize(
                        amount = servingSizeJson.getInt("amount"),
                        unit = servingSizeJson.getString("unit"),
                        description = servingSizeJson.getString("description")
                    ),
                    nutritionPer100g = NutritionValues(
                        calories = nutritionJson.getDouble("calories"),
                        protein = nutritionJson.getDouble("protein"),
                        fat = nutritionJson.getDouble("fat"),
                        carbs = nutritionJson.getDouble("carbs"),
                        fiber = nutritionJson.getDouble("fiber"),
                        sugar = nutritionJson.getDouble("sugar"),
                        vitaminC = nutritionJson.getDouble("vitaminC")
                    )
                )

                nutritionDataMap[key] = nutritionData
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getNutritionData(label: String): NutritionData? {
        return nutritionDataMap[label]
    }

    fun getNutritionDisplay(label: String): NutritionDisplay? {
        val data = getNutritionData(label) ?: return null
        return NutritionDisplay.fromNutritionData(data)
    }
}