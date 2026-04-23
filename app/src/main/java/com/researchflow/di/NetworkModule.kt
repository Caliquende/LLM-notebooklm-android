package com.researchflow.di

import android.content.Context
import android.content.pm.ApplicationInfo
import com.researchflow.data.remote.BridgeApi
import com.researchflow.data.remote.SearchApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext
import com.researchflow.data.preferences.SettingsDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideMoshi(): Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun provideOkHttpClient(
        @ApplicationContext context: Context,
        settingsDataStore: SettingsDataStore
    ): OkHttpClient {
        val isDebug = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                // BUG-002: Dynamically resolve bridge URL from SettingsDataStore
                val request = chain.request()
                val currentBridgeUrl = runBlocking { settingsDataStore.bridgeUrl.first() }
                val newBaseUrl = currentBridgeUrl.toHttpUrlOrNull()
                if (newBaseUrl != null) {
                    val newUrl = request.url.newBuilder()
                        .scheme(newBaseUrl.scheme)
                        .host(newBaseUrl.host)
                        .port(newBaseUrl.port)
                        .build()
                    val newRequest = request.newBuilder()
                        .url(newUrl)
                        .build()
                    chain.proceed(newRequest)
                } else {
                    chain.proceed(request)
                }
            }
            .apply {
                // BUG-004: Use BASIC instead of BODY logging to avoid API key leakage in logs
                if (isDebug) {
                    addInterceptor(
                        HttpLoggingInterceptor().apply {
                            level = HttpLoggingInterceptor.Level.BASIC
                        }
                    )
                }
            }
            .build()
    }

    @Provides
    @Singleton
    fun provideBridgeApi(client: OkHttpClient, moshi: Moshi): BridgeApi {
        return Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8080/") // Android emulator → host localhost
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(BridgeApi::class.java)
    }

    @Provides
    @Singleton
    fun provideSearchApi(client: OkHttpClient, moshi: Moshi): SearchApi {
        return Retrofit.Builder()
            .baseUrl("http://10.0.2.2:8080/") // Same bridge server handles search
            .client(client)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(SearchApi::class.java)
    }
}
