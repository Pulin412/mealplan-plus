package com.mealplanplus.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

// Currently empty - local auth doesn't need additional providers
// Firebase/Google providers will be added here when enabling external auth
@Module
@InstallIn(SingletonComponent::class)
object AuthModule
