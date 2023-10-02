package com.repocket.repocketandroidsdk

import android.os.Bundle
import androidx.activity.ComponentActivity

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.repocket.androidsdk.RepocketSDK.Initialize("dea4126d-8c28-481f-a2b8-54309e1e5221")
    }
}