package com.vehiclesafetyguardian.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import javax.inject.Inject

class LocationManager @Inject constructor(
    private val context: Context
) {
    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    private var locationCallback: LocationCallback? = null
    private var androidLocationListener: LocationListener? = null
    private var currentListener: ((Location) -> Unit)? = null

    @SuppressLint("MissingPermission")
    fun startLocationUpdates(callback: (Location) -> Unit) {
        currentListener = callback

        // Use FusedLocationProvider for best accuracy
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            1000 // 1 second interval
        ).apply {
            setMinUpdateDistanceMeters(5f)
            setWaitForAccurateLocation(true)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.lastOrNull()?.let { location ->
                    currentListener?.invoke(location)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback as LocationCallback,
            Looper.getMainLooper()
        )

        // Fallback to Android LocationManager
        val locationManager = ContextCompat.getSystemService(
            context,
            LocationManager::class.java
        ) as android.location.LocationManager

        androidLocationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                currentListener?.invoke(location)
            }

            override fun onStatusChanged(provider: String, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        locationManager.requestLocationUpdates(
            android.location.LocationManager.GPS_PROVIDER,
            1000,
            5f,
            androidLocationListener as LocationListener,
            Looper.getMainLooper()
        )
    }

    fun stopLocationUpdates() {
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        androidLocationListener?.let { listener ->
            val locationManager = ContextCompat.getSystemService(
                context,
                LocationManager::class.java
            ) as android.location.LocationManager
            locationManager.removeUpdates(listener)
        }
        currentListener = null
    }
}