package com.vehiclesafetyguardian.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Process
import androidx.annotation.RequiresApi
import com.vehiclesafetyguardian.ml.ModelInterpreter
import java.util.concurrent.Executors
import java.util.concurrent.ThreadPoolExecutor

class OptimizationManager(private val context: Context) {

    fun optimizeAppPerformance() {
        adjustThreadPool()
        limitBackgroundProcesses()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            enableBatteryOptimizations()
        }
    }

    private fun adjustThreadPool() {
        val processorCount = Runtime.getRuntime().availableProcessors()
        val executor = Executors.newFixedThreadPool(processorCount.coerceAtMost(4)) as ThreadPoolExecutor
        ModelInterpreter.setExecutor(executor)
    }

    private fun limitBackgroundProcesses() {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            activityManager.setProcessStateSummary(byteArrayOf(1)) // Limit background state
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun enableBatteryOptimizations() {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
        if (!powerManager.isIgnoringBatteryOptimizations(context.packageName)) {
            // Prompt user to add to battery optimization whitelist
        }
    }

    companion object {
        fun getMemoryUsage(context: Context): String {
            val memoryInfo = ActivityManager.MemoryInfo()
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            activityManager.getMemoryInfo(memoryInfo)
            
            return "Used: ${memoryInfo.totalMem - memoryInfo.availMem} / Total: ${memoryInfo.totalMem}"
        }
    }
}