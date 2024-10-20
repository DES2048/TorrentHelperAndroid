package com.yae.torrenthelper

import androidx.compose.runtime.Composable
import com.yae.torrenthelper.ui.screen.HomeScreen
import com.yae.torrenthelper.ui.screen.SettingsScreen

interface NavDestination {
    val route: String
    val screen: @Composable ()->Unit
}

object HomeDest: NavDestination {
    override val route = "home"
    override val screen: @Composable () ->Unit= { HomeScreen() }
}

object SettingsDest: NavDestination {
    override val route = "settings"
    override val screen: @Composable () -> Unit = { }

}