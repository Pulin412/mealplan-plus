package com.mealplanplus.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// Zero-billing policy:
// - OAuth provider: Google (Android) only
// - Do not add billable Firebase services in this module
@Module
@InstallIn(SingletonComponent::class)
object AuthModule
