package com.vehiclesafetyguardian

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vehiclesafetyguardian.location.LocationManager
import com.vehiclesafetyguardian.ml.DetectionResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class MainViewModel @Inject constructor(
    private val locationManager: LocationManager
) : ViewModel() {

    private val _speedLimit = MutableLiveData<Int?>()
    val speedLimit: LiveData<Int?> = _speedLimit

    private val _currentSpeed = MutableLiveData<Float>()
    val currentSpeed: LiveData<Float> = _currentSpeed

    private val _showAlert = MutableLiveData<Boolean>()
    val showAlert: LiveData<Boolean> = _showAlert

    private val _detectionResults = MutableStateFlow<List<DetectionResult>>(emptyList())
    val detectionResults: StateFlow<List<DetectionResult>> = _detectionResults.asStateFlow()

    fun updateDetectionResults(results: List<DetectionResult>) {
        viewModelScope.launch {
            _detectionResults.emit(results)
            checkForAlerts(results)
        }
    }

    private fun checkForAlerts(results: List<DetectionResult>) {
        // Check for vehicles approaching rapidly
        val collisionRisk = results.any { result ->
            result is DetectionResult.Vehicle && 
            result.closingSpeed > 5.5f // 20 km/h in m/s
        }

        // Check for speed limit violations
        val speedViolation = _speedLimit.value?.let { limit ->
            _currentSpeed.value?.let { speed ->
                speed > limit
            }
        } ?: false

        _showAlert.postValue(collisionRisk || speedViolation)
    }

    fun startLocationUpdates() {
        locationManager.startLocationUpdates { location ->
            _currentSpeed.postValue(location.speed * 3.6f) // Convert m/s to km/h
        }
    }

    fun stopLocationUpdates() {
        locationManager.stopLocationUpdates()
    }
}