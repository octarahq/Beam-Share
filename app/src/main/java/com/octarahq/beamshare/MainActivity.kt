package com.octarahq.beamshare

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import java.io.File

const val APP_VERSION = "0.1.0"

class MainActivity : ComponentActivity() {
    private lateinit var sendViewModel: SendViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        CryptoManager.init(this)
        
        sendViewModel = androidx.lifecycle.ViewModelProvider(this)[SendViewModel::class.java]

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val requestPermissionLauncher = registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { _ -> }
            requestPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        handleIntent(intent)

        setContent {
            BeamShareApp(sendViewModel)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        when (intent.action) {
            Intent.ACTION_SEND -> {
                if (intent.type?.startsWith("text/") == true) {
                    val sharedText = intent.getStringExtra(Intent.EXTRA_TEXT)
                    if (sharedText != null) {
                        saveTextToTempFileAndSelect(sharedText)
                    }
                } else {
                    val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(Intent.EXTRA_STREAM)
                    }
                    uri?.let { sendViewModel.selectFile(it) }
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val uris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
                }
                uris?.let {
                    if (it.isNotEmpty()) {
                        sendViewModel.selectFile(it[0])
                    }
                }
            }
        }
    }

    private fun saveTextToTempFileAndSelect(text: String) {
        try {
            val tempFile = File(cacheDir, "shared_text.txt")
            tempFile.writeText(text)
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", tempFile)
            sendViewModel.selectFile(uri)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@Composable
fun BeamShareApp(sendViewModel: SendViewModel, receiveViewModel: ReceiveViewModel = viewModel()) {
    val themeMode by receiveViewModel.themeMode.collectAsStateWithLifecycle()
    val isDarkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    var currentTab by remember { mutableStateOf("receive") }
    var showSettings by remember { mutableStateOf(false) }
    var currentSubScreen by remember { mutableStateOf<String?>(null) }

    BackHandler(enabled = showSettings || currentSubScreen != null) {
        if (currentSubScreen != null) {
            currentSubScreen = null
        } else if (showSettings) {
            showSettings = false
        }
    }

    LaunchedEffect(sendViewModel.selectedFileUri) {
        if (sendViewModel.selectedFileUri != null) {
            currentTab = "send"
            showSettings = false
            currentSubScreen = null
        }
    }

    MaterialTheme(
        colorScheme = if (isDarkTheme) {
            darkColorScheme(
                primary = Color(0xFFB1C5FF),
                onPrimary = Color(0xFF002C71),
                primaryContainer = Color(0xFF0043A6),
                onPrimaryContainer = Color(0xFFD9E2FF),
                secondary = Color(0xFFC0C6DC),
                onSecondary = Color(0xFF2A3042),
                secondaryContainer = Color(0xFF404659),
                onSecondaryContainer = Color(0xFFDCE2F9),
                background = Color(0xFF1B1B1F),
                onBackground = Color(0xFFE3E2E6),
                surface = Color(0xFF1B1B1F),
                onSurface = Color(0xFFE3E2E6),
                surfaceVariant = Color(0xFF44474F),
                onSurfaceVariant = Color(0xFFC4C6D0),
                outline = Color(0xFF8E9099)
            )
        } else {
            lightColorScheme(
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
        }
    ) {
        Scaffold(
            topBar = { 
                TopBar(
                    showBack = showSettings || currentSubScreen != null,
                    currentSubScreen = currentSubScreen,
                    onBack = { 
                        if (currentSubScreen != null) currentSubScreen = null 
                        else showSettings = false 
                    },
                    onSettings = { showSettings = true }
                ) 
            },
            bottomBar = { 
                if (!showSettings && currentSubScreen == null) {
                    BottomNavBar(currentTab) { currentTab = it }
                }
            }
        ) { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                if (currentSubScreen != null) {
                    val context = LocalContext.current
                    val settingsManager = remember { SettingsManager(context) }
                    when (currentSubScreen) {
                        "trusted" -> TrustedDevicesScreen(settingsManager, onBack = { currentSubScreen = null })
                        "blacklist" -> BlacklistScreen(settingsManager, onBack = { currentSubScreen = null })
                        "history" -> HistoryScreen(settingsManager, onBack = { currentSubScreen = null })
                        "about" -> AboutScreen(onBack = { currentSubScreen = null })
                    }
                } else {
                    AnimatedContent(
                        targetState = showSettings,
                        transitionSpec = {
                            if (targetState) {
                                slideInHorizontally { it } + fadeIn() togetherWith slideOutHorizontally { -it } + fadeOut()
                            } else {
                                slideInHorizontally { -it } + fadeIn() togetherWith slideOutHorizontally { it } + fadeOut()
                            }
                        }, label = ""
                    ) { isSettings ->
                        if (isSettings) {
                            SettingsScreen(onNavigate = { currentSubScreen = it })
                        } else {
                            if (currentTab == "receive") {
                                ReceiveScreen()
                            } else {
                                SendScreen(sendViewModel)
                            }
                        }
                    }
                }
                
                val incomingRequest by IncomingTransferManager.currentRequest.collectAsStateWithLifecycle()
                incomingRequest?.let { request ->
                    IncomingTransferDialog(request)
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(onNavigate: (String) -> Unit, viewModel: ReceiveViewModel = viewModel()) {
    val context = LocalContext.current
    val deviceName by viewModel.deviceName.collectAsStateWithLifecycle()
    val visibilityMode by viewModel.visibilityMode.collectAsStateWithLifecycle()
    val downloadPath by viewModel.downloadPathDisplay.collectAsStateWithLifecycle()
    val transferMethod by viewModel.transferMethod.collectAsStateWithLifecycle()
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()

    var showNameEditor by remember { mutableStateOf(false) }
    var showTransferMethodSelector by remember { mutableStateOf(false) }
    var showThemeSelector by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf(deviceName) }

    val folderPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        viewModel.updateDownloadUri(uri)
    }

    if (showThemeSelector) {
        ThemeSelectorDialog(
            currentMode = themeMode,
            onDismiss = { showThemeSelector = false },
            onSelect = {
                viewModel.setThemeMode(it)
                showThemeSelector = false
            }
        )
    }

    if (showTransferMethodSelector) {
        TransferMethodDialog(
            currentMethod = transferMethod,
            onDismiss = { showTransferMethodSelector = false },
            onSelect = {
                viewModel.setTransferMethod(it)
                showTransferMethodSelector = false
            }
        )
    }

    if (showNameEditor) {
        AlertDialog(
            onDismissRequest = { showNameEditor = false },
            title = { Text("Nom de l'appareil") },
            text = {
                TextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateDeviceName(tempName)
                    showNameEditor = false
                }) { Text("Enregistrer") }
            },
            dismissButton = {
                TextButton(onClick = { showNameEditor = false }) { Text("Annuler") }
            }
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerLow),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Smartphone,
                contentDescription = null,
                modifier = Modifier.size(120.dp).graphicsLayer { alpha = 0.8f },
                tint = MaterialTheme.colorScheme.primary
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(24.dp))
                    .graphicsLayer { scaleX = 1.05f; scaleY = 1.05f }
            )
        }
        
        Spacer(Modifier.height(16.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable { 
                tempName = deviceName
                showNameEditor = true 
            }
        ) {
            Text(
                text = deviceName,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Default.Edit,
                contentDescription = "Modifier",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.outline
            )
        }

        Spacer(Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text("Visibilité", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = when(visibilityMode) {
                            VisibilityMode.DISABLED -> "Désactivé"
                            VisibilityMode.TRUSTED -> "Appareils de confiance"
                            else -> "Visible par tout le monde"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column {
                SettingsItem(
                    icon = Icons.Default.Folder,
                    title = "Emplacement de téléchargement",
                    subtitle = downloadPath,
                    onClick = { folderPickerLauncher.launch(null) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                SettingsItem(
                    icon = if (isSystemInDarkTheme()) Icons.Default.DarkMode else Icons.Default.LightMode,
                    title = "Apparence",
                    subtitle = when(themeMode) {
                        ThemeMode.LIGHT -> "Clair"
                        ThemeMode.DARK -> "Sombre"
                        ThemeMode.SYSTEM -> "Système"
                    },
                    onClick = { showThemeSelector = true }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                SettingsItem(
                    icon = Icons.Default.Wifi,
                    title = "Préférences de données",
                    subtitle = when(transferMethod) {
                        TransferMethod.WIFI_LOCAL_ONLY -> "Wi-Fi local uniquement"
                        TransferMethod.BLUETOOTH_CLASSIC -> "Bluetooth Classic"
                        TransferMethod.WIFI_DIRECT_HYBRID -> "Wi-Fi Direct Hybride"
                        TransferMethod.AUTO -> "Automatique"
                    },
                    onClick = { showTransferMethodSelector = true }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                SettingsItem(
                    icon = Icons.Default.Notifications,
                    title = "Notifications",
                    subtitle = "Gérer les alertes",
                    onClick = {
                        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                        } else {
                            @Suppress("DEPRECATION")
                            Intent("android.settings.APP_NOTIFICATION_SETTINGS").apply {
                                putExtra("app_package", context.packageName)
                                putExtra("app_uid", context.applicationInfo.uid)
                            }
                        }
                        context.startActivity(intent)
                    }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column {
                SettingsItem(
                    icon = Icons.Default.History,
                    title = "Historique des transferts",
                    onClick = { onNavigate("history") }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                SettingsItem(
                    icon = Icons.Default.Group,
                    title = "Appareils de confiance",
                    onClick = { onNavigate("trusted") }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                SettingsItem(
                    icon = Icons.Default.Block,
                    title = "Liste noire",
                    onClick = { onNavigate("blacklist") }
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "BEAM SHARE",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    SettingsItem(
                        icon = Icons.Default.Security,
                        title = "Gérer les autorisations",
                        onClick = {
                            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                            context.startActivity(intent)
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    SettingsItem(
                        icon = Icons.Default.Info,
                        title = "À propos de Beam Share",
                        onClick = { onNavigate("about") }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    SettingsItem(
                        icon = Icons.Default.Policy,
                        title = "Politique de confidentialité",
                        onClick = { }
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.alpha(0.4f)) {
            Text(text = "Beam Share v$APP_VERSION", style = MaterialTheme.typography.labelSmall)
            Text(text = "Powered by Octara", style = MaterialTheme.typography.labelSmall)
        }
        Spacer(Modifier.height(48.dp))
    }
}

@Composable
fun ThemeSelectorDialog(
    currentMode: ThemeMode,
    onDismiss: () -> Unit,
    onSelect: (ThemeMode) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Apparence") },
        text = {
            Column {
                ThemeOption(
                    title = "Système",
                    icon = Icons.Default.SettingsSuggest,
                    selected = currentMode == ThemeMode.SYSTEM,
                    onClick = { onSelect(ThemeMode.SYSTEM) }
                )
                ThemeOption(
                    title = "Clair",
                    icon = Icons.Default.LightMode,
                    selected = currentMode == ThemeMode.LIGHT,
                    onClick = { onSelect(ThemeMode.LIGHT) }
                )
                ThemeOption(
                    title = "Sombre",
                    icon = Icons.Default.DarkMode,
                    selected = currentMode == ThemeMode.DARK,
                    onClick = { onSelect(ThemeMode.DARK) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fermer") }
        }
    )
}

@Composable
fun ThemeOption(
    title: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun TransferMethodDialog(
    currentMethod: TransferMethod,
    onDismiss: () -> Unit,
    onSelect: (TransferMethod) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Méthode de transfert") },
        text = {
            Column {
                TransferMethodOption(
                    method = TransferMethod.AUTO,
                    title = "Automatique",
                    subtitle = "Alterne intelligemment selon la connexion",
                    icon = Icons.Default.AutoMode,
                    selected = currentMethod == TransferMethod.AUTO,
                    onClick = { onSelect(TransferMethod.AUTO) }
                )
                TransferMethodOption(
                    method = TransferMethod.WIFI_LOCAL_ONLY,
                    title = "Wi-Fi local",
                    subtitle = "Réseau Wi-Fi commun (mDNS + Sockets)",
                    icon = Icons.Default.Wifi,
                    selected = currentMethod == TransferMethod.WIFI_LOCAL_ONLY,
                    onClick = { onSelect(TransferMethod.WIFI_LOCAL_ONLY) }
                )
                TransferMethodOption(
                    method = TransferMethod.WIFI_DIRECT_HYBRID,
                    title = "Wi-Fi Direct Hybride",
                    subtitle = "Connexion directe haute vitesse (4G/Extérieur)",
                    icon = Icons.Default.SwapCalls,
                    selected = currentMethod == TransferMethod.WIFI_DIRECT_HYBRID,
                    onClick = { onSelect(TransferMethod.WIFI_DIRECT_HYBRID) }
                )
                TransferMethodOption(
                    method = TransferMethod.BLUETOOTH_CLASSIC,
                    title = "Bluetooth Classic",
                    subtitle = "Petits fichiers et texte (RFCOMM)",
                    icon = Icons.Default.Bluetooth,
                    selected = currentMethod == TransferMethod.BLUETOOTH_CLASSIC,
                    onClick = { onSelect(TransferMethod.BLUETOOTH_CLASSIC) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Fermer") }
        }
    )
}

@Composable
fun TransferMethodOption(
    method: TransferMethod,
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun SettingsItem(icon: ImageVector, title: String, subtitle: String? = null, onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
            }
        }
        Icon(
            imageVector = if (subtitle == "Gérer les alertes" || title == "Gérer les autorisations") Icons.AutoMirrored.Filled.OpenInNew else Icons.Default.ChevronRight,
            contentDescription = null, 
            modifier = Modifier.size(20.dp), 
            tint = MaterialTheme.colorScheme.outline
        )
    }
}

@Composable
fun TrustedDevicesScreen(settingsManager: SettingsManager, onBack: () -> Unit) {
    val trustedMap = remember { mutableStateMapOf<String, String?>().apply { putAll(settingsManager.getTrustedDevicesMap()) } }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(16.dp))
        if (trustedMap.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Aucun appareil de confiance", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn {
                items(trustedMap.keys.toList()) { id ->
                    val name = trustedMap[id] ?: "Nom inconnu"
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            Text("ID: ${id.take(12)}...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Code: ${CryptoManager.calculateVerificationCode(id.decodeHex())}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = {
                            settingsManager.removeTrustedDevice(id)
                            trustedMap.remove(id)
                        }) {
                            Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                }
            }
        }
    }
}

@Composable
fun BlacklistScreen(settingsManager: SettingsManager, onBack: () -> Unit) {
    val blacklistedDevices = remember { mutableStateListOf<String>().apply { addAll(settingsManager.getBlacklistedDevices()) } }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(16.dp))
        if (blacklistedDevices.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("La liste noire est vide", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn {
                items(blacklistedDevices) { id ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("ID: ${id.take(12)}...", style = MaterialTheme.typography.bodyLarge)
                        IconButton(onClick = {
                            settingsManager.removeBlacklistedDevice(id)
                            blacklistedDevices.remove(id)
                        }) {
                            Icon(Icons.Default.Restore, null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                }
            }
        }
    }
}

@Composable
fun HistoryScreen(settingsManager: SettingsManager, onBack: () -> Unit) {
    var history by remember { mutableStateOf(settingsManager.getHistory()) }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(16.dp))
        if (history.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Aucun transfert récent", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn {
                items(history) { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(
                                if (item.isIncoming) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primaryContainer
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (item.isIncoming) Icons.Default.Download else Icons.Default.Upload,
                                null,
                                tint = if (item.isIncoming) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(item.fileName, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${if (item.isIncoming) "De" else "Vers"} ${item.deviceName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            if (item.success) "OK" else "Échec",
                            color = if (item.success) Color(0xFF2E7D32) else Color.Red,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                }
            }
        }
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = R.mipmap.ic_launcher,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp)
                )
            }
            
            Spacer(Modifier.height(24.dp))
            
            Text(
                text = "Beam Share",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "Transfert de fichiers décentralisé",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Version $APP_VERSION",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.outline
            )
            Text(
                text = "© 2024 Octara HQ",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(showBack: Boolean, currentSubScreen: String?, onBack: () -> Unit, onSettings: () -> Unit) {
    val title = when {
        currentSubScreen == "trusted" -> "Appareils de confiance"
        currentSubScreen == "history" -> "Historique"
        currentSubScreen == "blacklist" -> "Liste noire"
        currentSubScreen == "about" -> "À propos"
        showBack -> "Settings"
        else -> "Beam Share"
    }

    CenterAlignedTopAppBar(
        navigationIcon = {
            if (showBack) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.primary)
                }
            } else {
                Icon(
                    imageVector = Icons.Default.Share,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 16.dp).size(24.dp)
                )
            }
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
        },
        actions = {
            if (!showBack) {
                IconButton(onClick = onSettings) {
                    Icon(Icons.Default.Settings, contentDescription = "Settings")
                }
            } else if (currentSubScreen == "history") {
                val context = LocalContext.current
                val settingsManager = remember { SettingsManager(context) }
                IconButton(onClick = { settingsManager.clearHistory() }) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear History", tint = MaterialTheme.colorScheme.primary)
                }
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
        )
    )
}

@Composable
fun BottomNavBar(currentTab: String, onTabSelected: (String) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavItem(
                label = "Receive",
                icon = Icons.Outlined.Download,
                selected = currentTab == "receive",
                onClick = { onTabSelected("receive") }
            )
            NavItem(
                label = "Send",
                icon = Icons.Outlined.Upload,
                selected = currentTab == "send",
                onClick = { onTabSelected("send") }
            )
        }
    }
}

@Composable
fun NavItem(label: String, icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(CircleShape)
            .clickable { onClick() }
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ReceiveScreen(viewModel: ReceiveViewModel = viewModel()) {
    val visibilityMode by viewModel.visibilityMode.collectAsStateWithLifecycle()
    val deviceName by viewModel.deviceName.collectAsStateWithLifecycle()
    val remainingSeconds by viewModel.remainingSeconds.collectAsStateWithLifecycle()
    val everyoneTimeout by viewModel.everyoneTimeoutMinutes.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .padding(vertical = 40.dp)
                .size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            if (visibilityMode != VisibilityMode.DISABLED) {
                PulseAnimation()
            }
            
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(if (visibilityMode == VisibilityMode.DISABLED) Color.Gray else MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardDoubleArrowDown,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        Text(
            text = if (visibilityMode == VisibilityMode.DISABLED) "Receiving Disabled" else "Ready to Receive",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        Text(
            text = buildString {
                append("Visible as ")
                append(deviceName)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(40.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "VISIBILITY SETTINGS",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(8.dp))
            
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    VisibilityOption(
                        title = "Désactiver",
                        subtitle = "Hidden from everyone",
                        icon = Icons.Default.VisibilityOff,
                        selected = visibilityMode == VisibilityMode.DISABLED,
                        onClick = { viewModel.setVisibilityMode(VisibilityMode.DISABLED) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    VisibilityOption(
                        title = "Appareils de confiance",
                        subtitle = "Only your trusted devices",
                        icon = Icons.Default.Group,
                        selected = visibilityMode == VisibilityMode.TRUSTED,
                        onClick = { viewModel.setVisibilityMode(VisibilityMode.TRUSTED) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    VisibilityOption(
                        title = if (remainingSeconds > 0) "Tout le monde (${remainingSeconds / 60}:${"%02d".format(remainingSeconds % 60)})" else "Tout le monde ($everyoneTimeout min)",
                        subtitle = "Temporary discovery mode",
                        icon = Icons.Default.Public,
                        selected = visibilityMode == VisibilityMode.EVERYONE_TEMP,
                        onClick = { viewModel.setVisibilityMode(VisibilityMode.EVERYONE_TEMP) }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            IconButton(
                                onClick = { viewModel.adjustTimeout(-1) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = "Moins", tint = MaterialTheme.colorScheme.primary)
                            }
                            Text(
                                text = "$everyoneTimeout min",
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            IconButton(
                                onClick = { viewModel.adjustTimeout(1) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "Plus", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    VisibilityOption(
                        title = "Tout le monde pour toujours",
                        subtitle = "Always discoverable",
                        icon = Icons.Default.AllInclusive,
                        selected = visibilityMode == VisibilityMode.EVERYONE_ALWAYS,
                        onClick = { viewModel.setVisibilityMode(VisibilityMode.EVERYONE_ALWAYS) }
                    )
                }
            }
        }
    }
}

@Composable
fun VisibilityOption(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    extraContent: @Composable (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(if (selected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f) else Color.Transparent)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (selected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        if (extraContent != null && selected) {
            extraContent()
        }
    }
}

@Composable
fun PulseAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "scale"
    )
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "alpha"
    )

    Box(
        modifier = Modifier
            .size(120.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = alphaAnim
            }
            .background(MaterialTheme.colorScheme.primary, CircleShape)
    )
}

@Composable
fun SendScreen(viewModel: SendViewModel) {
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val pickFileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.selectFile(it) }
    }

    DisposableEffect(Unit) {
        viewModel.startDiscovery()
        onDispose { viewModel.stopDiscovery() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(24.dp))
        
        Text(
            text = "Sélectionner un destinataire",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 8.dp)
        ) {
            RadarPing()
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Recherche d'appareils à proximité...",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            RadarBackground()

            if (devices.isEmpty()) {
                Text(
                    text = "Aucun appareil trouvé",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    items(devices) { device ->
                        val transferState = viewModel.transferStates[device.name] ?: DeviceTransferState()
                        DeviceBentoCard(
                            device = device,
                            transferState = transferState,
                            onClick = { viewModel.selectDevice(device) }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        FileSelectionCard(
            fileName = viewModel.selectedFileName,
            isAnyActive = viewModel.isAnyTransferActive,
            onModify = { pickFileLauncher.launch("*/*") }
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun RadarPing() {
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart),
        label = "scale"
    )
    val alpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart),
        label = "alpha"
    )

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(MaterialTheme.colorScheme.primary, CircleShape)
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                }
                .border(1.dp, MaterialTheme.colorScheme.primary, CircleShape)
        )
    }
}

@Composable
fun RadarBackground() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        repeat(3) { i ->
            val size = (300 + i * 200).dp
            Box(
                modifier = Modifier
                    .size(size)
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.05f), CircleShape)
            )
        }
    }
}

@Composable
fun DeviceBentoCard(
    device: BeamDevice,
    transferState: DeviceTransferState,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(getDeviceBackgroundColor(device.name))
                    .border(
                        if (transferState.isTransferring) 2.dp else 0.dp,
                        MaterialTheme.colorScheme.primary,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = "https://ui-avatars.com/api/?name=${device.name}&background=random&color=fff&size=128",
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                
                if (transferState.finishedMessage != null) {
                    val bgColor = if (transferState.isError) Color(0xFFBA1A1A) else Color(0xFF008038)
                    val icon = if (transferState.isError) Icons.Default.Close else Icons.Default.Check
                    
                    Box(Modifier.fillMaxSize().background(bgColor.copy(alpha = 0.8f)), contentAlignment = Alignment.Center) {
                        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
                    }
                }
            }

            Surface(
                modifier = Modifier
                    .size(28.dp)
                    .offset(x = (-2).dp, y = (-2).dp),
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Icon(
                    imageVector = getDeviceIcon(device.model),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Text(
            text = device.name,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = transferState.finishedMessage ?: if (transferState.isTransferring) transferState.status else "Prêt à recevoir",
            style = MaterialTheme.typography.labelSmall,
            color = if (transferState.finishedMessage != null) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        if (transferState.isTransferring) {
            Spacer(Modifier.height(4.dp))
            if (transferState.progress < 0) {
                 LinearProgressIndicator(modifier = Modifier.width(64.dp).height(2.dp).clip(CircleShape))
            } else {
                LinearProgressIndicator(
                    progress = { transferState.progress },
                    modifier = Modifier.width(64.dp).height(2.dp).clip(CircleShape)
                )
            }
        }
    }
}

@Composable
fun FileSelectionCard(fileName: String?, isAnyActive: Boolean, onModify: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Description,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            Spacer(Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (fileName != null) "Prêt à envoyer" else "Aucun fichier",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = fileName ?: "Sélectionnez ce que vous voulez envoyer",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            if (!isAnyActive) {
                TextButton(onClick = onModify) {
                    Text("Modifier")
                }
            }
        }
    }
}

@Composable
fun IncomingTransferDialog(request: IncomingTransferRequest) {
    val context = LocalContext.current
    val settingsManager = remember { SettingsManager(context) }
    var showBlockConfirmation by remember { mutableStateOf(false) }
    
    if (showBlockConfirmation) {
        AlertDialog(
            onDismissRequest = { showBlockConfirmation = false },
            title = { Text("Bloquer cet appareil ?") },
            text = { Text("Si vous bloquez ${request.senderName}, il ne pourra plus vous envoyer de fichiers.") },
            confirmButton = {
                Button(
                    onClick = {
                        settingsManager.blacklistDevice(request.senderId)
                        request.decline()
                        showBlockConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Bloquer")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    request.decline()
                    showBlockConfirmation = false 
                }) {
                    Text("Refuser seulement")
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = { 
            if (request.status != TransferStatus.TRANSFERRING) IncomingTransferManager.clear() 
        },
        icon = { 
            Icon(
                imageVector = when(request.status) {
                    TransferStatus.COMPLETED -> Icons.Default.CheckCircle
                    TransferStatus.FAILED -> Icons.Default.Error
                    else -> Icons.Default.Download
                }, 
                contentDescription = null,
                tint = if (request.status == TransferStatus.COMPLETED) Color(0xFF2E7D32) else MaterialTheme.colorScheme.primary
            ) 
        },
        title = { 
            Text(text = when(request.status) {
                TransferStatus.PENDING -> "Demande de transfert"
                TransferStatus.TRANSFERRING -> "Réception en cours..."
                TransferStatus.COMPLETED -> "Transfert terminé"
                TransferStatus.FAILED -> "Échec du transfert"
            }) 
        },
        text = {
            Column {
                if (request.status == TransferStatus.PENDING) {
                    Text(
                        text = "${request.senderName} veut vous envoyer :",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    
                    Spacer(Modifier.height(8.dp))
                    
                    Surface(
                        color = if (request.isEncrypted) Color(0xFFE8F5E9) else Color(0xFFFFF3E0),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (request.isEncrypted) Icons.Default.Security else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (request.isEncrypted) Color(0xFF2E7D32) else Color(0xFFE65100),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (request.isEncrypted) "Connexion chiffrée (AES-256)" else "⚠️ Connexion non sécurisée",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (request.isEncrypted) Color(0xFF2E7D32) else Color(0xFFE65100)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = request.fileName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${request.fileSize / 1024} KB • ${request.fileType}",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                
                if (request.status == TransferStatus.TRANSFERRING) {
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { request.progress },
                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape)
                    )
                }

                if (request.status == TransferStatus.PENDING) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Code de vérification :",
                        style = MaterialTheme.typography.labelSmall
                    )
                    Text(
                        text = request.verificationCode,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (request.isTrusted) {
                        Text(
                            text = "✓ Appareil de confiance",
                            color = Color(0xFF2E7D32),
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
                
                if (request.status == TransferStatus.FAILED) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = request.errorMessage ?: "Erreur inconnue",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            when (request.status) {
                TransferStatus.PENDING -> {
                    Button(
                        onClick = { request.accept() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Accepter")
                    }
                }
                TransferStatus.COMPLETED -> {
                    Button(onClick = {
                        handleFileAction(context, request)
                        IncomingTransferManager.clear()
                    }) {
                        val label = when {
                            request.fileType.startsWith("image/") || request.fileType.startsWith("video/") -> "Afficher dans la galerie"
                            request.fileType == "text/plain" -> "Copier"
                            else -> "Afficher dans mes dossiers"
                        }
                        Text(label)
                    }
                }
                TransferStatus.FAILED -> {
                    Button(onClick = { IncomingTransferManager.clear() }) { 
                        Text("Fermer") 
                    }
                }
                else -> {}
            }
        },
        dismissButton = {
            if (request.status == TransferStatus.PENDING) {
                TextButton(onClick = { 
                    if (!request.isTrusted) {
                        showBlockConfirmation = true
                    } else {
                        request.decline()
                    }
                }) {
                    Text("Refuser")
                }
            } else if (request.status == TransferStatus.COMPLETED) {
                TextButton(onClick = { IncomingTransferManager.clear() }) {
                    Text("Fermer")
                }
            }
        }
    )
}

private fun handleFileAction(context: Context, request: IncomingTransferRequest) {
    val filePath = request.savedFilePath ?: return
    val file = File(filePath)
    if (!file.exists()) return

    when {
        request.fileType == "text/plain" -> {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("BeamShare Text", file.readText())
            clipboard.setPrimaryClip(clip)
        }
        request.fileType.startsWith("image/") || request.fileType.startsWith("video/") -> {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                setDataAndType(uri, request.fileType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
        else -> {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                setDataAndType(uri, "*/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }
}

fun getDeviceIcon(model: String): ImageVector {
    val n = model.lowercase()
    return when {
        n.contains("mac") || n.contains("pc") || n.contains("laptop") || n.contains("linux") || n.contains("windows") -> Icons.Default.Laptop
        n.contains("tablet") || n.contains("ipad") || n.contains("tab") -> Icons.Default.Tablet
        n.contains("tv") -> Icons.Default.Tv
        n.contains("studio") || n.contains("desktop") -> Icons.Default.DesktopWindows
        else -> Icons.Default.Smartphone
    }
}

fun getDeviceBackgroundColor(name: String): Color {
    val colors = listOf(
        Color(0xFF004BCA), Color(0xFF008038), Color(0xFF575F69), 
        Color(0xFFBA1A1A), Color(0xFF00642A), Color(0xFF0052DC)
    )
    return colors[name.hashCode().coerceAtLeast(0) % colors.size]
}
