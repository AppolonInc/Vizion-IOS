package com.vizion.security.di

import android.content.Context
import com.vizion.security.BuildConfig
import com.vizion.security.data.local.datastore.SecurePreferences
import com.vizion.security.data.remote.VizionApiService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    
    private const val BASE_URL = "https://appollon-inc.com/api/"
    
    @Singleton
    @Provides
    fun provideHttpLoggingInterceptor(): HttpLoggingInterceptor {
        return HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.BASIC
            }
        }
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(securePreferences: SecurePreferences): Interceptor {
        return Interceptor { chain ->
            val originalRequest = chain.request()

            val needsAuth = !originalRequest.url.encodedPath.contains("/mobile-login/") &&
                    !originalRequest.url.encodedPath.contains("/register/")

            if (needsAuth) {
                val accessToken = securePreferences.getString("access_token")

                if (!accessToken.isNullOrEmpty()) {
                    val authenticatedRequest = originalRequest.newBuilder()
                        .header("Authorization", "Bearer $accessToken")
                        .build()

                    chain.proceed(authenticatedRequest)
                } else {
                    chain.proceed(originalRequest)
                }
            } else {
                chain.proceed(originalRequest)
            }
        }
    }

    // GARDEZ SEULEMENT cette version complète de provideOkHttpClient
    @Provides
    @Singleton
    fun provideOkHttpClient(
        loggingInterceptor: HttpLoggingInterceptor,
        authInterceptor: Interceptor
    ): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .retryOnConnectionFailure(true)
            .build()
    }

    // GARDEZ SEULEMENT cette version complète de provideRetrofit
    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideVizionApiService(retrofit: Retrofit): VizionApiService {
        return retrofit.create(VizionApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideSecurePreferences(@ApplicationContext context: Context): SecurePreferences {
        return SecurePreferences(context)
    }
}