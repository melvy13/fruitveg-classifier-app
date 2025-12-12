package com.example.apptest

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.apptest.ui.theme.FruitVegTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ClassificationActivity : ComponentActivity() {

    private lateinit var classifier: FruitVegClassifier
    private lateinit var nutritionRepository: NutritionRepository
    private lateinit var historyRepository: HistoryRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize classifier & nutrition repo
        classifier = FruitVegClassifier(this)
        nutritionRepository = NutritionRepository(this)

        val database = AppDatabase.getDatabase(this)
        historyRepository = HistoryRepository(database.historyDao())

        // Get image URI from intent
        val imageUriString = intent.getStringExtra("IMAGE_URI")
        val imageUri = imageUriString?.let { Uri.parse(it) }

        setContent {
            FruitVegTheme {
                ClassificationScreen(
                    imageUri = imageUri,
                    onClassify = { uri -> classifyImage(uri) },
                    nutritionRepository = nutritionRepository,
                    onSaveHistory = { result, nutrition, imagePath ->
                        saveToHistory(result, nutrition, imagePath)
                    }
                )
            }
        }
    }

    private suspend fun classifyImage(uri: Uri): ClassificationResult {
        return withContext(Dispatchers.Default) {
            val bitmap = ImagePreprocessor.loadBitmapFromUri(this@ClassificationActivity, uri)
            classifier.classify(bitmap)
        }
    }

    private suspend fun saveToHistory(
        result: ClassificationResult,
        nutrition: NutritionDisplay?,
        imagePath: String
    ) {
        withContext(Dispatchers.IO) {
            if (nutrition != null) {
                val topPredictionsString = result.allProbabilities
                    .sortedByDescending { it.second }
                    .take(5)
                    .joinToString(",") { "${it.first}:${it.second}" }

                val history = ClassificationHistory(
                    label = result.label,
                    displayName = nutrition.displayName,
                    confidence = result.confidence,
                    imagePath = imagePath,
                    timestamp = System.currentTimeMillis(),
                    servingDescription = nutrition.servingDescription,

                    caloriesPerServing = nutrition.perServing.calories,
                    waterPerServing = nutrition.perServing.water,
                    proteinPerServing = nutrition.perServing.protein,
                    fatPerServing = nutrition.perServing.fat,
                    totalCarbsPerServing = nutrition.perServing.totalCarbs,
                    fiberPerServing = nutrition.perServing.fiber,
                    sugarPerServing = nutrition.perServing.sugar,
                    vitaminCPerServing = nutrition.perServing.vitaminC,

                    caloriesPer100g = nutrition.per100g.calories,
                    waterPer100g = nutrition.per100g.water,
                    proteinPer100g = nutrition.per100g.protein,
                    fatPer100g = nutrition.per100g.fat,
                    totalCarbsPer100g = nutrition.per100g.totalCarbs,
                    fiberPer100g = nutrition.per100g.fiber,
                    sugarPer100g = nutrition.per100g.sugar,
                    vitaminCPer100g = nutrition.per100g.vitaminC,

                    topPredictions = topPredictionsString
                )
                historyRepository.insert(history)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        classifier.close()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClassificationScreen(
    imageUri: Uri?,
    onClassify: suspend (Uri) -> ClassificationResult,
    nutritionRepository: NutritionRepository,
    onSaveHistory: suspend (ClassificationResult, NutritionDisplay?, String) -> Unit
) {
    var classificationResult by remember { mutableStateOf<ClassificationResult?>(null) }
    var isClassifying by remember { mutableStateOf(false) }
    var nutritionDisplay by remember { mutableStateOf<NutritionDisplay?>(null) }
    var savedImagePath by remember { mutableStateOf<String?>(null) }
    var showPerServing by remember { mutableStateOf(true) }
    var showPredictions by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? ComponentActivity

    // Automatically classify when screen loads
    LaunchedEffect(imageUri) {
        if (imageUri != null && classificationResult == null) {
            isClassifying = true
            savedImagePath = saveImageToInternalStorage(context, imageUri)
            val result = onClassify(imageUri)
            classificationResult = result

            val startTime = System.nanoTime()
            nutritionDisplay = nutritionRepository.getNutritionDisplay(result.label)
            val endTime = System.nanoTime()
            val inferenceTime = (endTime - startTime) / 1_000_000
            android.util.Log.d("Performance", "Obtaining nutrition data took: $inferenceTime ms")

            if (savedImagePath != null) {
                onSaveHistory(result, nutritionDisplay, savedImagePath!!)
            }
            isClassifying = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Image Classification") },
                navigationIcon = {
                    IconButton(onClick = { activity?.finish() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Display image
                if (savedImagePath != null) {
                    val imageFile = File(savedImagePath!!)
                    if (imageFile.exists()) {
                        AsyncImage(
                            model = imageFile,
                            contentDescription = "Preprocessed image",
                            modifier = Modifier
                                .size(224.dp)
                                .padding(16.dp),
                            contentScale = ContentScale.Fit
                        )
                    }
                } else if (imageUri != null && isClassifying) {
                    Box(
                        modifier = Modifier
                            .size(224.dp)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Show results
                if (isClassifying) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Classifying...")
                } else if (classificationResult != null && nutritionDisplay != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = nutritionDisplay!!.displayName,
                                style = MaterialTheme.typography.headlineSmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Confidence: ${
                                    String.format(
                                        "%.2f",
                                        classificationResult!!.confidence * 100
                                    )
                                }%",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = showPerServing,
                            onClick = { showPerServing = true },
                            label = { Text("Per Serving") },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = !showPerServing,
                            onClick = { showPerServing = false },
                            label = { Text("Per 100g") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    NutritionCard(
                        nutritionDisplay = nutritionDisplay!!,
                        showPerServing = showPerServing
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .clickable { showPredictions = !showPredictions },
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Top 5 Predictions",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Icon(
                                    imageVector = if (showPredictions)
                                        Icons.Default.KeyboardArrowUp
                                    else
                                        Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (showPredictions) "Collapse" else "Expand"
                                )
                            }

                            if (showPredictions) {
                                Spacer(modifier = Modifier.height(16.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(12.dp))
                                classificationResult!!.allProbabilities
                                    .sortedByDescending { it.second }
                                    .take(5)
                                    .forEach { (label, prob) ->
                                        val displayName =
                                            nutritionRepository.getNutritionDisplay(label)?.displayName
                                                ?: label
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 6.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = displayName,
                                                style = MaterialTheme.typography.bodyMedium,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Text(
                                                text = "${String.format("%.2f", prob * 100)}%",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "No image selected",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

private suspend fun saveImageToInternalStorage(context: Context, uri: Uri): String? {
    return withContext(Dispatchers.IO) {
        try {
            val originalBitmap = ImagePreprocessor.loadBitmapFromUri(context, uri)
            val preprocessedBitmap = ImagePreprocessor.preprocessBitmap(originalBitmap, 224)

            val filename = "img_${System.currentTimeMillis()}.jpg"
            val file = File(context.filesDir, filename)

            FileOutputStream(file).use { out ->
                preprocessedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }

            file.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

@Composable
fun NutritionCard(
    nutritionDisplay: NutritionDisplay,
    showPerServing: Boolean
) {
    val nutritionValues = if (showPerServing) nutritionDisplay.perServing else nutritionDisplay.per100g
    val title = if (showPerServing) "Per Serving (${nutritionDisplay.servingDescription})" else "Per 100g"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Nutritional Information",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            NutritionRow("Calories", "${String.format("%.0f", nutritionValues.calories)} kcal")
            NutritionRow("Water", "${String.format("%.1f", nutritionValues.water)}g")
            NutritionRow("Protein", "${String.format("%.1f", nutritionValues.protein)}g")
            NutritionRow("Fat", "${String.format("%.1f", nutritionValues.fat)}g")
            NutritionRow("Total Carbs", "${String.format("%.1f", nutritionValues.totalCarbs)}g")
            NutritionRow("Fiber", "${String.format("%.1f", nutritionValues.fiber)}g")
            NutritionRow("Sugar", "${String.format("%.1f", nutritionValues.sugar)}g")
            NutritionRow("Vitamin C", "${String.format("%.1f", nutritionValues.vitaminC)}mg")
        }
    }
}

@Composable
fun NutritionRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

