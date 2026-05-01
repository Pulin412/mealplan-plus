package com.mealplanplus.ui.screens.backup

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*

data class DriveBackupEntry(
    val fileId: String,
    val name: String,
    val createdTime: String,
    val size: String
)

/** Thin wrapper around Drive REST API v3 using the user's Drive access token. */
object DriveHelper {

    private val client = OkHttpClient()
    private const val BASE = "https://www.googleapis.com/drive/v3"
    private const val UPLOAD_BASE = "https://www.googleapis.com/upload/drive/v3"
    private const val BOUNDARY = "mealplan_backup_boundary"

    suspend fun listBackups(token: String): List<DriveBackupEntry> = withContext(Dispatchers.IO) {
        // No q-filter and no orderBy — both can cause 400 on appDataFolder.
        // We own all files in appDataFolder so no filter needed; sort client-side.
        val url = "$BASE/files" +
                "?spaces=appDataFolder" +
                "&fields=files(id,name,createdTime,size)"
        val req = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .get()
            .build()
        val (code, body) = client.newCall(req).execute().use { Pair(it.code, it.body?.string() ?: "{}") }
        if (code !in 200..299) throw Exception("Drive list failed ($code): $body")
        val files = JSONObject(body).optJSONArray("files") ?: JSONArray()
        (0 until files.length())
            .map { i ->
                val f = files.getJSONObject(i)
                val rawCreatedTime = f.optString("createdTime")
                DriveBackupEntry(
                    fileId = f.getString("id"),
                    name = f.optString("name", "backup"),
                    createdTime = formatDriveDate(rawCreatedTime),
                    // Drive returns size as a JSON string for large files
                    size = formatSize(f.optString("size", "0").toLongOrNull() ?: 0L)
                )
            }
            .sortedByDescending { it.name }  // filename contains date: mealplan_backup_YYYY-MM-DD.json
    }

    /**
     * Multipart upload to appDataFolder.
     * Uses raw multipart/related body — the safest format for Drive API v3.
     */
    suspend fun uploadFile(token: String, name: String, json: String): String = withContext(Dispatchers.IO) {
        val metadata = """{"name":"$name","parents":["appDataFolder"]}"""
        val rawBody = "--$BOUNDARY\r\n" +
                "Content-Type: application/json; charset=UTF-8\r\n\r\n" +
                "$metadata\r\n" +
                "--$BOUNDARY\r\n" +
                "Content-Type: application/json\r\n\r\n" +
                "$json\r\n" +
                "--$BOUNDARY--"
        val req = Request.Builder()
            .url("$UPLOAD_BASE/files?uploadType=multipart&fields=id")
            .header("Authorization", "Bearer $token")
            .header("Content-Type", "multipart/related; boundary=$BOUNDARY")
            .post(rawBody.toRequestBody())
            .build()
        val (code, response) = client.newCall(req).execute().use { Pair(it.code, it.body?.string() ?: "{}") }
        if (code !in 200..299) throw Exception("Drive upload failed ($code): $response")
        JSONObject(response).optString("id", "")
    }

    suspend fun downloadFile(token: String, fileId: String): String = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("$BASE/files/$fileId?alt=media")
            .header("Authorization", "Bearer $token")
            .get()
            .build()
        client.newCall(req).execute().use { it.body?.string() ?: "" }
    }

    suspend fun deleteFile(token: String, fileId: String) = withContext(Dispatchers.IO) {
        val req = Request.Builder()
            .url("$BASE/files/$fileId")
            .header("Authorization", "Bearer $token")
            .delete()
            .build()
        client.newCall(req).execute().close()
    }

    private fun formatDriveDate(isoStr: String): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
                .also { it.timeZone = TimeZone.getTimeZone("UTC") }
            val date = sdf.parse(isoStr) ?: return isoStr
            SimpleDateFormat("d MMM yyyy, HH:mm", Locale.US).format(date)
        } catch (_: Exception) { isoStr }
    }

    private fun formatSize(bytes: Long): String = when {
        bytes < 1024L -> "${bytes}B"
        bytes < 1024L * 1024L -> "${bytes / 1024}KB"
        else -> "${bytes / (1024L * 1024L)}MB"
    }
}
