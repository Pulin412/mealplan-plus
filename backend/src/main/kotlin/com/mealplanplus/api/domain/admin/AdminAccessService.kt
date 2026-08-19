package com.mealplanplus.api.domain.admin

import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service

/**
 * Decides who is an admin: the caller's Firebase-token email (carried on the authentication details by
 * [com.mealplanplus.api.filter.FirebaseTokenFilter]) must be on the `app.admin-emails` allowlist.
 * No schema, no custom claims — a config allowlist, matched case-insensitively.
 */
@Service
class AdminAccessService(
    @Value("\${app.admin-emails:}") adminEmailsCsv: String
) {
    private val adminEmails: Set<String> =
        adminEmailsCsv.split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()

    fun isAdmin(email: String?): Boolean =
        email != null && adminEmails.contains(email.trim().lowercase())

    /** Email of the currently authenticated caller, or null (the Firebase filter stores it on auth.details). */
    fun currentEmail(): String? =
        SecurityContextHolder.getContext().authentication?.details as? String

    fun isCurrentUserAdmin(): Boolean = isAdmin(currentEmail())
}
