package com.yae.torrenthelper.ui.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.core.DataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yae.torrenthelper.TestSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.format.TextStyle
import javax.inject.Inject

data class TestSettingsState(val backendURL:String, val backendUser:String, val backendPasword:String) {
    companion object {
        val Default:TestSettingsState = TestSettings.getDefaultInstance().run {
            TestSettingsState(backendURL, backendUser, backendPassword)
        }
    }
}

@HiltViewModel
class SettingsScreenViewModel
    @Inject constructor(private val testSettingsDataStore:DataStore<TestSettings>) :ViewModel() {
    private val data = testSettingsDataStore.data
    val state = data.map { settings->TestSettingsState(settings.backendURL, settings.backendUser,
        settings.backendPassword) }

    fun saveSettings(settings:TestSettingsState) {
        viewModelScope.launch {
            testSettingsDataStore.updateData {
                it.toBuilder().setBackendURL(settings.backendURL)
                    .setBackendUser(settings.backendUser)
                    .setBackendPassword(settings.backendPasword)
                    .build()
            }
        }
    }

}

@Composable
fun SettingsScreen(vm:SettingsScreenViewModel) {
    val state:TestSettingsState by vm.state.collectAsState(initial = TestSettingsState.Default)

    var backendUrl by remember {
        mutableStateOf(state.backendURL)
    }
    var backendUser by remember {
        mutableStateOf(state.backendUser)
    }

    var backendPasword by remember {
        mutableStateOf(state.backendPasword)
    }
    LaunchedEffect(key1 = state) {
        backendUrl = state.backendURL
        backendUser = state.backendUser
        backendPasword = state.backendPasword
    }
    Column {
        Text(text = "Settings", fontSize = 20.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 5.dp))
        Divider()
        Text(text = "Backend", fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
            modifier=Modifier.padding(vertical = 5.dp))
        Divider()

        TextField(value = backendUrl, textStyle = androidx.compose.ui.text.TextStyle(
            fontSize = 12.sp
        ),
            onValueChange = {backendUrl=it}, label={Text(text = "Url")})
        TextField(value = backendUser, onValueChange = {backendUser=it})
        TextField(value = backendPasword, onValueChange = {backendPasword=it}, visualTransformation = PasswordVisualTransformation())
        Divider()
        Button(onClick = { vm.saveSettings(TestSettingsState(backendUrl, backendUser, backendPasword)) }) {
            Text(text = "Save")
        }
    }

}