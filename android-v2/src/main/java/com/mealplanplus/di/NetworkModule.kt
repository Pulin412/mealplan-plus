package com.mealplanplus.di

import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.mealplanplus.BuildConfig
import com.mealplanplus.data.remote.OpenFoodFactsApi
import com.mealplanplus.data.remote.apiGson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    fun provideOkHttpClient(auth: FirebaseAuth): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                // Await the token — `.result` throws when the Task isn't already resolved
                // (e.g. sync firing on app start), which silently dropped the auth header.
                // Blocking is fine here: interceptors run on OkHttp's background threads.
                val token = runCatching {
                    auth.currentUser?.let { Tasks.await(it.getIdToken(false)) }?.token
                }.getOrNull()
                val request = if (token != null)
                    chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer $token")
                        .build()
                else chain.request()
                chain.proceed(request)
            }
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                        else HttpLoggingInterceptor.Level.NONE
            })
            .build()

    @Provides @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(apiGson()))
            .build()

    @Provides @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    /** Open Food Facts client — its own Retrofit (public API, NO Firebase auth header leaked to them). */
    @Provides @Singleton
    fun provideOpenFoodFactsApi(): OpenFoodFactsApi {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                // OFF asks callers to identify themselves via User-Agent.
                chain.proceed(chain.request().newBuilder().header("User-Agent", "MealPlanPlus/2.0 (Android)").build())
            }
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
            })
            .build()
        return Retrofit.Builder()
            .baseUrl("https://world.openfoodfacts.org/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenFoodFactsApi::class.java)
    }
}
