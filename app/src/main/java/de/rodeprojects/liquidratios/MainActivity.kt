package de.rodeprojects.liquidratios

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import de.rodeprojects.liquidratios.data.MixStore
import de.rodeprojects.liquidratios.data.mixDataStore
import de.rodeprojects.liquidratios.ui.LiquidRatiosTheme
import de.rodeprojects.liquidratios.ui.MixScreen

class MainActivity : ComponentActivity() {
    private val model: MixViewModel by viewModels {
        viewModelFactory { initializer { MixViewModel(MixStore(applicationContext.mixDataStore)) } }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.BLACK),
        )
        super.onCreate(savedInstanceState)
        setContent {
            val state by model.state.collectAsStateWithLifecycle()
            LiquidRatiosTheme {
                MixScreen(state, model::dispatch)
            }
        }
    }
}
