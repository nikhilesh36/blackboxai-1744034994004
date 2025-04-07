package com.vehiclesafetyguardian

import android.app.Application
import com.vehiclesafetyguardian.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class VehicleSafetyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@VehicleSafetyApplication)
            modules(appModule)
        }
    }
}