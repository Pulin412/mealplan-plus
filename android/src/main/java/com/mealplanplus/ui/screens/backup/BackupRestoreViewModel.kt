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
import com.mealplanplus.data.local.DietDao
import com.mealplanplus.data.local.GroceryDao
import com.mealplanplus.data.local.HealthMetricDao
import com.mealplanplus.data.local.MealDao
import com.mealplanplus.data.model.*
import com.mealplanplus.data.remote.*
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
    private val mealDao: MealDao,
    private val dietDao: DietDao,
    private val healthMetricDao: HealthMetricDao,
    private val groceryDao: GroceryDao
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
                val userId = AuthPreferences.getUserId(context).first()
                    ?: throw Exception("Not logged in")
                val snapshot = buildSnapshot(userId)
                val json = gson.toJson(snapshot)
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
                val userId = AuthPreferences.getUserId(context).first()
                    ?: throw Exception("Not logged in")
                val json = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)
                        ?.bufferedReader()?.readText()
                        ?: throw Exception("Could not read file")
                }
                val data = gson.fromJson(json, SyncPullResponse::class.java)
                restoreSnapshot(userId, data)
                _uiState.update {
                    it.copy(isImporting = false, successMessage = "Restore complete — data imported from file")
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
                val userId = AuthPreferences.getUserId(context).first()
                    ?: throw Exception("Not logged in")
                val snapshot = buildSnapshot(userId)
                val json = gson.toJson(snapshot)
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
                val userId = AuthPreferences.getUserId(context).first()
                    ?: throw Exception("Not logged in")
                val json = DriveHelper.downloadFile(token, fileId)
                val data = gson.fromJson(json, SyncPullResponse::class.java)
                restoreSnapshot(userId, data)
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

    // ── Room snapshot helpers ──────────────────────────────────────────────────

    /**
     * Read ALL user data from Room and package it as a [SyncPullResponse] JSON-compatible
     * structure. This runs entirely offline — no network required.
     */
    private suspend fun buildSnapshot(userId: Long): SyncPullResponse = withContext(Dispatchers.IO) {
        val meals = mealDao.getAllMeals().first().map { meal ->
            val items = mealDao.getMealFoodItems(meal.id)
            MealDto(
                id = meal.id,
                serverId = meal.serverId,
                name = meal.name,
                items = items.map { item ->
                    MealFoodItemDto(
                        mealId = item.mealId,
                        foodId = item.foodId,
                        quantity = item.quantity,
                        unit = item.unit.name,
                        notes = item.notes
                    )
                },
                updatedAt = meal.updatedAt
            )
        }

        val diets = dietDao.getAllDietsOnce().map { diet ->
            val dietMeals = dietDao.getDietMeals(diet.id)
            DietDto(
                id = diet.id,
                serverId = diet.serverId,
                name = diet.name,
                description = diet.description,
                meals = dietMeals.map { dm ->
                    DietMealDto(
                        dietId = dm.dietId,
                        mealId = dm.mealId ?: 0L,
                        slot = dm.slotType,
                        instructions = dm.instructions
                    )
                },
                updatedAt = diet.updatedAt
            )
        }

        val metrics = healthMetricDao.getAllMetrics(userId).map { m ->
            HealthMetricDto(
                id = m.id,
                serverId = m.serverId,
                type = m.metricType ?: "",
                subType = m.subType,
                value = m.value,
                secondaryValue = m.secondaryValue,
                recordedAt = m.date,
                updatedAt = m.updatedAt
            )
        }

        val groceries = groceryDao.getListsByUser(userId).first().map { list ->
            val items = groceryDao.getItemsByList(list.id).first()
            GroceryListDto(
                id = list.id,
                serverId = list.serverId,
                name = list.name,
                items = items.map { gi ->
                    GroceryItemDto(
                        id = gi.id,
                        groceryListId = gi.listId,
                        foodId = gi.foodId,
                        name = gi.customName ?: "",
                        quantity = gi.quantity,
                        unit = gi.unit.name,
                        done = gi.isChecked
                    )
                },
                updatedAt = list.updatedAt
            )
        }

        SyncPullResponse(meals = meals, diets = diets, healthMetrics = metrics, groceryLists = groceries)
    }

    /**
     * Write a parsed backup snapshot directly into Room. Uses REPLACE strategy so
     * existing records are overwritten by the backup. Runs entirely offline.
     */
    private suspend fun restoreSnapshot(userId: Long, data: SyncPullResponse) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()

        // Restore meals
        data.meals.forEach { dto ->
            val mealId = mealDao.insertMeal(
                Meal(
                    id = dto.id,
                    name = dto.name,
                    serverId = dto.serverId,
                    updatedAt = dto.updatedAt ?: now
                )
            )
            val insertedMealId = if (dto.id != 0L) dto.id else mealId
            dto.items.forEach { item ->
                mealDao.insertMealFoodItem(
                    MealFoodItem(
                        mealId = insertedMealId,
                        foodId = item.foodId,
                        quantity = item.quantity,
                        unit = runCatching { FoodUnit.valueOf(item.unit) }.getOrDefault(FoodUnit.GRAM),
                        notes = item.notes
                    )
                )
            }
        }

        // Restore diets
        data.diets.forEach { dto ->
            val dietId = dietDao.insertDiet(
                Diet(
                    id = dto.id,
                    name = dto.name,
                    description = dto.description,
                    serverId = dto.serverId,
                    updatedAt = dto.updatedAt ?: now
                )
            )
            val insertedDietId = if (dto.id != 0L) dto.id else dietId
            dto.meals.forEach { dm ->
                dietDao.insertDietMeal(
                    DietMeal(
                        dietId = insertedDietId,
                        slotType = dm.slot,
                        mealId = dm.mealId.takeIf { it != 0L },
                        instructions = dm.instructions
                    )
                )
            }
        }

        // Restore health metrics
        data.healthMetrics.forEach { dto ->
            healthMetricDao.insertHealthMetric(
                HealthMetric(
                    id = dto.id,
                    userId = userId,
                    date = dto.recordedAt ?: now,
                    metricType = dto.type.takeIf { it.isNotEmpty() },
                    subType = dto.subType,
                    value = dto.value,
                    secondaryValue = dto.secondaryValue,
                    serverId = dto.serverId,
                    updatedAt = dto.updatedAt ?: now
                )
            )
        }

        // Restore grocery lists and items
        data.groceryLists.forEach { dto ->
            val listId = groceryDao.insertGroceryList(
                GroceryList(
                    id = dto.id,
                    userId = userId,
                    name = dto.name,
                    serverId = dto.serverId,
                    updatedAt = dto.updatedAt ?: now
                )
            )
            val insertedListId = if (dto.id != 0L) dto.id else listId
            dto.items.forEach { gi ->
                groceryDao.upsertItem(
                    GroceryItem(
                        id = gi.id,
                        listId = insertedListId,
                        foodId = gi.foodId,
                        customName = gi.name.takeIf { it.isNotEmpty() },
                        quantity = gi.quantity,
                        unit = runCatching { FoodUnit.valueOf(gi.unit) }.getOrDefault(FoodUnit.GRAM),
                        isChecked = gi.done
                    )
                )
            }
        }
    }

    // ── Private helpers ────────────────────────────────────────────────────────

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
