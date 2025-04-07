package com.vehiclesafetyguardian.di

import android.content.Context
import com.vehiclesafetyguardian.location.LocationManager
import com.vehiclesafetyguardian.ml.ObjectDetector
import dagger.Module
import dagger.Provides
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [AppModule::class]
)
object TestAppModule {
    @Provides
    @Singleton
    fun provideLocationManager(context: Context): LocationManager {
        return mockk(relaxed = true)
    }

    @Provides
    @Singleton
    fun provideObjectDetector(context: Context): ObjectDetector {
        return mockk(relaxed = true)
    }
}