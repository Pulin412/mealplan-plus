package com.mealplanplus.data.local

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Imports diet/meal data from CSV files.
 *
 * Expected CSV format (flat structure, one row per food item):
 * diet_name,diet_description,tag,slot,meal_name,food,quantity,unit
 *
 * Example:
 * Diet-1,Healthy breakfast diet,REMISSION,BREAKFAST,Omelet,Egg Whole,2,piece
 * Diet-1,Healthy breakfast diet,REMISSION,BREAKFAST,Omelet,Butter,12,g
 * Diet-1,Healthy breakfast diet,REMISSION,LUNCH,Fruit Bowl,Apple,100,g
 */
@Singleton
class CsvDataImporter @Inject constructor(
    private val jsonDataImporter: JsonDataImporter
) {
    private val gson = Gson()
    private val TAG = "CsvDataImporter"

    // CSV column indices
    private companion object {
        const val COL_DIET_NAME = 0
        const val COL_DIET_DESC = 1
        const val COL_TAG = 2
        const val COL_SLOT = 3
        const val COL_MEAL_NAME = 4
        const val COL_FOOD = 5
        const val COL_QUANTITY = 6
        const val COL_UNIT = 7
        const val MIN_COLUMNS = 8
    }

    data class CsvRow(
        val dietName: String,
        val dietDescription: String?,
        val tag: String?,
        val slot: String,
        val mealName: String,
        val food: String,
        val quantity: Double,
        val unit: String
    )

    /**
     * Import diets from a CSV file URI
     */
    suspend fun importFromUri(
        context: Context,
        uri: Uri,
        userId: Long,
        strategy: ImportStrategy = ImportStrategy.SKIP_DUPLICATES
    ): ImportResult = withContext(Dispatchers.IO) {
        try {
            // Read and parse CSV
            val rows = readCsvFromUri(context, uri)
            if (rows.isEmpty()) {
                return@withContext ImportResult(false, errorMessage = "No valid data rows found in CSV")
            }

            Log.d(TAG, "Parsed ${rows.size} rows from CSV")

            // Convert to JSON format
            val json = convertToJson(rows)
            Log.d(TAG, "Converted CSV to JSON")

            // Use existing JSON importer
            jsonDataImporter.importFromJson(json, userId, strategy)
        } catch (e: Exception) {
            Log.e(TAG, "CSV import failed: ${e.message}", e)
            ImportResult(false, errorMessage = e.message ?: "CSV import failed")
        }
    }

    private fun readCsvFromUri(context: Context, uri: Uri): List<CsvRow> {
        val rows = mutableListOf<CsvRow>()

        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BufferedReader(InputStreamReader(inputStream)).use { reader ->
                var lineNumber = 0
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    lineNumber++

                    // Skip header row (check for common header names)
                    if (lineNumber == 1 && isHeaderRow(line!!)) {
                        Log.d(TAG, "Skipping header row")
                        continue
                    }

                    // Skip empty lines
                    if (line.isNullOrBlank()) continue

                    try {
                        val row = parseCsvLine(line!!)
                        if (row != null) {
                            rows.add(row)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Skipping invalid row $lineNumber: ${e.message}")
                    }
                }
            }
        }

        return rows
    }

    private fun isHeaderRow(line: String): Boolean {
        val lower = line.lowercase()
        return lower.contains("diet_name") ||
                lower.contains("diet name") ||
                lower.contains("slot") ||
                lower.contains("meal_name") ||
                lower.contains("food") && lower.contains("quantity")
    }

    private fun parseCsvLine(line: String): CsvRow? {
        // Handle both comma and tab delimiters
        val delimiter = if (line.contains('\t')) '\t' else ','
        val parts = parseCsvFields(line, delimiter)

        if (parts.size < MIN_COLUMNS) {
            Log.w(TAG, "Row has ${parts.size} columns, need $MIN_COLUMNS")
            return null
        }

        val dietName = parts[COL_DIET_NAME].trim()
        if (dietName.isBlank()) return null

        val quantity = parts[COL_QUANTITY].trim().toDoubleOrNull()
        if (quantity == null || quantity <= 0) {
            Log.w(TAG, "Invalid quantity: ${parts[COL_QUANTITY]}")
            return null
        }

        return CsvRow(
            dietName = dietName,
            dietDescription = parts[COL_DIET_DESC].trim().takeIf { it.isNotBlank() },
            tag = parts[COL_TAG].trim().uppercase().takeIf { it.isNotBlank() },
            slot = normalizeSlot(parts[COL_SLOT].trim()),
            mealName = parts[COL_MEAL_NAME].trim(),
            food = parts[COL_FOOD].trim(),
            quantity = quantity,
            unit = parts[COL_UNIT].trim().ifBlank { "g" }
        )
    }

    /**
     * Parse CSV line handling quoted fields with commas
     */
    private fun parseCsvFields(line: String, delimiter: Char): List<String> {
        val fields = mutableListOf<String>()
        val currentField = StringBuilder()
        var inQuotes = false

        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char == delimiter && !inQuotes -> {
                    fields.add(currentField.toString())
                    currentField.clear()
                }
                else -> currentField.append(char)
            }
        }
        fields.add(currentField.toString())

        return fields
    }

    private fun normalizeSlot(slot: String): String {
        return when (slot.uppercase()) {
            "BREAKFAST", "BF", "B" -> "BREAKFAST"
            "LUNCH", "L" -> "LUNCH"
            "DINNER", "D" -> "DINNER"
            "NOON", "MIDMORNING", "MID-MORNING", "SNACK1" -> "NOON"
            "EVENING", "EVE", "SNACK2", "SNACK" -> "EVENING"
            else -> slot.uppercase()
        }
    }

    /**
     * Convert CSV rows to seed_data.json format
     */
    private fun convertToJson(rows: List<CsvRow>): String {
        // Group by diet
        val dietGroups = rows.groupBy { it.dietName }

        val diets = dietGroups.map { (dietName, dietRows) ->
            // Get diet-level info from first row
            val firstRow = dietRows.first()

            // Group by slot -> meal
            val mealsBySlot = dietRows.groupBy { it.slot }
                .mapValues { (_, slotRows) ->
                    // For each slot, group by meal name (in case multiple meals per slot)
                    // Take the first meal name for the slot
                    val mealName = slotRows.first().mealName
                    val items = slotRows.map { row ->
                        mapOf(
                            "food" to row.food,
                            "quantity" to row.quantity,
                            "unit" to row.unit
                        )
                    }
                    mapOf(
                        "name" to mealName,
                        "items" to items
                    )
                }

            mapOf(
                "name" to dietName,
                "description" to firstRow.dietDescription,
                "meal_type" to firstRow.tag,
                "meals" to mealsBySlot
            )
        }

        val seedData = mapOf("diets" to diets)
        return gson.toJson(seedData)
    }
}
