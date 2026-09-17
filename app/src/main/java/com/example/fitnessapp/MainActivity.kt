package com.example.fitnessapp

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fitnessapp.data.security.AppLockManager
import com.example.fitnessapp.theme.FitnessAppTheme
import com.example.fitnessapp.ui.security.AppLockScreen

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = FitnessApplication.instance.repository

        setContent {
            FitnessAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val isAppLockEnabled by repository.isAppLockEnabled.collectAsStateWithLifecycle()
                    val isUnlocked by AppLockManager.isUnlocked.collectAsStateWithLifecycle()

                    if (isAppLockEnabled && !isUnlocked) {
                        AppLockScreen(
                            onUnlockSuccess = {
                                AppLockManager.setUnlocked(true)
                            }
                        )
                    } else {
                        MainNavigation()
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        AppLockManager.onActivityStopped()
    }

    override fun onResume() {
        super.onResume()
        val isLockEnabled = FitnessApplication.instance.repository.isAppLockEnabled.value
        AppLockManager.onActivityResumed(isLockEnabled)
    }
}
