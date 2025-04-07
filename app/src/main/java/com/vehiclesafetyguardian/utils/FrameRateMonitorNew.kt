package com.vehiclesafetyguardian.utils

import android.os.Handler
import android.os.Looper
import java.util.concurrent.atomic.AtomicInteger

/**
 * Monitors and reports frame rate in frames per second (FPS)
 * 
 * Usage:
 * 1. Create instance with callback: FrameRateMonitor { fps -> ... }
 * 2. Call start() to begin monitoring
 * 3. Call onFrameProcessed() for each frame
 * 4. Call stop() when done
 */
class FrameRateMonitor(private val callback: (fps: Int) -> Unit) {
    private val frameCount = AtomicInteger(0)
    private val handler = Handler(Looper.getMainLooper())
    private var isMonitoring = false

    private val monitorRunnable = object : Runnable {
        override fun run() {
            val fps = frameCount.getAndSet(0)
            callback(fps)
            if (isMonitoring) {
                handler.postDelayed(this, 1000L)
            }
        }
    }

    fun start() {
        if (!isMonitoring) {
            isMonitoring = true
            handler.post(monitorRunnable)
        }
    }

    fun stop() {
        isMonitoring = false
        handler.removeCallbacks(monitorRunnable)
    }

    fun onFrameProcessed() {
        frameCount.incrementAndGet()
    }

    fun isMonitoring(): Boolean = isMonitoring
}