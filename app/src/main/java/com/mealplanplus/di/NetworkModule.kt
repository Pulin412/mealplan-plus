package com.mealplanplus.di

import com.mealplanplus.data.remote.OpenFoodFactsApi
import com.mealplanplus.data.remote.UsdaFoodApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private const val OPEN_FOOD_FACTS_URL = "https://world.openfoodfacts.org/"
    private const val USDA_API_URL = "https://api.nal.usda.gov/"

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("User-Agent", "MealPlanPlus/1.0 Android")
                    .build()
                chain.proceed(request)
            }
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("OpenFoodFacts")
    fun provideOpenFoodFactsRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(OPEN_FOOD_FACTS_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    @Named("USDA")
    fun provideUsdaRetrofit(client: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(USDA_API_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideOpenFoodFactsApi(@Named("OpenFoodFacts") retrofit: Retrofit): OpenFoodFactsApi {
        return retrofit.create(OpenFoodFactsApi::class.java)
    }

    @Provides
    @Singleton
    fun provideUsdaFoodApi(@Named("USDA") retrofit: Retrofit): UsdaFoodApi {
        return retrofit.create(UsdaFoodApi::class.java)
    }
}
