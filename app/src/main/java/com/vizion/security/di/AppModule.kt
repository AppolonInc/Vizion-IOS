package com.vizion.security.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Module Hilt pour les dépendances de base de l'application
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Fournit le Context de l'application pour l'injection
     */
    @Provides
    @Singleton
    fun provideApplicationContext(@ApplicationContext context: Context): Context {
        return context
    }
}