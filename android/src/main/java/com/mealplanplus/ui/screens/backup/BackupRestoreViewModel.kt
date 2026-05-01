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
import com.google.firebase.auth.FirebaseAuth
import com.google.gson.Gson
import com.mealplanplus.data.model.LocalBackupSnapshot
import com.mealplanplus.data.repository.BackupRepository
import com.mealplanplus.util.AuthPreferences
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.inject.Inject

data class BackupRestoreUiState(
    val isExporting: Boolean = false,
    val isImporting: Boolean = false,
    val isDriveConnected: Boolean = false,
    val isDriveUploading: Boolean = false,
    val isDriveDownloading: Boolean = false,
    val isDriveLoading: Boolean = false,
    val driveBackups: List<DriveBackupEntry> = emptyList(),
    val successMessage: String? = null,
    val error: String? = null
)

@HiltViewModel
class BackupRestoreViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val backupRepository: BackupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BackupRestoreUiState())
    val uiState: StateFlow<BackupRestoreUiState> = _uiState.asStateFlow()

    private val gson = Gson()
    private val driveScope = Scope("https://www.googleapis.com/auth/drive.appdata")

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
                val (userId, firebaseUid) = resolveUserIds()
                val snapshot = backupRepository.buildSnapshot(userId, firebaseUid)
                val compressed = compress(gson.toJson(snapshot))
                val file = writeBackupFile(compressed)
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/gzip"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "MealPlan+ Backup")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(
                    Intent.createChooser(intent, "Save backup").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                _uiState.update {
                    it.copy(isExporting = false,
                        successMessage = "Backup ready (${snapshot.summary()}) — choose where to save it")
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
                val (userId, firebaseUid) = resolveUserIds()
                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.readBytes()
                        ?: throw Exception("Could not read file")
                }
                val snapshot = parseSnapshot(bytes)
                backupRepository.restoreSnapshot(userId, firebaseUid, snapshot)
                _uiState.update {
                    it.copy(isImporting = false,
                        successMessage = "Restored from file — ${snapshot.summary()}")
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
                val (userId, firebaseUid) = resolveUserIds()
                val snapshot = backupRepository.buildSnapshot(userId, firebaseUid)
                val compressed = compress(gson.toJson(snapshot))
                DriveHelper.uploadFile(token, backupFileName(), compressed)
                refreshDriveList()
                _uiState.update {
                    it.copy(isDriveUploading = false,
                        successMessage = "Backed up to Google Drive — ${snapshot.summary()}")
                }
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
                val (userId, firebaseUid) = resolveUserIds()
                val bytes = DriveHelper.downloadFile(token, fileId)
                val snapshot = parseSnapshot(bytes)
                backupRepository.restoreSnapshot(userId, firebaseUid, snapshot)
                _uiState.update {
                    it.copy(isDriveDownloading = false,
                        successMessage = "Restored from Drive — ${snapshot.summary()}")
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

    // ── GZIP helpers ───────────────────────────────────────────────────────────

    private fun compress(json: String): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { it.write(json.toByteArray(Charsets.UTF_8)) }
        return bos.toByteArray()
    }

    /**
     * Decompress GZIP bytes → JSON string → [LocalBackupSnapshot].
     * Falls back to treating the bytes as plain JSON for old uncompressed backups.
     */
    private fun parseSnapshot(bytes: ByteArray): LocalBackupSnapshot {
        val json = try {
            GZIPInputStream(bytes.inputStream()).bufferedReader(Charsets.UTF_8).readText()
        } catch (_: Exception) {
            bytes.toString(Charsets.UTF_8)  // plain-JSON fallback (pre-compression backups)
        }
        return gson.fromJson(json, LocalBackupSnapshot::class.java)
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private fun refreshDriveList() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDriveLoading = true) }
            try {
                val token = getDriveToken() ?: return@launch
                val backups = DriveHelper.listBackups(token)
                _uiState.update { it.copy(driveBackups = backups, isDriveLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isDriveLoading = false, error = e.message) }
            }
        }
    }

    private suspend fun resolveUserIds(): Pair<Long, String> {
        val userId = AuthPreferences.getUserId(context).first()
            ?: throw Exception("Not logged in")
        // Prefer the UID persisted at login time; fall back to live Firebase session.
        // Never throw — workout queries cope with an empty UID via OR userId = ''.
        val firebaseUid =
            AuthPreferences.getFirebaseUid(context).first()
                ?: FirebaseAuth.getInstance().currentUser?.uid
                ?: ""
        return Pair(userId, firebaseUid)
    }

    private suspend fun getDriveToken(): String? = withContext(Dispatchers.IO) {
        val signedIn = GoogleSignIn.getLastSignedInAccount(context) ?: return@withContext null
        val googleAccount = signedIn.account ?: return@withContext null
        try {
            GoogleAuthUtil.getToken(
                context, googleAccount,
                "oauth2:https://www.googleapis.com/auth/drive.appdata"
            )
        } catch (_: Exception) { null }
    }

    private fun writeBackupFile(data: ByteArray): File {
        val dir = File(context.cacheDir, "backups").also { it.mkdirs() }
        return File(dir, backupFileName()).also { it.writeBytes(data) }
    }

    private fun backupFileName(): String {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        return "mealplan_backup_$date.json.gz"
    }

    fun clearMessage() { _uiState.update { it.copy(successMessage = null, error = null) } }
}
