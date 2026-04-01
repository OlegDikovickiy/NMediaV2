package ru.netology.nmedia.di

import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.messaging.FirebaseMessaging
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseMessaging(): FirebaseMessaging =
        FirebaseMessaging.getInstance()

    @Provides
    @Singleton
    fun provideGoogleApiAvailability(): GoogleApiAvailability =
        GoogleApiAvailability.getInstance()
}