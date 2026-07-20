package com.mealplanplus.di

import android.content.Context
import androidx.room.Room
import com.mealplanplus.data.local.AppDatabase
import com.mealplanplus.data.local.dao.FoodDao
import com.mealplanplus.data.local.dao.MealDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "mealplan_v2.db")
            // Destructive reset is intentional for the v2 offline-first rebuild: the local
            // DB is a throwaway cache that re-syncs from the server (source of truth).
            .fallbackToDestructiveMigration()
            .build()

    @Provides @Singleton
    fun provideFoodDao(db: AppDatabase): FoodDao = db.foodDao()

    @Provides @Singleton
    fun provideMealDao(db: AppDatabase): MealDao = db.mealDao()
}
