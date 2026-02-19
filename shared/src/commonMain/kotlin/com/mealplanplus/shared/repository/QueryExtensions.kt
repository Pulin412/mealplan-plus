package com.mealplanplus.shared.repository

import app.cash.sqldelight.Query
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow

/**
 * Extension to convert SQLDelight Query to Flow<List<T>>
 */
fun <T : Any> Query<T>.asFlowList(): Flow<List<T>> {
    return this.asFlow().mapToList(Dispatchers.Default)
}
