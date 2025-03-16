package com.yae.torrenthelper

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import com.google.accompanist.permissions.rememberPermissionState
import com.yae.torrenthelper.ui.screen.SettingsScreen
import com.yae.torrenthelper.ui.screen.SettingsScreenViewModel
import com.yae.torrenthelper.ui.theme.TorrentHelperTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TorrHelperApp()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun TorrHelperApp() {

    // request permission
    val storagePermission = rememberMultiplePermissionsState(
        listOf(
            android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        )
    )

    if (storagePermission.allPermissionsGranted) {
        AppNavigation()
    } else {
        Column {
            val textToShow = if (storagePermission.shouldShowRationale) {
                // If the user has denied the permission but the rationale can be shown,
                // then gently explain why the app requires this permission
                "Provide!!!."
            } else {
                // If it's the first time the user lands on this feature, or the user
                // doesn't want to be asked again for this permission, explain that the
                // permission is required
                "Provide storage permission"
            }
            Text(textToShow)
            Button(onClick = { storagePermission.launchMultiplePermissionRequest() }) {
                Text("Request permission")
            }
        }
    }


}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val backStackEntry  by navController.currentBackStackEntryAsState()
    val currentScreen by remember {
        mutableStateOf(HomeDest)
    }
    var showSettingsAppBarIcon by remember {
        mutableStateOf(true)
    }
    if (backStackEntry?.destination?.route ?:"" == SettingsDest.route) {
        showSettingsAppBarIcon = false
    } else {
        showSettingsAppBarIcon = true
    }
    TorrentHelperTheme {
        Scaffold(
            topBar = {
                TopAppBar(title = {
                    Text(text = "Torrent helper")

                }, actions={
                    if(showSettingsAppBarIcon) {
                        IconButton(onClick = { navController.navigate(SettingsDest.route) }) {
                            Icon(imageVector = Icons.Filled.Settings, contentDescription = "Settings")
                        }
                    }
                })
            }
        ) {
            NavHost(navController = navController, startDestination = HomeDest.route,
                modifier = Modifier.padding(it)) {

                composable(route=HomeDest.route) {
                    HomeDest.screen()
                }
                composable(route=SettingsDest.route) {
                    val settingsVm = hiltViewModel<SettingsScreenViewModel>()
                    SettingsScreen(vm = settingsVm)
                }
            }
        }
    }
}


