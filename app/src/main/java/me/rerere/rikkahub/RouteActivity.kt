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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.request.crossfade
import coil3.svg.SvgDecoder
import me.rerere.highlight.Highlighter
import me.rerere.highlight.LocalHighlighter
import me.rerere.rikkahub.ui.context.LocalAnimatedVisibilityScope
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.ui.context.LocalSharedTransitionScope
import me.rerere.rikkahub.ui.context.LocalTTSService
import me.rerere.rikkahub.ui.pages.chat.ChatPage
import me.rerere.rikkahub.ui.pages.debug.DebugPage
import me.rerere.rikkahub.ui.pages.history.HistoryPage
import me.rerere.rikkahub.ui.pages.setting.SettingAboutPage
import me.rerere.rikkahub.ui.pages.setting.SettingModelPage
import me.rerere.rikkahub.ui.pages.setting.SettingPage
import me.rerere.rikkahub.ui.pages.setting.SettingProviderPage
import me.rerere.rikkahub.ui.theme.RikkahubTheme
import org.koin.android.ext.android.inject
import kotlin.uuid.Uuid

private const val TAG = "RouteActivity"

class RouteActivity : ComponentActivity() {
    private val highlighter by inject<Highlighter>()
    private var ttsService by mutableStateOf<TextToSpeech?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        super.onCreate(savedInstanceState)
        setContent {
            RikkahubTheme {
                setSingletonImageLoaderFactory { context ->
                    ImageLoader.Builder(context)
                        .crossfade(true)
                        .components {
                            add(SvgDecoder.Factory(scaleToDensity = true))
                        }
                        .build()
                }
                AppRoutes()
            }
        }
        ttsService = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                Log.i(TAG, "onCreate: TTS engine initialized successfully")
            } else {
                ttsService = null
                Log.e(TAG, "onCreate: TTS engine initialization failed")
            }
        }
    }

    @Composable
    fun AppRoutes() {
        val navController = rememberNavController()
        SharedTransitionLayout {
            CompositionLocalProvider(
                LocalNavController provides navController,
                LocalSharedTransitionScope provides this,
                LocalHighlighter provides highlighter,
                LocalTTSService provides ttsService
            ) {
                NavHost(
                    navController = navController,
                    startDestination = "chat/${Uuid.random()}",
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

                    composableHelper("debug") {
                        DebugPage()
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
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