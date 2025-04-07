package com.vehiclesafetyguardian.di

import com.vehiclesafetyguardian.camera.CameraManager
import com.vehiclesafetyguardian.location.LocationManager
import com.vehiclesafetyguardian.ml.ModelInterpreter
import org.koin.dsl.module

val appModule = module {
    single { CameraManager(get()) }
    single { LocationManager(get()) }
    single { ModelInterpreter(get()) }
    
    // Will add more dependencies as we implement features
    // including ViewModels, Repositories, etc.
}