package com.mealplanplus.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Minutes + seconds input for a duration, stored/returned as total **seconds** (null when zero).
 * Two steppers so what you log — e.g. 5 min 4 sec — reads back exactly as 5:04, with none of the
 * decimal-minute ambiguity (5.4 "minutes" ≠ 5:04).
 */
@Composable
fun DurationInput(seconds: Int?, onChange: (Int?) -> Unit, modifier: Modifier = Modifier) {
    val total = (seconds ?: 0).coerceAtLeast(0)
    val mins = total / 60
    val secs = total % 60
    // Each Stepper spends ~80dp on its two ± buttons, so it needs a good bit more width for the
    // number + unit suffix to be legible (94dp clipped the value to nothing).
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Stepper(
            value = mins, onChange = { m -> onChange(((m.coerceAtLeast(0) * 60) + secs).takeIf { it > 0 }) },
            min = 0, max = 999, suffix = "m", modifier = Modifier.width(126.dp),
        )
        Spacer(Modifier.width(8.dp))
        Stepper(
            value = secs, onChange = { s -> onChange((mins * 60 + s.coerceIn(0, 59)).takeIf { it > 0 }) },
            min = 0, max = 59, step = 5, suffix = "s", modifier = Modifier.width(126.dp),
        )
    }
}
