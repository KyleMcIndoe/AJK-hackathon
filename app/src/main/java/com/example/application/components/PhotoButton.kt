package com.example.application.components

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
import com.example.application.SearchResult
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

                scope.launch {
                    // Camera warmup - allow auto-exposure and auto-focus to stabilize
                    statusMessage = "Preparing camera..."
                    Log.d("VinylScanner", "========================================")
                    Log.d("VinylScanner", "🎵 SCAN BUTTON PRESSED")
                    Log.d("VinylScanner", "🎥 Waiting for camera to stabilize...")
                    Log.d("VinylScanner", "========================================")

                    kotlinx.coroutines.delay(500) // 500ms warmup

                    statusMessage = "Capturing image..."
                    Log.d("VinylScanner", "📸 Camera ready, capturing now...")

                    // Create file in cache directory
                    val photoFile = File(
                        localContext.externalCacheDir,
                        "vinyl_${System.currentTimeMillis()}.jpg"
                    )

                    Log.d("VinylScanner", "📁 Will save to: ${photoFile.absolutePath}")

                    val outputFileOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                    val callback = object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            Log.d("VinylScanner", "✅ Camera callback: onImageSaved called")
                            Log.d("VinylScanner", "📁 Initial file size: ${photoFile.length()} bytes")

                            scope.launch {
                                // Wait for file to be fully written
                                var retries = 0
                                val maxRetries = 10
                                var fileReady = false

                                while (retries < maxRetries && !fileReady) {
                                    kotlinx.coroutines.delay(100) // Wait 100ms

                                    val fileSize = photoFile.length()
                                    val exists = photoFile.exists()
                                    val canRead = photoFile.canRead()

                                    Log.d("VinylScanner", "🔄 Check #$retries: exists=$exists, size=$fileSize, readable=$canRead")

                                    // File is ready if it exists, is readable, and has data
                                    if (exists && canRead && fileSize > 1000) { // At least 1KB
                                        // Wait one more cycle to ensure it's stable
                                        kotlinx.coroutines.delay(200)
                                        val newSize = photoFile.length()

                                        if (newSize == fileSize) {
                                            // Size hasn't changed, file is complete!
                                            fileReady = true
                                            Log.d("VinylScanner", "✅ File is stable and ready!")
                                            Log.d("VinylScanner", "📁 Final file size: $fileSize bytes")
                                        } else {
                                            Log.d("VinylScanner", "⏳ File still being written (size changed: $fileSize → $newSize)")
                                        }
                                    }

                                    retries++
                                }

                                if (!fileReady) {
                                    Log.e("VinylScanner", "❌ File not ready after ${maxRetries} retries!")
                                    isProcessing = false
                                    statusMessage = "Error: Image file not ready"
                                    return@launch
                                }

                                // Validate image content
                                statusMessage = "Validating image..."
                                Log.d("VinylScanner", "🔍 Validating image content...")

                                if (!validateImageContent(photoFile)) {
                                    Log.e("VinylScanner", "❌ Image validation failed - too dark")
                                    isProcessing = false
                                    statusMessage = "Error: Image too dark. Try better lighting."
                                    photoFile.delete()
                                    return@launch
                                }

                                Log.d("VinylScanner", "✅ Image validation passed!")

                                Log.d("VinylScanner", "========================================")
                                Log.d("VinylScanner", "✅ FILE VERIFIED AND READY")
                                Log.d("VinylScanner", "📁 Absolute path: ${photoFile.absolutePath}")
                                Log.d("VinylScanner", "📁 File exists: ${photoFile.exists()}")
                                Log.d("VinylScanner", "📁 File size: ${photoFile.length()} bytes")
                                Log.d("VinylScanner", "========================================")

                                processVinylWithPython(
                                    context = localContext,
                                    imageFile = photoFile,
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
                }
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
 * Validates that the captured image is not completely black or corrupted
 */
fun validateImageContent(photoFile: File): Boolean {
    try {
        val options = android.graphics.BitmapFactory.Options().apply {
            inSampleSize = 4 // Downsample for faster validation
        }
        val bitmap = android.graphics.BitmapFactory.decodeFile(photoFile.absolutePath, options)

        if (bitmap == null) {
            Log.e("VinylScanner", "❌ Bitmap decoding failed")
            return false
        }

        // Sample pixels to check if image is completely black
        val width = bitmap.width
        val height = bitmap.height
        val sampleSize = 100
        var totalBrightness = 0

        for (i in 0 until sampleSize) {
            val x = (width * i / sampleSize).coerceIn(0, width - 1)
            val y = (height * i / sampleSize).coerceIn(0, height - 1)
            val pixel = bitmap.getPixel(x, y)

            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            val brightness = (r + g + b) / 3
            totalBrightness += brightness
        }

        val avgBrightness = totalBrightness / sampleSize
        Log.d("VinylScanner", "📊 Image average brightness: $avgBrightness/255")

        bitmap.recycle()

        // If average brightness is extremely low (< 10), image is likely all black
        if (avgBrightness < 10) {
            Log.e("VinylScanner", "❌ Image is too dark (avg: $avgBrightness)")
            return false
        }

        return true
    } catch (e: Exception) {
        Log.e("VinylScanner", "❌ Image validation failed", e)
        return false
    }
}

/**
 * Process vinyl image with your existing Python scripts
 * Uses predict_from_image.py with x2=0, y2=0 to use full image (no cropping)
 */
suspend fun processVinylWithPython(
    context: Context,
    imageFile: File,
    onStatusUpdate: (String) -> Unit,
    onComplete: (album: String, artist: String) -> Unit,
    onError: (String) -> Unit
) = withContext(Dispatchers.IO) {
    try {
        Log.d("VinylScanner", "----------------------------------------")
        Log.d("VinylScanner", "🔄 STARTING PYTHON PROCESSING")
        Log.d("VinylScanner", "----------------------------------------")

        // Get absolute path from File
        val imagePath = imageFile.absolutePath

        Log.d("VinylScanner", "📁 Image file: ${imageFile.name}")
        Log.d("VinylScanner", "📁 Absolute path: $imagePath")
        Log.d("VinylScanner", "📁 File exists: ${imageFile.exists()}")
        Log.d("VinylScanner", "📁 File readable: ${imageFile.canRead()}")
        Log.d("VinylScanner", "📁 File size: ${imageFile.length()} bytes")

        if (!imageFile.exists()) {
            Log.e("VinylScanner", "❌ Image file does not exist!")
            withContext(Dispatchers.Main) {
                onError("Image file not found")
            }
            return@withContext
        }

        if (imageFile.length() == 0L) {
            Log.e("VinylScanner", "❌ Image file is empty (0 bytes)!")
            withContext(Dispatchers.Main) {
                onError("Image file is empty")
            }
            return@withContext
        }

        withContext(Dispatchers.Main) {
            onStatusUpdate("Loading image...")
        }

        val python = Python.getInstance()
        Log.d("VinylScanner", "✅ Python instance obtained")

        // Load image to get dimensions
        val img = android.graphics.BitmapFactory.decodeFile(imagePath)
        if (img == null) {
            Log.e("VinylScanner", "❌ Could not decode image file")
            withContext(Dispatchers.Main) { onError("Could not load image") }
            return@withContext
        }
        img.recycle()

        Log.d("VinylScanner", "========================================")
        Log.d("VinylScanner", "📍 COORDINATES (FULL IMAGE MODE)")
        Log.d("VinylScanner", "========================================")

        Log.d("VinylScanner", "========================================")
        Log.d("VinylScanner", "🐍 CALLING PYTHON PREDICTION")
        Log.d("VinylScanner", "========================================")
        Log.d("VinylScanner", "Module: predict_from_image")
        Log.d("VinylScanner", "Function: identify_album")
        Log.d("VinylScanner", "Arguments:")
        Log.d("VinylScanner", "  - image_path: $imagePath")

        withContext(Dispatchers.Main) {
            onStatusUpdate("Identifying album...")
        }

        // Call your existing predict_from_image.py
        val predictModule = python.getModule("predict_from_image")
        Log.d("VinylScanner", "✅ predict_from_image module loaded")

        val result: PyObject = predictModule.callAttr(
            "identify_album",
            imagePath,
            0,
            0,
            0,
            0
        )
        Log.d("VinylScanner", "✅ identify_album() returned")

        // Access Python dict directly using PyObject methods
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