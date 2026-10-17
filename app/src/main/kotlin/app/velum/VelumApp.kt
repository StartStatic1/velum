package app.velum

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.velum.core.designsystem.VelumInk
import app.velum.feature.details.DetailsRoute
import app.velum.feature.home.HomeRoute
import app.velum.feature.player.PlayerRoute
import app.velum.feature.player.newPlaybackProgress

@Composable
fun VelumApp() {
    val context = LocalContext.current
    val progress = remember { newPlaybackProgress(context) }
    val navController = rememberNavController()

    Scaffold(
        containerColor = VelumInk,
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            composable("home") {
                HomeRoute(
                    onOpenDetails = { id -> navController.navigate("details/$id") },
                    onPlay = { id -> navController.navigate("player/$id") },
                )
            }
            composable(
                route = "details/{titleId}",
                arguments = listOf(navArgument("titleId") { type = NavType.StringType }),
            ) { entry ->
                val titleId = entry.arguments?.getString("titleId").orEmpty()
                DetailsRoute(
                    titleId = titleId,
                    progress = progress,
                    onBack = { navController.popBackStack() },
                    onPlay = { id -> navController.navigate("player/$id") },
                )
            }
            composable(
                route = "player/{titleId}",
                arguments = listOf(navArgument("titleId") { type = NavType.StringType }),
            ) { entry ->
                val titleId = entry.arguments?.getString("titleId").orEmpty()
                PlayerRoute(
                    titleId = titleId,
                    progress = progress,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
