package com.example.application

import android.Manifest
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.net.toFile
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.example.application.data.repository.DiscogsRepository
import com.example.application.ui.theme.ApplicationTheme
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.launch
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.fold

class MainActivity : ComponentActivity() {
    private val repository = DiscogsRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ApplicationTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    WithPermission(
                        modifier = Modifier.padding(innerPadding),
                        permission = Manifest.permission.CAMERA
                    ) {
                        CameraAppScreen(
                            onImageCaptured = { uri ->
                                processImage(uri)
                            }
                        )
                    }
                }
            }
        }
    }

    private fun processImage(uri: Uri) {
        lifecycleScope.launch {
            try {
                val image = InputImage.fromFilePath(this@MainActivity, uri)
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

                recognizer.process(image)
                    .addOnSuccessListener { visionText ->
                        val extractedText = visionText.text
                        Log.d("VinylScanner", "Extracted text: $extractedText")

                        // Search Discogs with the extracted text
                        searchDiscogs(extractedText)
                    }
                    .addOnFailureListener { e ->
                        Log.e("VinylScanner", "Text recognition failed", e)
                    }
            } catch (e: Exception) {
                Log.e("VinylScanner", "Image processing failed", e)
            }
        }
    }

    private fun searchDiscogs(query: String) {
        lifecycleScope.launch {
            repository.searchByAlbumName(query).fold(
                onSuccess = { searchResponse ->
                    Log.d("VinylScanner", "Found ${searchResponse.results.size} results")
                    searchResponse.results.forEach { result ->
                        Log.d("VinylScanner", """
                            Title: ${result.title}
                            Year: ${result.year}
                            Genres: ${result.genre?.joinToString()}
                        """.trimIndent())
                    }
                },
                onFailure = { error ->
                    Log.e("VinylScanner", "Discogs search failed: ${error.message}")
                }
            )
        }
    }
}

@Composable
fun CameraAppScreen(
    onImageCaptured: (Uri) -> Unit = {}
) {
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var zoomLevel by remember { mutableFloatStateOf(0.0f) }
    val imageCaptureUseCase = remember { ImageCapture.Builder().build() }
    var showResults by remember { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf<List<SearchResult>>(emptyList()) }
    var isProcessing by remember { mutableStateOf(false) }

    val localContext = LocalContext.current
    val repository = remember { DiscogsRepository() }
    val scope = rememberCoroutineScope()

    Box {
        CameraPreview(
            lensFacing = lensFacing,
            zoomLevel = zoomLevel,
            imageCaptureUseCase = imageCaptureUseCase
        )

        // Camera controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(16.dp)
                )
                Text(
                    text = "Processing image...",
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = { lensFacing = CameraSelector.LENS_FACING_FRONT }) {
                    Text("Front")
                }
                Button(onClick = { lensFacing = CameraSelector.LENS_FACING_BACK }) {
                    Text("Back")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = { zoomLevel = 0.0f }) {
                    Text("1x")
                }
                Button(onClick = { zoomLevel = 0.5f }) {
                    Text("1.5x")
                }
                Button(onClick = { zoomLevel = 1.0f }) {
                    Text("2x")
                }
            }

            Button(
                onClick = {
                    isProcessing = true
                    val outputFileOptions = ImageCapture.OutputFileOptions.Builder(
                        File(localContext.externalCacheDir, "vinyl_${System.currentTimeMillis()}.jpg")
                    ).build()

                    val callback = object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            outputFileResults.savedUri?.let { uri ->
                                // Process the image with ML Kit
                                scope.launch {
                                    processImageAndSearch(
                                        context = localContext,
                                        uri = uri,
                                        repository = repository,
                                        onResults = { results ->
                                            searchResults = results
                                            showResults = true
                                            isProcessing = false
                                        },
                                        onError = {
                                            isProcessing = false
                                        }
                                    )
                                }
                            }
                        }

                        override fun onError(exception: ImageCaptureException) {
                            Log.e("VinylScanner", "Photo capture failed", exception)
                            isProcessing = false
                        }
                    }

                    imageCaptureUseCase.takePicture(
                        outputFileOptions,
                        ContextCompat.getMainExecutor(localContext),
                        callback
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                enabled = !isProcessing
            ) {
                Text("Scan Vinyl Record", style = MaterialTheme.typography.titleMedium)
            }

            if (showResults) {
                Button(
                    onClick = { showResults = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("View Results (${searchResults.size})")
                }
            }
        }
    }

    // Results bottom sheet or dialog
    if (showResults && searchResults.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showResults = false },
            title = { Text("Found Albums") },
            text = {
                LazyColumn {
                    items(searchResults) { result ->
                        AlbumResultItem(result) { releaseId ->
                            // Navigate to details or show more info
                            scope.launch {
                                repository.getReleaseDetails(releaseId).fold(
                                    onSuccess = { release ->
                                        Log.d("VinylScanner", "Full details: $release")
                                        // You can navigate to a details screen here
                                    },
                                    onFailure = { error ->
                                        Log.e("VinylScanner", "Failed to get details", error)
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showResults = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun AlbumResultItem(result: SearchResult, onItemClick: (Long) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        onClick = { onItemClick(result.id) }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = result.title,
                style = MaterialTheme.typography.titleMedium
            )
            result.year?.let {
                Text(
                    text = "Year: $it",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            result.genre?.let { genres ->
                Text(
                    text = "Genres: ${genres.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

suspend fun processImageAndSearch(
    context: Context,
    uri: Uri,
    repository: DiscogsRepository,
    onResults: (List<SearchResult>) -> Unit,
    onError: () -> Unit
) {
    try {
        val image = InputImage.fromFilePath(context, uri)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val extractedText = visionText.text
                Log.d("VinylScanner", "Extracted text: $extractedText")

                // Search Discogs with the extracted text
                kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                    repository.searchByAlbumName(extractedText).fold(
                        onSuccess = { searchResponse ->
                            Log.d("VinylScanner", "Found ${searchResponse.results.size} results")
                            onResults(searchResponse.results)
                        },
                        onFailure = { error ->
                            Log.e("VinylScanner", "Discogs search failed: ${error.message}")
                            onError()
                        }
                    )
                }
            }
            .addOnFailureListener { e ->
                Log.e("VinylScanner", "Text recognition failed", e)
                onError()
            }
    } catch (e: Exception) {
        Log.e("VinylScanner", "Image processing failed", e)
        onError()
    }
}

@Composable
fun CameraPreview(
    modifier: Modifier = Modifier,
    lensFacing: Int,
    zoomLevel: Float,
    imageCaptureUseCase: ImageCapture
) {
    val previewUseCase = remember { Preview.Builder().build() }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var cameraControl by remember { mutableStateOf<CameraControl?>(null) }
    val localContext = LocalContext.current

    fun rebindCameraProvider() {
        cameraProvider?.let { cameraProvider ->
            val cameraSelector = CameraSelector.Builder()
                .requireLensFacing(lensFacing)
                .build()
            cameraProvider.unbindAll()
            val camera = cameraProvider.bindToLifecycle(
                localContext as LifecycleOwner,
                cameraSelector,
                previewUseCase,
                imageCaptureUseCase
            )
            cameraControl = camera.cameraControl
        }
    }

    LaunchedEffect(Unit) {
        cameraProvider = suspendCoroutine { continuation ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(localContext)
            cameraProviderFuture.addListener({
                continuation.resume(cameraProviderFuture.get())
            }, ContextCompat.getMainExecutor(localContext))
        }
        rebindCameraProvider()
    }

    LaunchedEffect(lensFacing) {
        rebindCameraProvider()
    }

    LaunchedEffect(zoomLevel) {
        cameraControl?.setLinearZoom(zoomLevel)
    }

    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            PreviewView(context).also {
                previewUseCase.setSurfaceProvider(it.surfaceProvider)
                rebindCameraProvider()
            }
        }
    )
}