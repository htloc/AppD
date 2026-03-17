package com.kamenrider.simulator.view.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.kamenrider.simulator.common.manager.InputManager
import com.kamenrider.simulator.object3d.SceneController
import com.kamenrider.simulator.view.drivers.DriversScreen
import com.kamenrider.simulator.view.home.HomeScreen
import com.kamenrider.simulator.view.items.ItemsScreen
import com.kamenrider.simulator.view.show.ShowScreen

/**
 * Navigation routes – single source of truth.
 */
object Routes {
    const val HOME    = "home"
    const val DRIVERS = "drivers"
    const val SHOW    = "show/{driverId}"
    const val ITEMS   = "items/{driverId}"

    fun show(driverId: String)  = "show/$driverId"
    fun items(driverId: String) = "items/$driverId"
}

/**
 * Root NavHost wiring all screens to their routes.
 */
@Composable
fun AppNavigation(
    navController: NavHostController,
    inputManager: InputManager,
    sceneController: SceneController,
    onExit: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier
    ) {
        // ----- Home -------------------------------------------------------
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateToDrivers = { navController.navigate(Routes.DRIVERS) },
                onExit = onExit
            )
        }

        // ----- Drivers ----------------------------------------------------
        composable(Routes.DRIVERS) {
            DriversScreen(
                onNavigateToShow = { driverId ->
                    navController.navigate(Routes.show(driverId))
                },
                onBack = { navController.popBackStack() }
            )
        }

        // ----- Show (Main game) -------------------------------------------
        composable(
            route = Routes.SHOW,
            arguments = listOf(navArgument("driverId") { type = NavType.StringType })
        ) { backStackEntry ->
            val driverId = backStackEntry.arguments?.getString("driverId") ?: return@composable
            ShowScreen(
                driverId       = driverId,
                inputManager   = inputManager,
                sceneController = sceneController,
                onNavigateToItems = { id -> navController.navigate(Routes.items(id)) },
                onBack         = { navController.popBackStack() }
            )
        }

        // ----- Items ------------------------------------------------------
        composable(
            route = Routes.ITEMS,
            arguments = listOf(navArgument("driverId") { type = NavType.StringType })
        ) {
            ItemsScreen(
                onBack = { navController.popBackStack() },
                onItemInserted = { navController.popBackStack() }
            )
        }
    }
}
