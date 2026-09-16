package com.example.fitnessapp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Compare
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.fitnessapp.ui.bodyprogress.BodyProgressScreen
import com.example.fitnessapp.ui.dashboard.DashboardScreen
import com.example.fitnessapp.ui.onboarding.OnboardingScreen
import com.example.fitnessapp.ui.partner.PartnerScreen
import com.example.fitnessapp.ui.profile.ProfileSetupScreen
import com.example.fitnessapp.ui.scanner.FoodScannerScreen
import com.example.fitnessapp.ui.scanner.ManualFoodEntryDialog
import com.example.fitnessapp.ui.weight.WeightTrackerScreen

enum class BottomTab(val title: String, val icon: ImageVector) {
    DASHBOARD("Calories", Icons.Default.LocalFireDepartment),
    WEIGHT("BMI & Goal", Icons.Default.MonitorWeight),
    BODY_PROGRESS("Body Compare", Icons.Default.Compare),
    PARTNER_DUEL("Partner Duel", Icons.Default.EmojiEvents)
}

@Composable
fun MainNavigation() {
    val repo = FitnessApplication.instance.repository
    val hasCompletedOnboarding by repo.hasCompletedOnboarding.collectAsStateWithLifecycle()

    val initialNav = if (!hasCompletedOnboarding) OnboardingNav else DashboardNav
    val backStack = rememberNavBackStack(initialNav)

    var currentTab by remember { mutableStateOf(BottomTab.DASHBOARD) }
    var showManualEntryDialog by remember { mutableStateOf(false) }

    val currentTop = backStack.lastOrNull()
    val isFullScreen = currentTop == ScannerNav || currentTop == SettingsNav || currentTop == OnboardingNav

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<OnboardingNav> {
                OnboardingScreen(
                    onComplete = {
                        backStack.remove(OnboardingNav)
                        if (!backStack.contains(DashboardNav)) {
                            backStack.add(DashboardNav)
                        }
                    }
                )
            }

            entry<DashboardNav> {
                Scaffold(
                    bottomBar = {
                        FitnessBottomBar(
                            currentTab = currentTab,
                            onSelectTab = { currentTab = it }
                        )
                    }
                ) { paddingValues ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        when (currentTab) {
                            BottomTab.DASHBOARD -> DashboardScreen(
                                onNavigateToScanner = { backStack.add(ScannerNav) },
                                onNavigateToManualEntry = { showManualEntryDialog = true },
                                onNavigateToSettings = { backStack.add(SettingsNav) },
                                onNavigateToPartner = { currentTab = BottomTab.PARTNER_DUEL }
                            )
                            BottomTab.WEIGHT -> WeightTrackerScreen()
                            BottomTab.BODY_PROGRESS -> BodyProgressScreen()
                            BottomTab.PARTNER_DUEL -> PartnerScreen()
                        }
                    }

                    if (showManualEntryDialog) {
                        val activeProfile by repo.activeProfile.collectAsStateWithLifecycle()
                        ManualFoodEntryDialog(
                            userId = activeProfile.id,
                            onConfirm = {
                                repo.addMeal(it)
                                showManualEntryDialog = false
                            },
                            onDismiss = { showManualEntryDialog = false }
                        )
                    }
                }
            }

            entry<ScannerNav> {
                FoodScannerScreen(
                    onNavigateBack = { backStack.removeLastOrNull() }
                )
            }

            entry<SettingsNav> {
                ProfileSetupScreen(
                    onNavigateBack = { backStack.removeLastOrNull() },
                    onNavigateToOnboarding = { backStack.add(OnboardingNav) }
                )
            }
        }
    )
}

@Composable
private fun FitnessBottomBar(
    currentTab: BottomTab,
    onSelectTab: (BottomTab) -> Unit
) {
    NavigationBar {
        BottomTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = currentTab == tab,
                onClick = { onSelectTab(tab) },
                icon = { Icon(tab.icon, contentDescription = tab.title) },
                label = { Text(tab.title) }
            )
        }
    }
}
