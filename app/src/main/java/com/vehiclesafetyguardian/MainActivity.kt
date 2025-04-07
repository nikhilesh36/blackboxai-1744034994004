package com.vehiclesafetyguardian

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.vehiclesafetyguardian.camera.CameraManager
import com.vehiclesafetyguardian.databinding.ActivityMainBinding
import com.vehiclesafetyguardian.ml.ObjectDetector
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var cameraManager: CameraManager
    private lateinit var objectDetector: ObjectDetector

    private val requiredPermissions = arrayOf(
        Manifest.permission.CAMERA,
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.RECORD_AUDIO
    )

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.all { it.value }) {
            initializeCamera()
        } else {
            // Handle permission denial
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (hasRequiredPermissions()) {
            initializeCamera()
        } else {
            permissionLauncher.launch(requiredPermissions)
        }

        setupObservers()
    }

    private fun hasRequiredPermissions(): Boolean {
        return requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun initializeCamera() {
        lifecycleScope.launch {
            cameraManager = CameraManager(this@MainActivity)
            objectDetector = ObjectDetector(this@MainActivity)
            
            val imageAnalysis = cameraManager.getImageAnalysisUseCase(
                Size(1280, 720),
                objectDetector.getAnalyzer()
            )

            cameraManager.startCamera(
                this@MainActivity,
                binding.cameraContainer,
                imageAnalysis
            )
        }
    }

    private fun setupObservers() {
        viewModel.speedLimit.observe(this) { limit ->
            binding.speedLimitText.text = limit?.toString() ?: "--"
        }

        viewModel.currentSpeed.observe(this) { speed ->
            binding.currentSpeedText.text = getString(R.string.speed_format, speed)
        }

        viewModel.showAlert.observe(this) { show ->
            binding.alertButton.visibility = if (show) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraManager.shutdown()
    }
}