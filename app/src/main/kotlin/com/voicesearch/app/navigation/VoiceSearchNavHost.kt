package com.voicesearch.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.voicesearch.app.feature.imports.ui.ImportScreen
import com.voicesearch.app.feature.tablesettings.ui.TableSettingsScreen
import com.voicesearch.app.ui.home.HomeScreen
import com.voicesearch.app.ui.onboarding.OnboardingScreen
import com.voicesearch.app.ui.onboarding.OnboardingViewModel
import com.voicesearch.app.ui.settings.SettingsScreen

/**
 * Top-level navigation graph.
 *
 * Routes are typed as constants in [Routes]. As feature modules land
 * (import, settings, search BottomSheet), they register their own destinations here.
 *
 * For Stage 0 we have only Home + a generic Placeholder for routes that will be
 * filled in later stages — this lets us wire up navigation without dead links.
 */
@Composable
fun VoiceSearchNavHost(
    navController: NavHostController = rememberNavController(),
) {
    // Pick start destination based on the persisted "seen onboarding" flag.
    // Null = first emission pending → stay on HOME (safe default); the flag
    // is materialised in DataStore on first write, and subsequent launches
    // resolve cleanly. New installs hit ONBOARDING.
    val onboardingVm: OnboardingViewModel = hiltViewModel()
    val seen by onboardingVm.hasSeenOnboarding.collectAsStateWithLifecycle()
    val start = when (seen) {
        false -> Routes.ONBOARDING
        else -> Routes.HOME
    }
    NavHost(
        navController = navController,
        startDestination = start,
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onAddTable = { navController.navigate(Routes.IMPORT) },
                onOpenSettings = { tableId -> navController.navigate(Routes.settings(tableId)) },
                onOpenAppSettings = { navController.navigate(Routes.APP_SETTINGS) },
            )
        }
        composable(Routes.IMPORT) {
            ImportScreen(
                onBack = { navController.popBackStack() },
                onImportComplete = { tableId ->
                    // Drop Import off the back stack so back-from-Settings goes Home, not Import.
                    navController.navigate(Routes.settings(tableId)) {
                        popUpTo(Routes.HOME)
                    }
                },
            )
        }
        composable(
            route = Routes.SETTINGS_PATTERN,
            arguments = listOf(navArgument(Routes.ARG_TABLE_ID) { type = NavType.StringType }),
        ) {
            TableSettingsScreen(
                onBack = { navController.popBackStack() },
                onSaved = {
                    navController.popBackStack(route = Routes.HOME, inclusive = false)
                },
            )
        }
        composable(Routes.APP_SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }
    }
}
