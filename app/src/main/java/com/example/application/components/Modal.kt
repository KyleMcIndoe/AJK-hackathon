package com.example.application

import android.R
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
import androidx.compose.ui.tooling.preview.Preview
import com.example.application.ui.theme.ApplicationTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material3.*
import android.util.Log

// @Composable 
// fun Modal(btnText: String) {
//     var isOpen by remember {mutableStateOf(false)}

//     TextButton(onClick={
//         isOpen = !isOpen
//     }) {
//         Text(btnText)
//     }

//     ModalView(isOpen)
// }

// @Composable
// fun ModalView(isOpen: Boolean) {
//     if(isOpen) {
//         Text(
            
//         )
//     }
// }