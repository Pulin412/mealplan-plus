package com.mealplanplus.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import com.mealplanplus.ui.theme.Ink
import com.mealplanplus.ui.theme.Teal

/**
 * A subtle note affordance: renders nothing when [note] is blank; otherwise a small underlined
 * "details" link that opens a dialog with the full text. Used on list/summary rows (meals, diets,
 * the choose-a-diet picker, Home) so a note is discoverable without opening the editor.
 */
@Composable
fun NoteBadge(note: String?, modifier: Modifier = Modifier, label: String = "details") {
    val text = note?.trim().orEmpty()
    if (text.isEmpty()) return
    var open by remember { mutableStateOf(false) }
    Text(
        label,
        fontSize = 10.5.sp,
        color = Teal,
        textDecoration = TextDecoration.Underline,
        modifier = modifier.clickable { open = true },
    )
    if (open) {
        AlertDialog(
            onDismissRequest = { open = false },
            confirmButton = { TextButton(onClick = { open = false }) { Text("Close") } },
            title = { Text("Note", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Ink) },
            text = { Text(text, fontSize = 13.sp, color = Ink) },
        )
    }
}
