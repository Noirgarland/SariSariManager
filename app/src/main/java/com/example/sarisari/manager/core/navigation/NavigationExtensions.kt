package com.example.sarisari.manager.core.navigation

import androidx.navigation.NavController

/** Switches bottom-bar tabs the same way the bottom bar does, keeping each tab's state. */
fun NavController.navigateToTopLevel(route: Any) {
    navigate(route) {
        popUpTo<DashboardRoute> { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
