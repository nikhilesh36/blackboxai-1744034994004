package com.vehiclesafetyguardian

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.vehiclesafetyguardian.di.TestAppModule
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.UninstallModules
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@HiltAndroidTest
@UninstallModules(AppModule::class)
@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    var hiltRule = HiltAndroidRule(this)

    @Before
    fun setup() {
        hiltRule.inject()
    }

    @Test
    fun mainActivity_shouldDisplayCameraPreview() {
        ActivityScenario.launch(MainActivity::class.java)
        
        onView(withId(R.id.camera_container))
            .check(matches(isDisplayed()))
    }

    @Test
    fun mainActivity_shouldShowSpeedInfoCard() {
        ActivityScenario.launch(MainActivity::class.java)
        
        onView(withId(R.id.speed_card))
            .check(matches(isDisplayed()))
        
        onView(withId(R.id.current_speed_text))
            .check(matches(withText("0 km/h")))
    }
}