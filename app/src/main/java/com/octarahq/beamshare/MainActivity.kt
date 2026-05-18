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
import androidx.compose.ui.res.stringResource
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
                        "privacy" -> PrivacyPolicyScreen(onBack = { currentSubScreen = null })
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
            title = { Text(stringResource(R.string.device_name)) },
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
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = {
                TextButton(onClick = { showNameEditor = false }) { Text(stringResource(R.string.cancel)) }
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
                contentDescription = stringResource(R.string.modify),
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
                    Text(stringResource(R.string.visibility), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = when(visibilityMode) {
                            VisibilityMode.DISABLED -> stringResource(R.string.disabled)
                            VisibilityMode.TRUSTED -> stringResource(R.string.visible_to_trusted)
                            else -> stringResource(R.string.visible_to_everyone)
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
                    title = stringResource(R.string.download_location),
                    subtitle = downloadPath,
                    onClick = { folderPickerLauncher.launch(null) }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                SettingsItem(
                    icon = if (isSystemInDarkTheme()) Icons.Default.DarkMode else Icons.Default.LightMode,
                    title = stringResource(R.string.appearance),
                    subtitle = when(themeMode) {
                        ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                        ThemeMode.DARK -> stringResource(R.string.theme_dark)
                        ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                    },
                    onClick = { showThemeSelector = true }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                SettingsItem(
                    icon = Icons.Default.Wifi,
                    title = stringResource(R.string.data_preferences),
                    subtitle = when(transferMethod) {
                        TransferMethod.WIFI_LOCAL_ONLY -> stringResource(R.string.method_wifi_local)
                        TransferMethod.BLUETOOTH_CLASSIC -> stringResource(R.string.method_bluetooth)
                        TransferMethod.WIFI_DIRECT_HYBRID -> stringResource(R.string.method_wifi_direct)
                        TransferMethod.AUTO -> stringResource(R.string.method_auto)
                    },
                    onClick = { showTransferMethodSelector = true }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                SettingsItem(
                    icon = Icons.Default.Notifications,
                    title = stringResource(R.string.notifications),
                    subtitle = stringResource(R.string.manage_alerts),
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
                    title = stringResource(R.string.transfer_history),
                    onClick = { onNavigate("history") }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                SettingsItem(
                    icon = Icons.Default.Group,
                    title = stringResource(R.string.title_trusted_devices),
                    onClick = { onNavigate("trusted") }
                )
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                SettingsItem(
                    icon = Icons.Default.Block,
                    title = stringResource(R.string.title_blacklist),
                    onClick = { onNavigate("blacklist") }
                )
            }
        }

        Spacer(Modifier.height(32.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.app_name).uppercase(),
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
                        title = stringResource(R.string.manage_permissions),
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
                        title = stringResource(R.string.about_beam_share),
                        onClick = { onNavigate("about") }
                    )
                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                    SettingsItem(
                        icon = Icons.Default.Policy,
                        title = stringResource(R.string.privacy_policy),
                        onClick = { onNavigate("privacy") }
                    )
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.alpha(0.4f)) {
            Text(text = "${stringResource(R.string.app_name)} v$APP_VERSION", style = MaterialTheme.typography.labelSmall)
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
        title = { Text(stringResource(R.string.appearance)) },
        text = {
            Column {
                ThemeOption(
                    title = stringResource(R.string.theme_system),
                    icon = Icons.Default.SettingsSuggest,
                    selected = currentMode == ThemeMode.SYSTEM,
                    onClick = { onSelect(ThemeMode.SYSTEM) }
                )
                ThemeOption(
                    title = stringResource(R.string.theme_light),
                    icon = Icons.Default.LightMode,
                    selected = currentMode == ThemeMode.LIGHT,
                    onClick = { onSelect(ThemeMode.LIGHT) }
                )
                ThemeOption(
                    title = stringResource(R.string.theme_dark),
                    icon = Icons.Default.DarkMode,
                    selected = currentMode == ThemeMode.DARK,
                    onClick = { onSelect(ThemeMode.DARK) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
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
        title = { Text(stringResource(R.string.data_preferences)) },
        text = {
            Column {
                TransferMethodOption(
                    method = TransferMethod.AUTO,
                    title = stringResource(R.string.method_auto),
                    subtitle = stringResource(R.string.temp_everyone_desc),
                    icon = Icons.Default.AutoMode,
                    selected = currentMethod == TransferMethod.AUTO,
                    onClick = { onSelect(TransferMethod.AUTO) }
                )
                TransferMethodOption(
                    method = TransferMethod.WIFI_LOCAL_ONLY,
                    title = stringResource(R.string.method_wifi_local),
                    subtitle = "mDNS + Sockets",
                    icon = Icons.Default.Wifi,
                    selected = currentMethod == TransferMethod.WIFI_LOCAL_ONLY,
                    onClick = { onSelect(TransferMethod.WIFI_LOCAL_ONLY) }
                )
                TransferMethodOption(
                    method = TransferMethod.WIFI_DIRECT_HYBRID,
                    title = stringResource(R.string.method_wifi_direct),
                    subtitle = stringResource(R.string.ready_to_send),
                    icon = Icons.Default.SwapCalls,
                    selected = currentMethod == TransferMethod.WIFI_DIRECT_HYBRID,
                    onClick = { onSelect(TransferMethod.WIFI_DIRECT_HYBRID) }
                )
                TransferMethodOption(
                    method = TransferMethod.BLUETOOTH_CLASSIC,
                    title = stringResource(R.string.method_bluetooth),
                    subtitle = "RFCOMM",
                    icon = Icons.Default.Bluetooth,
                    selected = currentMethod == TransferMethod.BLUETOOTH_CLASSIC,
                    onClick = { onSelect(TransferMethod.BLUETOOTH_CLASSIC) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
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
            imageVector = if (subtitle == stringResource(R.string.manage_alerts) || title == stringResource(R.string.manage_permissions)) Icons.AutoMirrored.Filled.OpenInNew else Icons.Default.ChevronRight,
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
                Text(stringResource(R.string.no_trusted_devices), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn {
                items(trustedMap.keys.toList()) { id ->
                    val name = trustedMap[id] ?: stringResource(R.string.unknown_name)
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                            Text("ID: ${id.take(12)}...", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${stringResource(R.string.verification_code_label)} ${CryptoManager.calculateVerificationCode(id.decodeHex())}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
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
                Text(stringResource(R.string.blacklist_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                Text(stringResource(R.string.no_recent_transfers), color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                                "${if (item.isIncoming) stringResource(R.string.from) else stringResource(R.string.to)} ${item.deviceName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            if (item.success) stringResource(R.string.success) else stringResource(R.string.failed),
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
fun PrivacyPolicyScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            text = stringResource(R.string.privacy_policy),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(Modifier.height(16.dp))
        
        PrivacySection(
            title = stringResource(R.string.privacy_point1_title),
            content = stringResource(R.string.privacy_point1_content)
        )
        
        PrivacySection(
            title = stringResource(R.string.privacy_point2_title),
            content = stringResource(R.string.privacy_point2_content)
        )
        
        PrivacySection(
            title = stringResource(R.string.privacy_point3_title),
            content = stringResource(R.string.privacy_point3_content)
        )
        
        PrivacySection(
            title = stringResource(R.string.privacy_point4_title),
            content = stringResource(R.string.privacy_point4_content)
        )
        
        Spacer(Modifier.height(24.dp))
        
        Text(
            text = "Contact: octara.xyz",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(Modifier.height(48.dp))
    }
}

@Composable
fun PrivacySection(title: String, content: String) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Justify
        )
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
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = stringResource(R.string.app_description),
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
                text = "${stringResource(R.string.title_about)} $APP_VERSION",
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
        currentSubScreen == "trusted" -> stringResource(R.string.title_trusted_devices)
        currentSubScreen == "history" -> stringResource(R.string.title_history)
        currentSubScreen == "blacklist" -> stringResource(R.string.title_blacklist)
        currentSubScreen == "about" -> stringResource(R.string.title_about)
        currentSubScreen == "privacy" -> stringResource(R.string.privacy_policy)
        showBack -> stringResource(R.string.title_settings)
        else -> stringResource(R.string.app_name)
    }

    CenterAlignedTopAppBar(
        navigationIcon = {
            if (showBack) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cancel), tint = MaterialTheme.colorScheme.primary)
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
                    Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.title_settings))
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
                label = stringResource(R.string.tab_receive),
                icon = Icons.Outlined.Download,
                selected = currentTab == "receive",
                onClick = { onTabSelected("receive") }
            )
            NavItem(
                label = stringResource(R.string.tab_send),
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
            text = if (visibilityMode == VisibilityMode.DISABLED) stringResource(R.string.receiving_disabled) else stringResource(R.string.ready_to_receive),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        Text(
            text = stringResource(R.string.visible_as, deviceName),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(40.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.visibility_settings_header),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(8.dp))
            
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    VisibilityOption(
                        title = stringResource(R.string.disable_visibility),
                        subtitle = stringResource(R.string.hidden_from_everyone),
                        icon = Icons.Default.VisibilityOff,
                        selected = visibilityMode == VisibilityMode.DISABLED,
                        onClick = { viewModel.setVisibilityMode(VisibilityMode.DISABLED) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    VisibilityOption(
                        title = stringResource(R.string.visible_to_trusted),
                        subtitle = stringResource(R.string.only_trusted_desc),
                        icon = Icons.Default.Group,
                        selected = visibilityMode == VisibilityMode.TRUSTED,
                        onClick = { viewModel.setVisibilityMode(VisibilityMode.TRUSTED) }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    VisibilityOption(
                        title = if (remainingSeconds > 0) "${stringResource(R.string.everyone)} (${remainingSeconds / 60}:${"%02d".format(remainingSeconds % 60)})" else "${stringResource(R.string.everyone)} ($everyoneTimeout ${stringResource(R.string.min)})",
                        subtitle = stringResource(R.string.temp_everyone_desc),
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
                                Icon(Icons.Default.Remove, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                            Text(
                                text = "$everyoneTimeout ${stringResource(R.string.min)}",
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            IconButton(
                                onClick = { viewModel.adjustTimeout(1) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
                    VisibilityOption(
                        title = stringResource(R.string.visible_to_everyone),
                        subtitle = stringResource(R.string.always_everyone_desc),
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
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape)
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
            text = stringResource(R.string.select_recipient),
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
                text = stringResource(R.string.searching_nearby),
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
                    text = stringResource(R.string.no_devices_found),
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
                color = MaterialTheme.colorScheme.surface,
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
            text = transferState.finishedMessage ?: if (transferState.isTransferring) transferState.status else stringResource(R.string.ready_to_receive_desc),
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
                    text = if (fileName != null) stringResource(R.string.ready_to_send) else stringResource(R.string.no_file),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = fileName ?: stringResource(R.string.select_file_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            if (!isAnyActive) {
                TextButton(onClick = onModify) {
                    Text(stringResource(R.string.modify))
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
            title = { Text(stringResource(R.string.block_device_title)) },
            text = { Text(stringResource(R.string.block_device_desc, request.senderName)) },
            confirmButton = {
                Button(
                    onClick = {
                        settingsManager.blacklistDevice(request.senderId)
                        request.decline()
                        showBlockConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.block))
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    request.decline()
                    showBlockConfirmation = false 
                }) {
                    Text(stringResource(R.string.refuse_only))
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
                TransferStatus.PENDING -> stringResource(R.string.transfer_request_title)
                TransferStatus.TRANSFERRING -> stringResource(R.string.receiving_status)
                TransferStatus.COMPLETED -> stringResource(R.string.transfer_completed)
                TransferStatus.FAILED -> stringResource(R.string.transfer_failed)
            }) 
        },
        text = {
            Column {
                if (request.status == TransferStatus.PENDING) {
                    Text(
                        text = "${request.senderName} ${stringResource(R.string.wants_to_send)}",
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
                                text = if (request.isEncrypted) stringResource(R.string.encrypted_connection) else stringResource(R.string.unsecure_connection),
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
                        text = stringResource(R.string.verification_code_label),
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
                            text = stringResource(R.string.trusted_device_check),
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
                        Text(stringResource(R.string.accept))
                    }
                }
                TransferStatus.COMPLETED -> {
                    Button(onClick = {
                        handleFileAction(context, request)
                        IncomingTransferManager.clear()
                    }) {
                        val label = when {
                            request.fileType.startsWith("image/") || request.fileType.startsWith("video/") -> stringResource(R.string.action_gallery)
                            request.fileType == "text/plain" -> stringResource(R.string.action_copy)
                            else -> stringResource(R.string.action_folders)
                        }
                        Text(label)
                    }
                }
                TransferStatus.FAILED -> {
                    Button(onClick = { IncomingTransferManager.clear() }) { 
                        Text(stringResource(R.string.close)) 
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
                    Text(stringResource(R.string.refuse))
                }
            } else if (request.status == TransferStatus.COMPLETED) {
                TextButton(onClick = { IncomingTransferManager.clear() }) {
                    Text(stringResource(R.string.close))
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
        Color(0xFF6750A4), Color(0xFF388E3C), Color(0xFF455A64), 
        Color(0xFFD32F2F), Color(0xFF1976D2), Color(0xFFF57C00)
    )
    return colors[Math.abs(name.hashCode()) % colors.size]
}
