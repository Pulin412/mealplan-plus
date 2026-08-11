package com.mealplanplus.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/**
 * Confirmation shown when the user tries to leave a create/edit screen with unsaved changes.
 *
 * - **Save** runs the caller's normal save (validation included); it's disabled when the entity
 *   isn't valid yet ([canSave] = false), so we never persist a half-filled record.
 * - **Discard** leaves without saving.
 * - **Keep editing** (and tap-outside / back) stays on the screen — for an accidental tap.
 */
@Composable
fun UnsavedChangesDialog(
    canSave: Boolean,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Save changes?") },
        text = {
            Text(
                if (canSave) "You have unsaved changes. Save them before leaving?"
                else "You have unsaved changes, but they can't be saved yet.",
            )
        },
        confirmButton = { TextButton(enabled = canSave, onClick = onSave) { Text("Save") } },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onDiscard) { Text("Discard") }
                TextButton(onClick = onDismiss) { Text("Keep editing") }
            }
        },
    )
}
