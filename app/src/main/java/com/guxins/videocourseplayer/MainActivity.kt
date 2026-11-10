package com.guxins.videocourseplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.guxins.videocourseplayer.ui.VideoCoursePlayerApp
import com.guxins.videocourseplayer.ui.theme.VideoCoursePlayerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val container = (application as VideoCoursePlayerApplication).container
        setContent {
            VideoCoursePlayerTheme {
                VideoCoursePlayerApp(container = container)
            }
        }
    }
}
