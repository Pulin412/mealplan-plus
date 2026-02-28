package com.mealplanplus.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.mealplanplus.data.model.DailyLogWithFoods
import com.mealplanplus.data.model.HealthMetric
import com.mealplanplus.data.model.MetricType
import java.io.File
import java.io.FileWriter
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object CsvExporter {

    fun exportFoodLog(
        context: Context,
        logs: List<DailyLogWithFoods>,
        filename: String = "mealplan_food_log"
    ): Uri? {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val file = File(context.cacheDir, "${filename}_$timestamp.csv")

        try {
            FileWriter(file).use { writer ->
                // Header
                writer.append("Date,Slot,Food,Quantity,Calories,Protein(g),Carbs(g),Fat(g),Notes\n")

                // Data rows
                logs.sortedBy { it.log.date }.forEach { logWithFoods ->
                    logWithFoods.foods.forEach { food ->
                        writer.append(
                            "${logWithFoods.log.date}," +
                            "${food.loggedFood.slotType}," +
                            "\"${food.food.name}\"," +
                            "${food.loggedFood.quantity}," +
                            "${food.calculatedCalories.toInt()}," +
                            "${food.calculatedProtein.toInt()}," +
                            "${food.calculatedCarbs.toInt()}," +
                            "${food.calculatedFat.toInt()}," +
                            "\"${food.loggedFood.notes ?: ""}\"\n"
                        )
                    }
                }
            }
            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun exportHealthMetrics(
        context: Context,
        metrics: List<HealthMetric>,
        filename: String = "mealplan_health"
    ): Uri? {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
        val file = File(context.cacheDir, "${filename}_$timestamp.csv")

        try {
            FileWriter(file).use { writer ->
                // Header
                writer.append("Date,Type,Value,Unit,Notes\n")

                // Data rows
                metrics.sortedBy { it.date }.forEach { metric ->
                    val type = metric.metricType?.let {
                        try { MetricType.valueOf(it) } catch (e: Exception) { null }
                    }
                    val typeName = type?.displayName ?: "Custom"
                    val unit = type?.unit ?: ""

                    writer.append(
                        "${metric.date}," +
                        "\"$typeName\"," +
                        "${metric.value}," +
                        "\"$unit\"," +
                        "\"${metric.notes ?: ""}\"\n"
                    )
                }
            }
            return FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun shareFile(context: Context, uri: Uri, title: String = "Export Data") {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, title))
    }
}
