package com.vehiclesafetyguardian.utils

import android.os.Handler
import android.os.Looper
import java.util.concurrent.atomic.AtomicInteger

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

    /**
     * Starts monitoring frame rate
     */
    fun start() {
        if (!isMonitoring) {
            isMonitoring = true
            handler.post(monitorRunnable)
        }
    }

    /**
     * Stops monitoring frame rate
     */
    fun stop() {
        isMonitoring = false
        handler.removeCallbacks(monitorRunnable)
    }

    /**
     * Call this for each frame processed to update FPS count
     */
    fun onFrameProcessed() {
        frameCount.incrementAndGet()
    }

    /**
     * Gets current monitoring status
     */
    fun isMonitoring(): Boolean = isMonitoring
}