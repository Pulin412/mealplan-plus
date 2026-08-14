package com.mealplanplus.di

import com.mealplanplus.data.generated.api.AssistantApi
import com.mealplanplus.data.generated.api.DashboardApi
import com.mealplanplus.data.generated.api.DietsApi
import com.mealplanplus.data.generated.api.ExercisesApi
import com.mealplanplus.data.generated.api.FeedbackApi
import com.mealplanplus.data.generated.api.FoodsApi
import com.mealplanplus.data.generated.api.HealthMetricsApi
import com.mealplanplus.data.generated.api.WorkoutTemplatesApi
import com.mealplanplus.data.generated.api.WorkoutSessionsApi
import com.mealplanplus.data.generated.api.LoggingApi
import com.mealplanplus.data.generated.api.MealsApi
import com.mealplanplus.data.generated.api.PlansApi
import com.mealplanplus.data.generated.api.SyncApi
import com.mealplanplus.data.generated.api.TagsApi
import com.mealplanplus.data.generated.api.UsersApi
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

    /** User profile (body metrics + targets + preferences). */
    @Provides @Singleton
    fun provideUsersApi(retrofit: Retrofit): UsersApi =
        retrofit.create(UsersApi::class.java)

    /** Social — follow graph, profiles, discovery, shared-library reads, copy (online-only, direct REST). */
    @Provides @Singleton
    fun provideSocialApi(retrofit: Retrofit): com.mealplanplus.data.generated.api.SocialApi =
        retrofit.create(com.mealplanplus.data.generated.api.SocialApi::class.java)

    /** Calendar day-plans (diet + planned workouts per date). */
    @Provides @Singleton
    fun providePlansApi(retrofit: Retrofit): PlansApi = retrofit.create(PlansApi::class.java)

    /** Online diet/meal lists — used where numeric server ids are needed (e.g. Plan). */
    @Provides @Singleton
    fun provideDietsApi(retrofit: Retrofit): DietsApi = retrofit.create(DietsApi::class.java)

    @Provides @Singleton
    fun provideMealsApi(retrofit: Retrofit): MealsApi = retrofit.create(MealsApi::class.java)

    /** Exercise library (server-backed CRUD). */
    @Provides @Singleton
    fun provideExercisesApi(retrofit: Retrofit): ExercisesApi = retrofit.create(ExercisesApi::class.java)

    /** Workout templates (server-backed CRUD; not in the offline sync contract). */
    @Provides @Singleton
    fun provideWorkoutTemplatesApi(retrofit: Retrofit): WorkoutTemplatesApi =
        retrofit.create(WorkoutTemplatesApi::class.java)

    /** Workout sessions (server-backed; read-only history for the Logs tab). */
    @Provides @Singleton
    fun provideWorkoutSessionsApi(retrofit: Retrofit): WorkoutSessionsApi =
        retrofit.create(WorkoutSessionsApi::class.java)

    /** Health metrics (server-backed; glucose / weight / blood pressure). */
    @Provides @Singleton
    fun provideHealthMetricsApi(retrofit: Retrofit): HealthMetricsApi =
        retrofit.create(HealthMetricsApi::class.java)

    /** In-app feedback submissions (server-backed). */
    @Provides @Singleton
    fun provideFeedbackApi(retrofit: Retrofit): FeedbackApi =
        retrofit.create(FeedbackApi::class.java)

    /** AI nutrition assistant — chat + read-only provider-chain status. */
    @Provides @Singleton
    fun provideAssistantApi(retrofit: Retrofit): AssistantApi =
        retrofit.create(AssistantApi::class.java)

    // FoodRepository, SyncManager, SyncCursorStore use @Inject constructors — Hilt provides them.
}
