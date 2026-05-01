package com.mealplanplus.ui.screens.backup

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.gson.Gson
import com.mealplanplus.data.remote.MealPlanApi
import com.mealplanplus.data.remote.SyncPullResponse
import com.mealplanplus.data.remote.SyncPushRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class BackupRestoreUiState(
    // Local file
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    // Drive
    val isDriveConnected: Boolean = false,
    val isDriveUploading: Boolean = false,
    val isDriveDownloading: Boolean = false,
    val isDriveLoading: Boolean = false,
    val driveBackups: List<DriveBackupEntry> = emptyList(),
    // Feedback
    val successMessage: String? = null,
    val error: String? = null
)

@HiltViewModel
class BackupRestoreViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: MealPlanApi
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupRestoreUiState())
    val uiState: StateFlow<BackupRestoreUiState> = _uiState.asStateFlow()

    private val gson = Gson()
    private val driveScope = Scope("https://www.googleapis.com/auth/drive.appdata")

    /** Options for requesting Drive scope. Call this to build the sign-in intent. */
    fun buildDriveSignInOptions(): GoogleSignInOptions =
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(driveScope)
            .build()

    init { checkDriveConnection() }

    fun checkDriveConnection() {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        val connected = account != null && GoogleSignIn.hasPermissions(account, driveScope)
        _uiState.update { it.copy(isDriveConnected = connected) }
        if (connected) refreshDriveList()
    }

    fun onDriveSignInResult(success: Boolean) {
        val account = GoogleSignIn.getLastSignedInAccount(context)
        val connected = account != null && GoogleSignIn.hasPermissions(account, driveScope)
        _uiState.update { it.copy(isDriveConnected = connected) }
        if (connected) refreshDriveList()
        else if (!success) _uiState.update { it.copy(error = "Google sign-in cancelled or failed") }
    }

    // ── Local file export ──────────────────────────────────────────────────────

    fun exportLocalFile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, error = null) }
            try {
                val data = api.pull(since = "1970-01-01T00:00:00Z")
                val json = gson.toJson(data)
                val file = writeBackupFile(json)
                val uri = FileProvider.getUriForFile(
                    context, "${context.packageName}.provider", file
                )
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "MealPlan+ Backup")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(
                    Intent.createChooser(intent, "Save backup").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                _uiState.update {
                    it.copy(isExporting = false, successMessage = "Backup ready — choose where to save it")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isExporting = false, error = "Export failed: ${e.message}") }
            }
        }
    }

    fun importLocalFile(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isImporting = true, error = null) }
            try {
                val json = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)
                        ?.bufferedReader()?.readText()
                        ?: throw Exception("Could not read file")
                }
                val data = gson.fromJson(json, SyncPullResponse::class.java)
                val pushRequest = SyncPushRequest(
                    meals = data.meals,
                    diets = data.diets,
                    healthMetrics = data.healthMetrics,
                    groceryLists = data.groceryLists
                )
                api.push(pushRequest)
                _uiState.update {
                    it.copy(isImporting = false, successMessage = "Restore complete — data synced to server")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isImporting = false, error = "Import failed: ${e.message}") }
            }
        }
    }

    // ── Google Drive ───────────────────────────────────────────────────────────

    fun uploadToDrive() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDriveUploading = true, error = null) }
            try {
                val token = getDriveToken() ?: throw Exception("Not connected to Google Drive")
                val data = api.pull(since = "1970-01-01T00:00:00Z")
                val json = gson.toJson(data)
                DriveHelper.uploadFile(token, backupFileName(), json)
                refreshDriveList()
                _uiState.update { it.copy(isDriveUploading = false, successMessage = "Backed up to Google Drive") }
            } catch (e: Exception) {
                _uiState.update { it.copy(isDriveUploading = false, error = "Drive upload failed: ${e.message}") }
            }
        }
    }

    fun restoreFromDrive(fileId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isDriveDownloading = true, error = null) }
            try {
                val token = getDriveToken() ?: throw Exception("Not connected to Google Drive")
                val json = DriveHelper.downloadFile(token, fileId)
                val data = gson.fromJson(json, SyncPullResponse::class.java)
                val pushRequest = SyncPushRequest(
                    meals = data.meals,
                    diets = data.diets,
                    healthMetrics = data.healthMetrics,
                    groceryLists = data.groceryLists
                )
                api.push(pushRequest)
                _uiState.update {
                    it.copy(isDriveDownloading = false, successMessage = "Restore complete — data synced from Drive")
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isDriveDownloading = false, error = "Restore failed: ${e.message}") }
            }
        }
    }

    fun deleteDriveBackup(fileId: String) {
        viewModelScope.launch {
            try {
                val token = getDriveToken() ?: return@launch
                DriveHelper.deleteFile(token, fileId)
                refreshDriveList()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Delete failed: ${e.message}") }
            }
        }
    }

    private fun refreshDriveList() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDriveLoading = true) }
            try {
                val token = getDriveToken() ?: return@launch
                val backups = DriveHelper.listBackups(token)
                _uiState.update { it.copy(driveBackups = backups, isDriveLoading = false) }
            } catch (_: Exception) {
                _uiState.update { it.copy(isDriveLoading = false) }
            }
        }
    }

    private suspend fun getDriveToken(): String? = withContext(Dispatchers.IO) {
        val signedIn = GoogleSignIn.getLastSignedInAccount(context) ?: return@withContext null
        val googleAccount = signedIn.account ?: return@withContext null
        try {
            GoogleAuthUtil.getToken(
                context,
                googleAccount,
                "oauth2:https://www.googleapis.com/auth/drive.appdata"
            )
        } catch (_: Exception) { null }
    }

    private fun writeBackupFile(json: String): File {
        val dir = File(context.cacheDir, "backups").also { it.mkdirs() }
        return File(dir, backupFileName()).also { it.writeText(json) }
    }

    private fun backupFileName(): String {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        return "mealplan_backup_$date.json"
    }

    fun clearMessage() { _uiState.update { it.copy(successMessage = null, error = null) } }
}
