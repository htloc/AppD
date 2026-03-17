package com.kamenrider.simulator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.kamenrider.simulator.common.manager.InputManager
import com.kamenrider.simulator.object3d.SceneController
import com.kamenrider.simulator.ui.theme.KamenRiderTheme
import com.kamenrider.simulator.view.navigation.AppNavigation
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Single-activity host.
 *
 * Hilt injects managers directly; they are passed down to the NavHost so
 * the composables have access without needing to retrieve them from a ViewModel
 * (managers are not ViewModel-scoped, they are app-scoped singletons).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var inputManager: InputManager

    @Inject
    lateinit var sceneController: SceneController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            KamenRiderTheme {
                val navController = rememberNavController()
                val systemUiController = rememberSystemUiController()

                // Force dark status bar icons to false (white icons on dark bg)
                androidx.compose.runtime.LaunchedEffect(Unit) {
                    systemUiController.setSystemBarsColor(
                        color = androidx.compose.ui.graphics.Color.Transparent,
                        darkIcons = false
                    )
                }

                AppNavigation(
                    navController = navController,
                    inputManager  = inputManager,
                    sceneController = sceneController,
                    onExit = { finish() }
                )
            }
        }
    }
}
