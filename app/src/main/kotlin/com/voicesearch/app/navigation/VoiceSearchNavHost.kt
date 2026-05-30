package com.voicesearch.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.voicesearch.app.feature.imports.ui.ImportScreen
import com.voicesearch.app.ui.home.HomeScreen
import com.voicesearch.app.ui.placeholder.PlaceholderScreen

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
                onOpenMenu = { /* stage 6 */ },
            )
        }
        composable(Routes.IMPORT) {
            ImportScreen(
                onBack = { navController.popBackStack() },
                onImportComplete = {
                    // Stage 3 will route to a real table settings screen.
                    navController.popBackStack()
                },
            )
        }
        composable(Routes.SETTINGS) {
            PlaceholderScreen(
                title = "Настройки таблицы",
                hint = "Появится на Этапе 3",
                onBack = { navController.popBackStack() },
            )
        }
    }
}
