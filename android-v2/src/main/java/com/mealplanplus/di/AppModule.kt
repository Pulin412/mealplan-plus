package com.mealplanplus.di

import com.mealplanplus.data.generated.api.DashboardApi
import com.mealplanplus.data.generated.api.FoodsApi
import com.mealplanplus.data.generated.api.LoggingApi
import com.mealplanplus.data.generated.api.SyncApi
import com.mealplanplus.data.generated.api.TagsApi
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

    /** Home/Today logging — toggle planned meal slots as consumed. */
    @Provides @Singleton
    fun provideLoggingApi(retrofit: Retrofit): LoggingApi =
        retrofit.create(LoggingApi::class.java)

    /** Delta-sync endpoints — the single network path for offline-first domain data. */
    @Provides @Singleton
    fun provideSyncApi(retrofit: Retrofit): SyncApi =
        retrofit.create(SyncApi::class.java)

    /** Tag directory (online-only): list/create tags for diets. */
    @Provides @Singleton
    fun provideTagsApi(retrofit: Retrofit): TagsApi =
        retrofit.create(TagsApi::class.java)

    // FoodRepository, SyncManager, SyncCursorStore use @Inject constructors — Hilt provides them.
}
