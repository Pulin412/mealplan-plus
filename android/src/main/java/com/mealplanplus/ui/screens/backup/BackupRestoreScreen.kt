package com.mealplanplus.ui.screens.backup

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.mealplanplus.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    onNavigateBack: () -> Unit,
    viewModel: BackupRestoreViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // File picker for local import
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri -> uri?.let { viewModel.importLocalFile(it) } }

    // Google Drive sign-in launcher
    val driveSignInLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.onDriveSignInResult(result.resultCode == Activity.RESULT_OK)
    }

    // Show snackbar for feedback
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Long)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Backup & Restore", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = minimalTopAppBarColors()
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = BgPage
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {

            // ── Info banner ────────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(TagGrayBg, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Default.Info, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                Text(
                    "Export creates a JSON snapshot of all your data. " +
                    "Import pushes it back to the server — safe to run multiple times.",
                    fontSize = 12.sp, color = TextMuted, lineHeight = 17.sp
                )
            }

            // ── Local file section ─────────────────────────────────────────────
            SectionCard(title = "Local file", icon = Icons.Default.Folder) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Save a backup file to your device — Files, email, iCloud, Dropbox, or anywhere else. " +
                        "Works without a Google account.",
                        fontSize = 12.sp, color = TextMuted, lineHeight = 17.sp
                    )

                    // Export
                    ActionButton(
                        label = if (uiState.isExporting) "Preparing export…" else "Export backup",
                        icon = Icons.Default.Upload,
                        enabled = !uiState.isExporting && !uiState.isImporting,
                        loading = uiState.isExporting,
                        onClick = { viewModel.exportLocalFile() }
                    )

                    HorizontalDivider(color = DividerColor, thickness = 0.5.dp)

                    // Import
                    Text(
                        "Import a previously exported backup file to restore your data.",
                        fontSize = 12.sp, color = TextMuted, lineHeight = 17.sp
                    )
                    ActionButton(
                        label = if (uiState.isImporting) "Importing…" else "Import backup file",
                        icon = Icons.Default.Download,
                        enabled = !uiState.isExporting && !uiState.isImporting,
                        loading = uiState.isImporting,
                        secondary = true,
                        onClick = { filePicker.launch("*/*") }
                    )
                }
            }

            // ── Google Drive section ───────────────────────────────────────────
            SectionCard(
                title = "Google Drive",
                icon = Icons.Default.CloudUpload,
                badge = if (uiState.isDriveConnected) "Connected" else null
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!uiState.isDriveConnected) {
                        Text(
                            "Connect your Google account to back up directly to your Drive (appDataFolder — " +
                            "hidden from your Drive file list, stored in your free 15 GB).",
                            fontSize = 12.sp, color = TextMuted, lineHeight = 17.sp
                        )
                        ActionButton(
                            label = "Connect Google account",
                            icon = Icons.Default.AccountCircle,
                            onClick = {
                                val opts = viewModel.buildDriveSignInOptions()
                                val client = GoogleSignIn.getClient(context, opts)
                                driveSignInLauncher.launch(client.signInIntent)
                            }
                        )
                    } else {
                        // Upload
                        ActionButton(
                            label = if (uiState.isDriveUploading) "Uploading…" else "Backup to Drive now",
                            icon = Icons.Default.CloudUpload,
                            enabled = !uiState.isDriveUploading && !uiState.isDriveDownloading,
                            loading = uiState.isDriveUploading,
                            onClick = { viewModel.uploadToDrive() }
                        )

                        HorizontalDivider(color = DividerColor, thickness = 0.5.dp)

                        // Backup list
                        Text(
                            "Previous Drive backups",
                            fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted,
                            letterSpacing = 0.5.sp
                        )

                        if (uiState.isDriveLoading) {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = DesignGreen)
                            }
                        } else if (uiState.driveBackups.isEmpty()) {
                            Text("No Drive backups yet", fontSize = 12.sp, color = TextMuted)
                        } else {
                            uiState.driveBackups.forEach { entry ->
                                DriveBackupRow(
                                    entry = entry,
                                    restoring = uiState.isDriveDownloading,
                                    onRestore = { viewModel.restoreFromDrive(entry.fileId) },
                                    onDelete = { viewModel.deleteDriveBackup(entry.fileId) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    icon: ImageVector,
    badge: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CardBg, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(icon, contentDescription = null, tint = DesignGreen, modifier = Modifier.size(18.dp))
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary,
                modifier = Modifier.weight(1f))
            if (badge != null) {
                Text(
                    badge,
                    fontSize = 9.sp, fontWeight = FontWeight.Bold, color = DesignGreen,
                    modifier = Modifier
                        .background(DesignGreen.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
        HorizontalDivider(color = DividerColor, thickness = 0.5.dp)
        content()
    }
}

@Composable
private fun ActionButton(
    label: String,
    icon: ImageVector,
    enabled: Boolean = true,
    loading: Boolean = false,
    secondary: Boolean = false,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = if (secondary) ButtonDefaults.outlinedButtonColors()
                 else ButtonDefaults.buttonColors(containerColor = TextPrimary, contentColor = CardBg),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = CardBg)
        } else {
            Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DriveBackupRow(
    entry: DriveBackupEntry,
    restoring: Boolean,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgPage, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(entry.createdTime, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
            Text("${entry.size} · ${entry.name}", fontSize = 11.sp, color = TextMuted)
        }
        Spacer(Modifier.width(8.dp))
        IconButton(
            onClick = onRestore,
            enabled = !restoring,
            modifier = Modifier.size(36.dp)
        ) {
            if (restoring) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = DesignGreen)
            } else {
                Icon(Icons.Default.Restore, contentDescription = "Restore", tint = DesignGreen, modifier = Modifier.size(18.dp))
            }
        }
        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextMuted, modifier = Modifier.size(16.dp))
        }
    }
}
