package com.example

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.DialerPreferences
import com.example.ui.MainViewModel
import com.example.ui.NuvDialerScreen
import com.example.ui.NuvSplashScreen
import com.example.ui.theme.MyApplicationTheme
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun attachBaseContext(newBase: Context) {
        val prefs = DialerPreferences(newBase)
        val lang = prefs.languageCode
        val locale = Locale.forLanguageTag(lang)
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        config.setLocales(android.os.LocaleList(locale))
        config.setLayoutDirection(locale)
        val localizedContext = newBase.createConfigurationContext(config)
        super.attachBaseContext(localizedContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val savedLang = viewModel.currentLanguage.value
        val locale = Locale.forLanguageTag(savedLang)
        Locale.setDefault(locale)
        val config = Configuration(resources.configuration)
        config.setLocale(locale)
        config.setLocales(android.os.LocaleList(locale))
        config.setLayoutDirection(locale)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)

        setContent {
            val currentLanguage by viewModel.currentLanguage.collectAsStateWithLifecycle()
            val context = LocalContext.current

            val localizedContextWrapper = remember(currentLanguage, context) {
                val loc = Locale.forLanguageTag(currentLanguage)
                Locale.setDefault(loc)
                val conf = Configuration(context.resources.configuration)
                conf.setLocale(loc)
                conf.setLocales(android.os.LocaleList(loc))
                conf.setLayoutDirection(loc)
                val localizedConfigContext = context.createConfigurationContext(conf)
                LocalizedActivityContext(
                    activity = this@MainActivity,
                    localizedContext = localizedConfigContext
                )
            }

            val localizedConfiguration = remember(currentLanguage, localizedContextWrapper) {
                localizedContextWrapper.resources.configuration
            }

            val registryOwner = LocalActivityResultRegistryOwner.current ?: this@MainActivity

            CompositionLocalProvider(
                LocalContext provides localizedContextWrapper,
                LocalConfiguration provides localizedConfiguration,
                LocalActivityResultRegistryOwner provides registryOwner
            ) {
                MyApplicationTheme {
                    NuvDialerApp(
                        viewModel = viewModel,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshTelephonyAndPermissionState()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        val prefs = DialerPreferences(this)
        val lang = prefs.languageCode
        val locale = Locale.forLanguageTag(lang)
        Locale.setDefault(locale)
        val config = Configuration(newConfig)
        config.setLocale(locale)
        config.setLocales(android.os.LocaleList(locale))
        config.setLayoutDirection(locale)
        super.onConfigurationChanged(config)
        @Suppress("DEPRECATION")
        resources.updateConfiguration(config, resources.displayMetrics)
    }
}

/**
 * Context wrapper that preserves Activity identity and ActivityResultRegistryOwner
 * while providing localized resources and configuration.
 */
class LocalizedActivityContext(
    val activity: AppCompatActivity,
    private val localizedContext: Context
) : ContextWrapper(activity), ActivityResultRegistryOwner {
    override val activityResultRegistry: ActivityResultRegistry
        get() = activity.activityResultRegistry

    override fun getResources(): Resources = localizedContext.resources

    override fun createConfigurationContext(overrideConfiguration: Configuration): Context =
        localizedContext.createConfigurationContext(overrideConfiguration)
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
