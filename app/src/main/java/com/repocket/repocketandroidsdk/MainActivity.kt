package com.repocket.repocketandroidsdk

import android.os.Bundle
import androidx.activity.ComponentActivity
import com.repocket.androidsdk.RepocketSDK.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Initialize("dea4126d-8c28-481f-a2b8-54309e1e5221")
        CreatePeer();
//        StopPeer();
    }
}