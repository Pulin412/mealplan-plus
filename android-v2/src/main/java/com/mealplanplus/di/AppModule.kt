package com.mealplanplus.di

import com.mealplanplus.data.generated.api.DashboardApi
import com.mealplanplus.data.generated.api.FoodsApi
import com.mealplanplus.data.local.dao.FoodDao
import com.mealplanplus.data.repository.FoodRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideFoodsApi(retrofit: Retrofit): FoodsApi =
        retrofit.create(FoodsApi::class.java)

    // Add more as screens are built:
    // MealsApi, DietsApi, ExercisesApi, WorkoutSessionsApi, PlansApi, LoggingApi, etc.
    @Provides @Singleton
    fun provideDashboardApi(retrofit: Retrofit): DashboardApi =
        retrofit.create(DashboardApi::class.java)

    @Provides @Singleton
    fun provideFoodRepository(dao: FoodDao, api: FoodsApi): FoodRepository =
        FoodRepository(dao, api)
}
