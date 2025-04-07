package com.vehiclesafetyguardian.location

import android.content.Context
import android.location.Location
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.nhaarman.mockitokotlin2.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.Mock
import org.mockito.MockitoAnnotations

@RunWith(AndroidJUnit4::class)
class LocationManagerTest {
    @Mock lateinit var mockContext: Context
    @Mock lateinit var mockFusedClient: FusedLocationProviderClient
    @Mock lateinit var mockCallback: (Location) -> Unit
    
    private lateinit var locationManager: LocationManager

    @Before
    fun setup() {
        MockitoAnnotations.initMocks(this)
        locationManager = LocationManager(mockContext).apply {
            fusedLocationClient = mockFusedClient
        }
    }

    @Test
    fun `startLocationUpdates should request location updates`() {
        locationManager.startLocationUpdates(mockCallback)
        
        val captor = ArgumentCaptor.forClass(LocationCallback::class.java)
        verify(mockFusedClient).requestLocationUpdates(
            any<LocationRequest>(),
            captor.capture(),
            any()
        )
        
        // Simulate location update
        val testLocation = Location("test").apply {
            latitude = 37.422
            longitude = -122.084
            speed = 10f // 10 m/s
        }
        captor.value.onLocationResult(LocationResult.create(listOf(testLocation)))
        
        verify(mockCallback).invoke(testLocation)
    }

    @Test
    fun `stopLocationUpdates should remove updates`() {
        locationManager.startLocationUpdates(mockCallback)
        locationManager.stopLocationUpdates()
        
        verify(mockFusedClient).removeLocationUpdates(any<LocationCallback>())
    }
}