package com.vocabapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.vocabapp.ui.navigation.VocabNavGraph
import com.vocabapp.ui.theme.VocabTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VocabTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    VocabNavGraph()
                }
            }
        }
    }
}
