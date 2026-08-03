package com.mealplanplus.di

import android.content.Context
import androidx.room.Room
import com.mealplanplus.data.local.AppDatabase
import com.mealplanplus.data.local.MIGRATION_8_9
import com.mealplanplus.data.local.MIGRATION_9_10
import com.mealplanplus.data.local.dao.CachedResponseDao
import com.mealplanplus.data.local.dao.DietDao
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
            // Explicit migrations preserve local data (incl. not-yet-synced rows) across upgrades.
            .addMigrations(MIGRATION_8_9, MIGRATION_9_10)
            // Fallback only for unexpected version gaps with no registered path: the local DB is a
            // cache that re-syncs from the server (source of truth), so a reset is recoverable.
            .fallbackToDestructiveMigration()
            .build()

    @Provides @Singleton
    fun provideFoodDao(db: AppDatabase): FoodDao = db.foodDao()

    @Provides @Singleton
    fun provideMealDao(db: AppDatabase): MealDao = db.mealDao()

    @Provides @Singleton
    fun provideDietDao(db: AppDatabase): DietDao = db.dietDao()

    @Provides @Singleton
    fun provideCachedResponseDao(db: AppDatabase): CachedResponseDao = db.cachedResponseDao()
}
