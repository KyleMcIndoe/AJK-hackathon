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

import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import org.json.JSONObject

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
                        VinylScannerApp()
                    }
                }
            }
        }
    }
}

@Composable
fun VinylScannerApp() {
    // Navigation state - which screen to show
    var selectedRelease by remember { mutableStateOf<Release?>(null) }

    // If no album selected, show camera screen
    // If album selected, show details screen with recommendations
    if (selectedRelease == null) {
        CameraAppScreen(
            onAlbumSelected = { release ->
                selectedRelease = release
            }
        )
    } else {
        AlbumDetailsScreen(
            release = selectedRelease!!,
            onBack = {
                selectedRelease = null // Go back to camera
            }
        )
    }
}

@Composable
fun CameraAppScreen(
    onAlbumSelected: (Release) -> Unit = {}  // ← NEW: Callback when album tapped
) {
    var lensFacing by remember { mutableIntStateOf(CameraSelector.LENS_FACING_BACK) }
    var zoomLevel by remember { mutableFloatStateOf(0.0f) }
    val imageCaptureUseCase = remember { ImageCapture.Builder().build() }

    var showResults by remember { mutableStateOf(false) }
    fun setShowResults(arg: Boolean): Unit {showResults = arg}

    var searchResults by remember { mutableStateOf<List<SearchResult>>(emptyList()) }

    val localContext = LocalContext.current
    val repository = remember { DiscogsRepository() }
    val scope = rememberCoroutineScope()

    fun switchLensFacing(): Unit {
        if(lensFacing == CameraSelector.LENS_FACING_FRONT) {
            lensFacing = CameraSelector.LENS_FACING_BACK
            return Unit
        }
        lensFacing = CameraSelector.LENS_FACING_FRONT
    }

    fun setZoomLevel(): Unit {
        if(zoomLevel == 2.0f) {
            zoomLevel = 0.5f
        }
        zoomLevel = zoomLevel + 0.5f
    }

    Box {
        CameraPreview(
            lensFacing = lensFacing,
            zoomLevel = zoomLevel,
            imageCaptureUseCase = imageCaptureUseCase
        )

        // Camera controls
        Column (
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Bottom
        ) {
            PhotoButton(imageCaptureUseCase, showResults, ::setShowResults, searchResults)
            FooterNav(::switchLensFacing, ::setZoomLevel)
        }
    }

    // Results dialog
    if (showResults && searchResults.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = { showResults = false },
            title = { Text("Found Albums") },
            text = {
                LazyColumn {
                    items(searchResults) { result ->
                        AlbumResultItem(result) { releaseId ->
                            // ← CHANGED: Now fetches full details and navigates
                            scope.launch {
                                repository.getReleaseDetails(releaseId).fold(
                                    onSuccess = { release ->
                                        showResults = false // Close dialog
                                        onAlbumSelected(release) // Navigate to details screen
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