package com.example.apptest

data class NutritionData(
    val displayName: String,
    val servingSize: ServingSize,
    val nutritionPer100g: NutritionValues
)

data class ServingSize(
    val amount: Int,
    val unit: String,
    val description: String
)

data class NutritionValues(
    val calories: Double,
    val protein: Double,
    val fat: Double,
    val carbs: Double,
    val fiber: Double,
    val sugar: Double,
    val vitaminC: Double
)

data class NutritionDisplay(
    val displayName: String,
    val servingDescription: String,
    val per100g: NutritionValues,
    val perServing: NutritionValues
) {
    companion object {
        fun fromNutritionData(data: NutritionData): NutritionDisplay {
            val servingSizeInGrams = data.servingSize.amount
            val multiplier = servingSizeInGrams / 100.0

            val perServing = NutritionValues(
                calories = data.nutritionPer100g.calories * multiplier,
                protein = data.nutritionPer100g.protein * multiplier,
                fat = data.nutritionPer100g.fat * multiplier,
                carbs = data.nutritionPer100g.carbs * multiplier,
                fiber = data.nutritionPer100g.fiber * multiplier,
                sugar = data.nutritionPer100g.sugar * multiplier,
                vitaminC = data.nutritionPer100g.vitaminC * multiplier
            )

            return NutritionDisplay(
                displayName = data.displayName,
                servingDescription = data.servingSize.description,
                per100g = data.nutritionPer100g,
                perServing = perServing
            )
        }
    }
}