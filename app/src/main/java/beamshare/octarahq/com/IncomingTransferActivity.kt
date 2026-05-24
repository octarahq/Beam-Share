package beamshare.octarahq.com

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

class IncomingTransferActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        }
        
        setContent {
            val receiveViewModel: ReceiveViewModel = viewModel()
            val themeMode by receiveViewModel.themeMode.collectAsStateWithLifecycle()
            val isDarkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            MaterialTheme(
                colorScheme = if (isDarkTheme) {
                    darkColorScheme(
                        primary = Color(0xFFD0BCFF),
                        onPrimary = Color(0xFF381E72),
                        primaryContainer = Color(0xFF4F378B),
                        onPrimaryContainer = Color(0xFFEADDFF),
                        secondary = Color(0xFFCCC2DC),
                        onSecondary = Color(0xFF332D41),
                        secondaryContainer = Color(0xFF4A4458),
                        onSecondaryContainer = Color(0xFFE8DEF8),
                        background = Color(0xFF1C1B1F),
                        onBackground = Color(0xFFE6E1E5),
                        surface = Color(0xFF1C1B1F),
                        onSurface = Color(0xFFE6E1E5),
                        surfaceVariant = Color(0xFF49454F),
                        onSurfaceVariant = Color(0xFFCAC4D0),
                        outline = Color(0xFF938F99)
                    )
                } else {
                    lightColorScheme(
                        primary = Color(0xFF6750A4),
                        onPrimary = Color.White,
                        primaryContainer = Color(0xFFEADDFF),
                        onPrimaryContainer = Color(0xFF21005D),
                        secondary = Color(0xFF625B71),
                        onSecondary = Color.White,
                        secondaryContainer = Color(0xFFE8DEF8),
                        onSecondaryContainer = Color(0xFF1D192B),
                        background = Color(0xFFFFFBFE),
                        onBackground = Color(0xFF1C1B1F),
                        surface = Color(0xFFFFFBFE),
                        onSurface = Color(0xFF1C1B1F),
                        surfaceVariant = Color(0xFFE7E0EB),
                        onSurfaceVariant = Color(0xFF49454F),
                        outline = Color(0xFF79747E)
                    )
                }
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
