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

@Composable
fun FooterNav(switchLensFacing: () -> Unit, setZoomLevel: () -> Unit) {
    FooterTheme {
        Row(
            modifier = Modifier.background(Color.White).fillMaxWidth().padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            FlipCameraButton(switchLensFacing)
            ChangeZoomButton(setZoomLevel)
        }
        
    }
}

@Composable
fun FlipCameraButton(switchLensFacing: () -> Unit) {
    fun handleClick() {
        switchLensFacing()
    }

    Box(

    ) {
        Image(
            painter = painterResource(id = R.drawable.switchcamera),
            contentDescription="",
            modifier=Modifier.clickable(onClick = {handleClick()}).size(48.dp)
        )
    }
}

@Composable
fun ChangeZoomButton(setZoomLevel: () -> Unit) {
    fun handleClick() {
        setZoomLevel()
    }

    Box(

    ) {
        Image(
            painter = painterResource(id = R.drawable.magnifyingflass),
            contentDescription="",
            modifier=Modifier.clickable(onClick = {handleClick()}).size(48.dp)
        )
    }
}