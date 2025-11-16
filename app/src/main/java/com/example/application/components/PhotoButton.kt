package com.example.application

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier.*
import androidx.compose.ui.tooling.preview.Preview
import com.example.application.ui.theme.*

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.*
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.net.toFile
import androidx.lifecycle.LifecycleOwner
import com.example.application.WithPermission
import com.example.application.ui.theme.CameraXWorkshopTheme
import java.io.File
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.ui.graphics.Color
import com.example.application.ui.theme.FooterTheme
import androidx.compose.ui.unit.*
import androidx.compose.material3.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.compose.ui.platform.LocalContext
import android.util.Log

import androidx.fragment.app.Fragment
import androidx.compose.ui.platform.LocalContext
import android.content.ContentResolver

import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import org.json.JSONObject

@Composable
fun PhotoButton(imageCaptureUseCase: ImageCapture, showResults: Boolean, setShowResults: (Boolean) -> Unit, searchResults: List<SearchResult>) {
    var isProcessing by remember { mutableStateOf(false) }
    val localContext = LocalContext.current

    Row(
            modifier = Modifier
                .padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(
                onClick = {
                    isProcessing = true
                    val outputFileOptions = ImageCapture.OutputFileOptions.Builder(
                        File(localContext.externalCacheDir, "vinyl_${System.currentTimeMillis()}.jpg")
                    ).build()

                    val callback = object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            // image saved at file:///storage/emulated/0/Android/data/com.example.application/cache/vinyl_1763293439252.jpg
                            //Python.start()
                            val py = Python.getInstance()
                            val module = py.getModule("predictor_from_image")
                            val pyResult = module.callAttr("predict_from_image", "${outputFileResults.savedUri}")
                        }

                        override fun onError(exception: ImageCaptureException) {
                            isProcessing = false
                        }
                    }

                    imageCaptureUseCase.takePicture(
                        outputFileOptions,
                        ContextCompat.getMainExecutor(localContext),
                        callback
                    )
                },
                enabled = !isProcessing
            ) {
                if(!isProcessing) { 
                    Text("Scan Vinyl Record", style = MaterialTheme.typography.titleMedium) 
                } else {
                    Text("Processing", style = MaterialTheme.typography.titleMedium)
                }
            }

            if (showResults && searchResults.isNotEmpty()) {
                setShowResults(true)
            }
        }
}

// fun runPythonScript(scriptPath: String, photoPath: String): Unit {
//     val process = ProcessBuilder(
//         "python3",
//         scriptPath,
//         photoPath,
//     ).redirectErrorStream(true).start()

//     val output = process.inputStream.bufferedReader().readText()

//     Log.d("IMPORTANT", "Python returned: $output")  // JSON string
// }
