package com.vehiclesafetyguardian.analytics

import android.content.Context
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.vehiclesafetyguardian.ml.DetectionResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FirebaseManager @Inject constructor(
    private val context: Context
) {
    private val firebaseAnalytics: FirebaseAnalytics by lazy {
        FirebaseAnalytics.getInstance(context)
    }

    private val crashlytics: FirebaseCrashlytics by lazy {
        FirebaseCrashlytics.getInstance()
    }

    fun logDetectionEvent(result: DetectionResult) {
        val params = when (result) {
            is DetectionResult.Vehicle -> mapOf(
                "type" to "vehicle",
                "confidence" to result.confidence,
                "closing_speed" to result.closingSpeed
            )
            is DetectionResult.Person -> mapOf(
                "type" to "pedestrian",
                "confidence" to result.confidence
            )
            else -> emptyMap()
        }
        firebaseAnalytics.logEvent("object_detected", params.toBundle())
    }

    fun logSpeedViolation(currentSpeed: Float, speedLimit: Int) {
        val params = mapOf(
            "current_speed" to currentSpeed,
            "speed_limit" to speedLimit,
            "overspeed_by" to (currentSpeed - speedLimit)
        )
        firebaseAnalytics.logEvent("speed_violation", params.toBundle())
    }

    fun logLaneDeparture() {
        firebaseAnalytics.logEvent("lane_departure", null)
    }

    fun logCriticalError(error: Throwable, message: String) {
        crashlytics.recordException(error)
        crashlytics.log(message)
    }

    fun setUserProperties(userId: String) {
        crashlytics.setUserId(userId)
        firebaseAnalytics.setUserId(userId)
    }
}

private fun Map<String, Any>.toBundle(): Bundle {
    return Bundle().apply {
        forEach { (key, value) ->
            when (value) {
                is String -> putString(key, value)
                is Int -> putInt(key, value)
                is Float -> putFloat(key, value)
                is Double -> putDouble(key, value)
                is Boolean -> putBoolean(key, value)
            }
        }
    }
}