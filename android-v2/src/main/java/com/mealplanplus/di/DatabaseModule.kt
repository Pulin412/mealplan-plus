package com.mealplanplus.di

import android.content.Context
import androidx.room.Room
import com.mealplanplus.data.local.AppDatabase
import com.mealplanplus.data.local.MIGRATION_1_2
import com.mealplanplus.data.local.dao.FoodDao
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
            // No fallbackToDestructiveMigration — explicit MIGRATION_X_Y always required.
            .addMigrations(MIGRATION_1_2)
            .build()

    @Provides @Singleton
    fun provideFoodDao(db: AppDatabase): FoodDao = db.foodDao()
}
