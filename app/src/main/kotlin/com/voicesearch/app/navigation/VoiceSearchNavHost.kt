package com.voicesearch.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.voicesearch.app.feature.imports.ui.ImportScreen
import com.voicesearch.app.feature.tablesettings.ui.TableSettingsScreen
import com.voicesearch.app.ui.home.HomeScreen

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
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onAddTable = { navController.navigate(Routes.IMPORT) },
                onOpenSettings = { tableId -> navController.navigate(Routes.settings(tableId)) },
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
    }
}
