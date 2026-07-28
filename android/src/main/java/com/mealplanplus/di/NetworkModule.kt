package com.mealplanplus.di

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.mealplanplus.BuildConfig
import com.mealplanplus.data.remote.OpenFoodFactsApi
import com.mealplanplus.data.remote.apiGson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides @Singleton
    fun provideOkHttpClient(
        auth: FirebaseAuth,
        @ApplicationContext context: Context,
    ): OkHttpClient =
        OkHttpClient.Builder()
            // Cloud Run scales to zero; the first request after idle pays a cold start (~5-10s+).
            // OkHttp's 10s default read timeout is too short → first call times out. Give it room.
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            // HTTP response cache: repeat GETs served from disk for a short window (snappy
            // re-navigation) and, when offline, served stale so screens still render.
            .cache(Cache(File(context.cacheDir, "http_cache"), HTTP_CACHE_BYTES))
            // Offline: serve a cached response (up to a week old) instead of failing.
            .addInterceptor { chain ->
                val request = chain.request()
                if (request.method == "GET" && !context.isOnline()) {
                    chain.proceed(
                        request.newBuilder()
                            .header("Cache-Control", "public, only-if-cached, max-stale=$OFFLINE_MAX_STALE")
                            .build()
                    )
                } else chain.proceed(request)
            }
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
            // Force a short cache window on GETs. The backend (Spring Security) sends no-store,
            // which would forbid caching, so we override it for our own private on-device cache.
            // Kept short so a just-written change isn't hidden for long.
            .addNetworkInterceptor { chain ->
                val response = chain.proceed(chain.request())
                if (chain.request().method == "GET") {
                    response.newBuilder()
                        .removeHeader("Pragma")
                        .header("Cache-Control", "private, max-age=$ONLINE_MAX_AGE")
                        .build()
                } else response
            }
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

    private const val HTTP_CACHE_BYTES = 5L * 1024 * 1024        // 5 MB on-device response cache
    private const val ONLINE_MAX_AGE = 5                         // seconds a cached GET is reused while online
    private const val OFFLINE_MAX_STALE = 7 * 24 * 60 * 60       // serve cache up to a week old when offline

    private fun Context.isOnline(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork ?: return false) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
