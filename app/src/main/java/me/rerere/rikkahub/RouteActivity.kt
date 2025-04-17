package me.rerere.rikkahub

import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.request.crossfade
import coil3.svg.SvgDecoder
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import me.rerere.highlight.Highlighter
import me.rerere.highlight.LocalHighlighter
import me.rerere.rikkahub.data.datastore.Settings
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.ui.context.LocalAnimatedVisibilityScope
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.ui.context.LocalSharedTransitionScope
import me.rerere.rikkahub.ui.context.LocalTTSService
import me.rerere.rikkahub.ui.pages.assistant.AssistantPage
import me.rerere.rikkahub.ui.pages.chat.ChatPage
import me.rerere.rikkahub.ui.pages.debug.DebugPage
import me.rerere.rikkahub.ui.pages.history.HistoryPage
import me.rerere.rikkahub.ui.pages.setting.SettingAboutPage
import me.rerere.rikkahub.ui.pages.setting.SettingModelPage
import me.rerere.rikkahub.ui.pages.setting.SettingPage
import me.rerere.rikkahub.ui.pages.setting.SettingProviderPage
import me.rerere.rikkahub.ui.pages.setting.SettingSearchPage
import me.rerere.rikkahub.ui.theme.RikkahubTheme
import org.koin.android.ext.android.inject
import kotlin.uuid.Uuid

private const val TAG = "RouteActivity"

class RouteActivity : ComponentActivity() {
    private val highlighter by inject<Highlighter>()
    private var ttsService by mutableStateOf<TextToSpeech?>(null)

    private val settingStore by inject<SettingsStore>()
    private val settings = settingStore.settingsFlow
        .stateIn(lifecycleScope, SharingStarted.Eagerly, Settings())

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        disableNavigationBarContrast()
        super.onCreate(savedInstanceState)
        setContent {
            val settingsState by settings.collectAsStateWithLifecycle()
            val navController = rememberNavController()
            RikkahubTheme(dynamicColor = settingsState.dynamicColor) {
                setSingletonImageLoaderFactory { context ->
                    ImageLoader.Builder(context)
                        .crossfade(true)
                        .components {
                            add(SvgDecoder.Factory(scaleToDensity = true))
                        }
                        .build()
                }
                AppRoutes(navController)
            }
        }
        initTTS()
    }

    private fun initTTS() {
        ttsService = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                Log.i(TAG, "onCreate: TTS engine initialized successfully")
            } else {
                ttsService = null
                Log.e(TAG, "onCreate: TTS engine initialization failed")
            }
        }
    }

    private fun disableNavigationBarContrast() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
    }

    @Composable
    fun AppRoutes(navController: NavHostController) {
        SharedTransitionLayout {
            CompositionLocalProvider(
                LocalNavController provides navController,
                LocalSharedTransitionScope provides this,
                LocalHighlighter provides highlighter,
                LocalTTSService provides ttsService
            ) {
                NavHost(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    navController = navController,
                    startDestination = rememberSaveable { "chat/${Uuid.random()}" },
                    enterTransition = {
                        scaleIn(initialScale = 0.35f) + fadeIn(animationSpec = tween(300))
                    },
                    exitTransition = {
                        fadeOut(animationSpec = tween(300))
                    },
                    popEnterTransition = {
                        fadeIn(animationSpec = tween(300))
                    },
                    popExitTransition = {
                        scaleOut(targetScale = 0.35f) + fadeOut(animationSpec = tween(300))
                    }
                ) {
                    composableHelper(
                        route = "chat/{id}",
                        args = listOf(
                            navArgument("id") {
                                type = NavType.StringType
                            }
                        ),
                    ) { entry ->
                        ChatPage(
                            id = Uuid.parse(entry.arguments?.getString("id")!!)
                        )
                    }

                    composableHelper("history") {
                        HistoryPage()
                    }

                    composableHelper("assistant") {
                        AssistantPage()
                    }

                    composableHelper("setting") {
                        SettingPage()
                    }

                    composableHelper("setting/provider") {
                        SettingProviderPage()
                    }

                    composableHelper("setting/models") {
                        SettingModelPage()
                    }

                    composableHelper("setting/about") {
                        SettingAboutPage()
                    }

                    composableHelper("setting/search") {
                        SettingSearchPage()
                    }

                    composableHelper("debug") {
                        DebugPage()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        destroyTTSService()
    }

    private fun destroyTTSService() {
        ttsService?.stop()
        ttsService?.shutdown()
        ttsService = null
    }
}


private fun NavGraphBuilder.composableHelper(
    route: String,
    args: List<NamedNavArgument> = emptyList(),
    enterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?)? = null,
    exitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?)? = null,
    popEnterTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?)? = null,
    popExitTransition: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?)? = null,
    content: @Composable AnimatedContentScope.(NavBackStackEntry) -> Unit
) {
    this.composable(
        route = route,
        arguments = args,
        enterTransition = enterTransition,
        exitTransition = exitTransition,
        popEnterTransition = popEnterTransition,
        popExitTransition = popExitTransition,
    ) { entry ->
        CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
            content(entry)
        }
    }
}