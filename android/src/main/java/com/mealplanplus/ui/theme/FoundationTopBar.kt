package com.mealplanplus.ui.theme

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable

/** White minimalist top app bar — matches Home / Plan / Log foundation screens. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun minimalTopAppBarColors() = TopAppBarDefaults.topAppBarColors(
    containerColor = CardBg,
    titleContentColor = TextPrimary,
    navigationIconContentColor = TextSecondary,
    actionIconContentColor = TextSecondary
)
