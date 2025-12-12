package com.autoaction.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.autoaction.ui.screen.ScriptEditorScreen
import com.autoaction.ui.screen.ScriptListScreen
import com.autoaction.ui.screen.SettingsScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = "script_list") {
        composable("script_list") {
            ScriptListScreen(
                onNavigateToEditor = { scriptId ->
                    if (scriptId != null) {
                        navController.navigate("script_editor/$scriptId")
                    } else {
                        navController.navigate("script_editor/new")
                    }
                },
                onNavigateToSettings = {
                    navController.navigate("settings")
                }
            )
        }
        composable(
            "script_editor/{scriptId}",
            arguments = listOf(navArgument("scriptId") { type = NavType.StringType })
        ) { backStackEntry ->
            val scriptId = backStackEntry.arguments?.getString("scriptId")
            ScriptEditorScreen(
                scriptId = if (scriptId == "new") null else scriptId,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("settings") {
            SettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
