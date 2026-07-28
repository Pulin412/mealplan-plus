package com.mealplanplus.data.export

import java.util.Locale

/**
 * Serialises an [ExportData] snapshot into a single, spreadsheet-friendly CSV with four labelled
 * sections (Meals, Diets, Workouts, Health). Pure and deterministic — no Android, no I/O — so the
 * formatting/escaping is covered by unit tests. Filtering (last-7-day workouts, last-90-day health)
 * is done upstream in [ExportRepository]; this just formats what it's given.
 *
 * Escaping follows RFC 4180: a field is quoted when it contains a comma, quote, or newline, and any
 * embedded quote is doubled. Numbers use [Locale.US] so the decimal point never becomes a comma.
 */
object CsvExporter {

    fun build(data: ExportData): String = buildString {
        append("# MEALS\n")
        append(headerLine("name", "slots", "kcal", "protein_g", "carbs_g", "fat_g", "items"))
        data.meals.forEach { m ->
            append(rowLine(m.name, m.slots.joinToString(";"), m.kcal.toString(), num(m.proteinG), num(m.carbsG), num(m.fatG), m.items))
        }

        append("\n# DIETS\n")
        append(headerLine("name", "tags", "kcal", "protein_g", "carbs_g", "fat_g", "entries"))
        data.diets.forEach { d ->
            append(rowLine(d.name, d.tags.joinToString(";"), d.kcal.toString(), num(d.proteinG), num(d.carbsG), num(d.fatG), d.entryCount.toString()))
        }

        append("\n# WORKOUTS (last 7 days)\n")
        append(headerLine("date", "workout", "exercise", "set", "reps", "weight_kg"))
        data.workoutSets.forEach { w ->
            append(rowLine(w.date, w.workout, w.exercise, w.setNumber.toString(), w.reps?.toString() ?: "", w.weightKg?.let { num(it) } ?: ""))
        }

        append("\n# HEALTH (last 90 days)\n")
        append(headerLine("type", "recorded_at", "value", "secondary", "unit"))
        data.health.forEach { h ->
            append(rowLine(h.type, h.recordedAt, num(h.value), h.secondaryValue?.let { num(it) } ?: "", h.unit))
        }
    }

    private fun headerLine(vararg cols: String): String = cols.joinToString(",") + "\n"

    private fun rowLine(vararg fields: String): String = fields.joinToString(",") { esc(it) } + "\n"

    private fun esc(v: String): String =
        if (v.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) "\"" + v.replace("\"", "\"\"") + "\""
        else v

    /** Round to one decimal; drop a trailing ".0" so whole numbers stay clean. */
    private fun num(d: Double): String {
        val r = Math.round(d * 10.0) / 10.0
        return if (r == Math.floor(r) && !r.isInfinite()) r.toLong().toString()
        else String.format(Locale.US, "%.1f", r)
    }
}
