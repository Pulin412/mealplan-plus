package com.mealplanplus.shared

/**
 * Platform interface - implemented differently on each platform
 */
interface Platform {
    val name: String
}

/**
 * Returns platform-specific implementation
 */
expect fun getPlatform(): Platform

/**
 * Greeting function to verify shared code works
 */
class Greeting {
    private val platform = getPlatform()

    fun greet(): String {
        return "Hello from ${platform.name}!"
    }
}
