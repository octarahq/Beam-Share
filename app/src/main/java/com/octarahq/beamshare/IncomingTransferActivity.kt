package com.octarahq.beamshare

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class IncomingTransferActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        
        setContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    primary = Color(0xFF004BCA),
                    onPrimary = Color.White,
                    primaryContainer = Color(0xFFD3E4FE),
                    onPrimaryContainer = Color(0xFF00174B),
                    secondary = Color(0xFF575F69),
                    onSecondary = Color.White,
                    secondaryContainer = Color(0xFFDBE3EF),
                    onSecondaryContainer = Color(0xFF141C25),
                    background = Color(0xFFF8F9FF),
                    onBackground = Color(0xFF0B1C30),
                    surface = Color(0xFFF8F9FF),
                    onSurface = Color(0xFF0B1C30),
                    surfaceVariant = Color(0xFFD3E4FE),
                    onSurfaceVariant = Color(0xFF424656),
                    outline = Color(0xFF737687)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent
                ) {
                    val currentRequest by IncomingTransferManager.currentRequest.collectAsStateWithLifecycle()
                    
                    LaunchedEffect(currentRequest) {
                        if (currentRequest == null) {
                            finish()
                        }
                    }
                    
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        currentRequest?.let { request ->
                            IncomingTransferDialog(request)
                        }
                    }
                }
            }
        }
    }
}
