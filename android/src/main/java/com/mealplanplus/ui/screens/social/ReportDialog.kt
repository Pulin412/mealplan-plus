package com.mealplanplus.ui.screens.social

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val REASONS = listOf("Spam", "Inappropriate", "Harassment", "Impersonation", "Other")

/**
 * Report a user or a shared item. Collects a reason (+ optional detail) and hands them back to the
 * caller, which POSTs the report. The backend records it for later moderation with no immediate
 * visible effect, so callers just confirm receipt.
 */
@Composable
fun ReportDialog(
    subject: String,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (reason: String, detail: String?) -> Unit,
) {
    var reason by remember { mutableStateOf<String?>(null) }
    var detail by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("Report $subject") },
        text = {
            Column {
                Text("Why are you reporting this?")
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    REASONS.forEach { r ->
                        FilterChip(selected = reason == r, onClick = { reason = r }, label = { Text(r) })
                    }
                }
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = detail,
                    onValueChange = { if (it.length <= 1000) detail = it },
                    placeholder = { Text("Add details (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !busy,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { reason?.let { onSubmit(it, detail.trim().ifBlank { null }) } },
                enabled = !busy && reason != null,
            ) { Text(if (busy) "Sending…" else "Report", fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("Cancel") } },
    )
}
