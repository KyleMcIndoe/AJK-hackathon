package com.example.application

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.chaquo.python.Python
import com.chaquo.python.PyException
import com.chaquo.python.PyObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun PhotoButton(
    imageCaptureUseCase: ImageCapture,
    showResults: Boolean,
    setShowResults: (Boolean) -> Unit,
    searchResults: List<SearchResult>
) {
    var isProcessing by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("Scan Vinyl Record") }
    val localContext = LocalContext.current
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status text
        if (isProcessing) {
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            CircularProgressIndicator(
                modifier = Modifier
                    .size(32.dp)
                    .padding(bottom = 8.dp)
            )
        }

        // Scan button
        Button(
            onClick = {
                isProcessing = true
                statusMessage = "Capturing image..."

                Log.d("VinylScanner", "========================================")
                Log.d("VinylScanner", "🎵 SCAN BUTTON PRESSED")
                Log.d("VinylScanner", "========================================")

                val outputFileOptions = ImageCapture.OutputFileOptions.Builder(
                    File(localContext.externalCacheDir, "vinyl_${System.currentTimeMillis()}.jpg")
                ).build()

                val callback = object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        outputFileResults.savedUri?.let { uri ->
                            Log.d("VinylScanner", "✅ Image saved successfully")
                            Log.d("VinylScanner", "📁 URI: $uri")

                            scope.launch {
                                processVinylWithPython(
                                    context = localContext,
                                    imageUri = uri,
                                    onStatusUpdate = { status ->
                                        statusMessage = status
                                        Log.d("VinylScanner", "📊 Status: $status")
                                    },
                                    onComplete = { album, artist ->
                                        isProcessing = false
                                        statusMessage = "Found: $album by $artist"
                                        Log.d("VinylScanner", "========================================")
                                        Log.d("VinylScanner", "✅ IDENTIFICATION COMPLETE!")
                                        Log.d("VinylScanner", "🎵 Album: $album")
                                        Log.d("VinylScanner", "👤 Artist: $artist")
                                        Log.d("VinylScanner", "========================================")
                                    },
                                    onError = { error ->
                                        isProcessing = false
                                        statusMessage = "Error: $error"
                                        Log.e("VinylScanner", "========================================")
                                        Log.e("VinylScanner", "❌ ERROR OCCURRED")
                                        Log.e("VinylScanner", "💥 $error")
                                        Log.e("VinylScanner", "========================================")
                                    }
                                )
                            }
                        } ?: run {
                            isProcessing = false
                            statusMessage = "Failed to save image"
                            Log.e("VinylScanner", "❌ Failed to get saved URI")
                        }
                    }

                    override fun onError(exception: ImageCaptureException) {
                        isProcessing = false
                        statusMessage = "Camera error"
                        Log.e("VinylScanner", "❌ Photo capture failed", exception)
                    }
                }

                imageCaptureUseCase.takePicture(
                    outputFileOptions,
                    ContextCompat.getMainExecutor(localContext),
                    callback
                )
            },
            enabled = !isProcessing,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (!isProcessing) "Scan Vinyl Record" else "Processing...",
                style = MaterialTheme.typography.titleMedium
            )
        }

        if (showResults && searchResults.isNotEmpty()) {
            setShowResults(true)
        }
    }
}

/**
 * Process vinyl image with Python scripts WITH DEBUG LOGGING
 */
suspend fun processVinylWithPython(
    context: Context,
    imageUri: Uri,
    onStatusUpdate: (String) -> Unit,
    onComplete: (album: String, artist: String) -> Unit,
    onError: (String) -> Unit
) = withContext(Dispatchers.IO) {
    try {
        Log.d("VinylScanner", "----------------------------------------")
        Log.d("VinylScanner", "🔄 STARTING PYTHON PROCESSING")
        Log.d("VinylScanner", "----------------------------------------")

        // Get the actual file path from URI
        val imagePath = imageUri.path ?: run {
            Log.e("VinylScanner", "❌ Invalid image path from URI")
            withContext(Dispatchers.Main) {
                onError("Invalid image path")
            }
            return@withContext
        }

        Log.d("VinylScanner", "📁 Image path: $imagePath")

        withContext(Dispatchers.Main) {
            onStatusUpdate("Detecting vinyl...")
        }

        val python = Python.getInstance()
        Log.d("VinylScanner", "✅ Python instance obtained")

        // Load image to get dimensions
        val file = File(imagePath)
        if (!file.exists()) {
            Log.e("VinylScanner", "❌ Image file does not exist at: $imagePath")
            withContext(Dispatchers.Main) {
                onError("Image file not found")
            }
            return@withContext
        }

        Log.d("VinylScanner", "✅ Image file exists")
        Log.d("VinylScanner", "📊 File size: ${file.length()} bytes")

        val img = android.graphics.BitmapFactory.decodeFile(imagePath)

        if (img == null) {
            Log.e("VinylScanner", "❌ Could not decode image file")
            withContext(Dispatchers.Main) {
                onError("Could not load image")
            }
            return@withContext
        }

        val width = img.width
        val height = img.height
        img.recycle()  // Free memory

        Log.d("VinylScanner", "========================================")
        Log.d("VinylScanner", "📐 IMAGE DIMENSIONS")
        Log.d("VinylScanner", "========================================")
        Log.d("VinylScanner", "Width:  $width px")
        Log.d("VinylScanner", "Height: $height px")
        Log.d("VinylScanner", "Aspect ratio: ${width.toFloat() / height.toFloat()}")

        // Default to full image (you can add vinyl detection here later)
        var x1 = 0
        var y1 = 0
        var x2 = width
        var y2 = height

        Log.d("VinylScanner", "========================================")
        Log.d("VinylScanner", "📍 DEFAULT COORDINATES (FULL IMAGE)")
        Log.d("VinylScanner", "========================================")
        Log.d("VinylScanner", "x1 (upper-left X):  $x1")
        Log.d("VinylScanner", "y1 (upper-left Y):  $y1")
        Log.d("VinylScanner", "x2 (lower-right X): $x2")
        Log.d("VinylScanner", "y2 (lower-right Y): $y2")
        Log.d("VinylScanner", "Crop dimensions: ${x2-x1} x ${y2-y1} px")

        // Optional: Uncomment to add vinyl detection
        /*
        Log.d("VinylScanner", "🔍 Attempting vinyl detection...")
        try {
            val detectorModule = python.getModule("vinyl_detector")
            Log.d("VinylScanner", "✅ vinyl_detector module loaded")

            val boxResult = detectorModule.callAttr("detect_vinyl", imagePath)
            Log.d("VinylScanner", "✅ detect_vinyl() called successfully")

            // Convert Python dict to Java Map
            val boxMap = boxResult.toJava(Map::class.java) as Map<*, *>

            x1 = boxMap["x1"].toString().toInt()
            y1 = boxMap["y1"].toString().toInt()
            x2 = boxMap["x2"].toString().toInt()
            y2 = boxMap["y2"].toString().toInt()

            Log.d("VinylScanner", "========================================")
            Log.d("VinylScanner", "📍 DETECTED COORDINATES")
            Log.d("VinylScanner", "========================================")
            Log.d("VinylScanner", "x1 (upper-left X):  $x1")
            Log.d("VinylScanner", "y1 (upper-left Y):  $y1")
            Log.d("VinylScanner", "x2 (lower-right X): $x2")
            Log.d("VinylScanner", "y2 (lower-right Y): $y2")
            Log.d("VinylScanner", "Detected crop: ${x2-x1} x ${y2-y1} px")
            Log.d("VinylScanner", "Crop percentage: ${((x2-x1) * (y2-y1) * 100.0 / (width * height)).toInt()}% of image")

        } catch (e: Exception) {
            Log.w("VinylScanner", "⚠️ Vinyl detection failed, using full image", e)
            Log.w("VinylScanner", "Error message: ${e.message}")
            // Keep using full image coordinates
        }
        */

        Log.d("VinylScanner", "========================================")
        Log.d("VinylScanner", "🐍 CALLING PYTHON PREDICTION")
        Log.d("VinylScanner", "========================================")
        Log.d("VinylScanner", "Module: predict_from_image")
        Log.d("VinylScanner", "Function: identify_album")
        Log.d("VinylScanner", "Arguments:")
        Log.d("VinylScanner", "  - image_path: $imagePath")
        Log.d("VinylScanner", "  - x1: $x1")
        Log.d("VinylScanner", "  - y1: $y1")
        Log.d("VinylScanner", "  - x2: $x2")
        Log.d("VinylScanner", "  - y2: $y2")

        withContext(Dispatchers.Main) {
            onStatusUpdate("Identifying album...")
        }

        // Call predict_from_image.py with coordinates
        val predictModule = python.getModule("predict_from_image")
        Log.d("VinylScanner", "✅ predict_from_image module loaded")

        val result: PyObject = predictModule.callAttr(
            "identify_album",
            imagePath,
            x1,
            y1,
            x2,
            y2
        )
        Log.d("VinylScanner", "✅ identify_album() returned")

        // Access Python dict directly using PyObject methods (no conversion needed)
        Log.d("VinylScanner", "========================================")
        Log.d("VinylScanner", "📦 PYTHON RESULT")
        Log.d("VinylScanner", "========================================")
        Log.d("VinylScanner", "Result object: $result")

        // Check for error using PyObject.get()
        val errorCheck = try {
            result.callAttr("get", "error")
        } catch (e: Exception) {
            null
        }

        if (errorCheck != null && errorCheck.toString() != "None") {
            val error = errorCheck.toString()
            Log.e("VinylScanner", "❌ Python returned error: $error")
            withContext(Dispatchers.Main) {
                onError(error)
            }
            return@withContext
        }

        // Extract album and artist using PyObject methods directly
        val albumObj = try {
            result.callAttr("get", "album")
        } catch (e: Exception) {
            Log.w("VinylScanner", "Could not get album from result", e)
            null
        }

        val artistObj = try {
            result.callAttr("get", "artist")
        } catch (e: Exception) {
            Log.w("VinylScanner", "Could not get artist from result", e)
            null
        }

        val album = albumObj?.toString() ?: "Unknown Album"
        val artist = artistObj?.toString() ?: "Unknown Artist"

        Log.d("VinylScanner", "Album object: $albumObj")
        Log.d("VinylScanner", "Artist object: $artistObj")

        Log.d("VinylScanner", "========================================")
        Log.d("VinylScanner", "✅ SUCCESSFULLY IDENTIFIED")
        Log.d("VinylScanner", "========================================")
        Log.d("VinylScanner", "🎵 Album:  $album")
        Log.d("VinylScanner", "👤 Artist: $artist")
        Log.d("VinylScanner", "========================================")

        withContext(Dispatchers.Main) {
            onComplete(album, artist)
        }

    } catch (e: PyException) {
        Log.e("VinylScanner", "========================================")
        Log.e("VinylScanner", "❌ PYTHON EXCEPTION")
        Log.e("VinylScanner", "========================================")
        Log.e("VinylScanner", "Exception type: ${e.javaClass.simpleName}")
        Log.e("VinylScanner", "Message: ${e.message}")
        Log.e("VinylScanner", "Stack trace:", e)
        Log.e("VinylScanner", "========================================")

        withContext(Dispatchers.Main) {
            onError("Python error: ${e.message}")
        }
    } catch (e: Exception) {
        Log.e("VinylScanner", "========================================")
        Log.e("VinylScanner", "❌ GENERAL EXCEPTION")
        Log.e("VinylScanner", "========================================")
        Log.e("VinylScanner", "Exception type: ${e.javaClass.simpleName}")
        Log.e("VinylScanner", "Message: ${e.message}")
        Log.e("VinylScanner", "Stack trace:", e)
        Log.e("VinylScanner", "========================================")

        withContext(Dispatchers.Main) {
            onError(e.message ?: "Unknown error")
        }
    }
}