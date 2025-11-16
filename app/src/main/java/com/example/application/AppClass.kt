package com.example.application

import android.app.Application
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import android.util.Log

class AppClass : Application() {
    override fun onCreate() {
        super.onCreate()
    
        Python.start(AndroidPlatform(this))
        
    }
}