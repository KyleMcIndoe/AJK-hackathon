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
import androidx.compose.ui.Modifier
import androidx.compose.ui.Modifier.*
import androidx.compose.ui.tooling.preview.Preview
import com.example.application.ui.theme.ApplicationTheme

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
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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

@Composable 
fun Pbutton(imageCaptureUseCase: androidx.camera.core.ImageCapture) {
    val localContext = LocalContext.current

    Button(onClick = {
        val outputFileOptions = ImageCapture.OutputFileOptions.Builder(File(localContext.externalCacheDir, "image.jpg"))
            .build()
        val callback = object: ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                outputFileResults.savedUri?.shareAsImage(localContext)
            }

            override fun onError(exception: ImageCaptureException) {
            }
        }
        imageCaptureUseCase.takePicture(outputFileOptions, ContextCompat.getMainExecutor(localContext), callback)
    }) {
        Text("Take Photo")
    }  
}
