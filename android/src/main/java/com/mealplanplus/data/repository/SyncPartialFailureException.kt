package com.mealplanplus.data.repository

/**
 * Thrown when push Step 1 (foods/meals/diets/metrics/groceries) succeeds but
 * Step 2 (daily logs) fails. The caller (SyncWorker) uses this to distinguish
 * a partial failure from a full push failure and can schedule a targeted retry.
 */
class SyncPartialFailureException(
    message: String,
    cause: Throwable
) : Exception(message, cause)
