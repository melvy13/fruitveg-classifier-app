package com.example.apptest

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.apptest.ui.theme.FruitVegTheme
import java.io.File

class MainActivity : ComponentActivity() {

    private var imageUri: Uri? = null

    // Camera launcher
    private val takePicture = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success: Boolean ->
        if (success) {
            imageUri?.let { uri ->
                navigateToClassification(uri)
                Toast.makeText(this, "Photo captured! URI: $uri", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Gallery launcher
    private val pickImage = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            imageUri = it
            navigateToClassification(it)
            Toast.makeText(this, "Image selected! URI: $uri", Toast.LENGTH_SHORT).show()
        }
    }

    // Camera permission launcher
    private val requestCameraPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FruitVegTheme {
                HomeScreen(
                    onCameraClick = { checkCameraPermissionAndOpen() },
                    onGalleryClick = { openGallery() },
                    onHistoryClick = {
                        val intent = android.content.Intent(this, HistoryActivity::class.java)
                        startActivity(intent)
                    }
                )
            }
        }
    }

    private fun checkCameraPermissionAndOpen() {
        when {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            else -> {
                requestCameraPermission.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun openCamera() {
        val photoFile = File(cacheDir, "photo_${System.currentTimeMillis()}.jpg")
        imageUri = FileProvider.getUriForFile(
            this, "${packageName}.provider", photoFile
        )
        takePicture.launch(imageUri)
    }

    private fun openGallery() {
        pickImage.launch("image/*")
    }

    private fun navigateToClassification(uri: Uri) {
        val intent = android.content.Intent(this, ClassificationActivity::class.java)
        intent.putExtra("IMAGE_URI", uri.toString())
        startActivity(intent)
    }

}

@Composable
fun HomeScreen(
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onHistoryClick: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = R.drawable.fnv_icon),
                contentDescription = "App Icon",
                modifier = Modifier.size(96.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Fruit & Vegetable NutriScan",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 48.dp)
            )

            Button(
                onClick = onCameraClick,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Open Camera")
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onGalleryClick,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("Choose from Gallery")
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onHistoryClick,
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("View History")
            }
        }
    }
}
