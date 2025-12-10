package com.example.apptest

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
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
            MaterialTheme {
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
                val history = ClassificationHistory(
                    label = result.label,
                    displayName = nutrition.displayName,
                    confidence = result.confidence,
                    imagePath = imagePath,
                    timestamp = System.currentTimeMillis(),
                    calories = nutrition.perServing.calories,
                    water = nutrition.perServing.water,
                    protein = nutrition.perServing.protein,
                    fat = nutrition.perServing.fat,
                    totalCarbs = nutrition.perServing.totalCarbs,
                    fiber = nutrition.perServing.fiber,
                    sugar = nutrition.perServing.sugar,
                    vitaminC = nutrition.perServing.vitaminC,
                    servingDescription = nutrition.servingDescription
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
            nutritionDisplay = nutritionRepository.getNutritionDisplay(result.label)
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
                Text(
                    text = "Image Classification",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(vertical = 16.dp)
                )

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

                    Spacer(modifier = Modifier.height(24.dp))

                    // Show results
                    if (isClassifying) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Classifying...")
                    } else if (classificationResult != null) {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    text = "Result: ${classificationResult!!.label}",
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Confidence: ${String.format("%.2f%%", classificationResult!!.confidence * 100)}",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = "Top 5 Predictions:",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                classificationResult!!.allProbabilities
                                    .sortedByDescending { it.second }
                                    .take(5)
                                    .forEach { (label, prob) ->
                                        Text(
                                            text = "$label: ${String.format("%.2f%%", prob * 100)}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            modifier = Modifier.padding(vertical = 2.dp)
                                        )
                                    }
                            }
                        }

                        if (nutritionDisplay != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            NutritionCard(nutritionDisplay = nutritionDisplay!!)
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
fun NutritionCard(nutritionDisplay: NutritionDisplay) {
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
                text = "Per Serving (${nutritionDisplay.servingDescription})",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            NutritionRow("Calories", "${String.format("%.0f", nutritionDisplay.perServing.calories)} kcal")
            NutritionRow("Water", "${String.format("%.1f", nutritionDisplay.perServing.water)}g")
            NutritionRow("Protein", "${String.format("%.1f", nutritionDisplay.perServing.protein)}g")
            NutritionRow("Fat", "${String.format("%.1f", nutritionDisplay.perServing.fat)}g")
            NutritionRow("Total Carbs", "${String.format("%.1f", nutritionDisplay.perServing.totalCarbs)}g")
            NutritionRow("Fiber", "${String.format("%.1f", nutritionDisplay.perServing.fiber)}g")
            NutritionRow("Sugar", "${String.format("%.1f", nutritionDisplay.perServing.sugar)}g")
            NutritionRow("Vitamin C", "${String.format("%.1f", nutritionDisplay.perServing.vitaminC)}mg")

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Per 100g",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))

            NutritionRow("Calories", "${String.format("%.0f", nutritionDisplay.per100g.calories)} kcal")
            NutritionRow("Water", "${String.format("%.1f", nutritionDisplay.per100g.water)}g")
            NutritionRow("Protein", "${String.format("%.1f", nutritionDisplay.per100g.protein)}g")
            NutritionRow("Fat", "${String.format("%.1f", nutritionDisplay.per100g.fat)}g")
            NutritionRow("Total Carbs", "${String.format("%.1f", nutritionDisplay.per100g.totalCarbs)}g")
            NutritionRow("Fiber", "${String.format("%.1f", nutritionDisplay.per100g.fiber)}g")
            NutritionRow("Sugar", "${String.format("%.1f", nutritionDisplay.per100g.sugar)}g")
            NutritionRow("Vitamin C", "${String.format("%.1f", nutritionDisplay.per100g.vitaminC)}mg")
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

