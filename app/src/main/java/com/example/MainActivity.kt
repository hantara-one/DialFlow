package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.ui.MainViewModel
import com.example.ui.NuvDialerScreen
import com.example.ui.NuvSplashScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                NuvDialerApp(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshTelephonyAndPermissionState()
    }
}

/**
 * Root container for DialFlow, displaying the brief animated splash screen
 * before transitioning smoothly into the existing Home screen.
 */
@Composable
fun NuvDialerApp(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    var showSplash by rememberSaveable { mutableStateOf(true) }

    Box(modifier = modifier.fillMaxSize()) {
        // Main Home Screen is fully initialized underneath
        NuvDialerScreen(
            viewModel = viewModel,
            modifier = Modifier.fillMaxSize()
        )

        // Smooth startup animation overlay on initial launch
        if (showSplash) {
            NuvSplashScreen(
                onAnimationComplete = { showSplash = false },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
