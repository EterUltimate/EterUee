package me.rerere.rikkahub

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSavedStateNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.ui.rememberSceneSetupNavEntryDecorator
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import coil3.svg.SvgDecoder
import com.dokar.sonner.Toaster
import com.dokar.sonner.rememberToasterState
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import kotlinx.serialization.Serializable
import me.rerere.highlight.Highlighter
import me.rerere.highlight.LocalHighlighter
import me.rerere.rikkahub.data.datastore.SettingsStore
import me.rerere.rikkahub.ui.context.LocalFirebaseAnalytics
import me.rerere.rikkahub.ui.context.LocalNavController
import me.rerere.rikkahub.ui.context.LocalSettings
import me.rerere.rikkahub.ui.context.LocalSharedTransitionScope
import me.rerere.rikkahub.ui.context.LocalTTSState
import me.rerere.rikkahub.ui.context.LocalToaster
import me.rerere.rikkahub.ui.context.popBack
import me.rerere.rikkahub.ui.hooks.readStringPreference
import me.rerere.rikkahub.ui.hooks.rememberCustomTtsState
import me.rerere.rikkahub.ui.pages.assistant.AssistantPage
import me.rerere.rikkahub.ui.pages.assistant.detail.AssistantDetailPage
import me.rerere.rikkahub.ui.pages.backup.BackupPage
import me.rerere.rikkahub.ui.pages.chat.ChatPage
import me.rerere.rikkahub.ui.pages.debug.DebugPage
import me.rerere.rikkahub.ui.pages.history.HistoryPage
import me.rerere.rikkahub.ui.pages.menu.MenuPage
import me.rerere.rikkahub.ui.pages.setting.SettingAboutPage
import me.rerere.rikkahub.ui.pages.setting.SettingDisplayPage
import me.rerere.rikkahub.ui.pages.setting.SettingMcpPage
import me.rerere.rikkahub.ui.pages.setting.SettingModelPage
import me.rerere.rikkahub.ui.pages.setting.SettingPage
import me.rerere.rikkahub.ui.pages.setting.SettingProviderDetailPage
import me.rerere.rikkahub.ui.pages.setting.SettingProviderPage
import me.rerere.rikkahub.ui.pages.setting.SettingSearchPage
import me.rerere.rikkahub.ui.pages.setting.SettingTTSPage
import me.rerere.rikkahub.ui.pages.share.handler.ShareHandlerPage
import me.rerere.rikkahub.ui.pages.translator.TranslatorPage
import me.rerere.rikkahub.ui.pages.webview.WebViewPage
import me.rerere.rikkahub.ui.theme.LocalDarkMode
import me.rerere.rikkahub.ui.theme.RikkahubTheme
import okhttp3.OkHttpClient
import org.koin.android.ext.android.inject
import kotlin.uuid.Uuid

private const val TAG = "RouteActivity"

class RouteActivity : ComponentActivity() {
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private val highlighter by inject<Highlighter>()
    private val okHttpClient by inject<OkHttpClient>()
    private val settingsStore by inject<SettingsStore>()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        disableNavigationBarContrast()
        super.onCreate(savedInstanceState)
        setContent {
            val navStack = rememberNavBackStack(
                Screen.Chat(
                    id = readStringPreference(
                        "lastConversationId",
                        Uuid.random().toString()
                    ) ?: Uuid.random().toString(),
                )
            )
            ShareHandler(navStack)
            RikkahubTheme {
                setSingletonImageLoaderFactory { context ->
                    ImageLoader.Builder(context)
                        .crossfade(true)
                        .components {
                            add(OkHttpNetworkFetcherFactory(callFactory = { okHttpClient }))
                            add(SvgDecoder.Factory(scaleToDensity = true))
                        }
                        .build()
                }
                AppRoutes(navStack)
            }
        }
        firebaseAnalytics = Firebase.analytics
    }

    private fun disableNavigationBarContrast() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
    }

    @Composable
    private fun ShareHandler(navBackStack: NavBackStack) {
        val shareIntent = remember {
            Intent().apply {
                action = intent?.action
                putExtra(Intent.EXTRA_TEXT, intent?.getStringExtra(Intent.EXTRA_TEXT))
            }
        }

        LaunchedEffect(navBackStack) {
            if (shareIntent.action == Intent.ACTION_SEND) {
                val text = shareIntent.getStringExtra(Intent.EXTRA_TEXT)
                if (text != null) {
                    navBackStack.add(Screen.ShareHandler(text))
                }
            }
        }
    }

    private val enterTransition: AnimatedContentTransitionScope<*>.() -> ContentTransform = {
        // Slide in from right when navigating forward
        slideInHorizontally(initialOffsetX = { it }) togetherWith
            slideOutHorizontally(targetOffsetX = { -it })
    }
    private val popTransition: AnimatedContentTransitionScope<*>.() -> ContentTransform = {
        // Slide in from left when navigating back
        slideInHorizontally(initialOffsetX = { -it / 3 }) + fadeIn() + scaleIn(initialScale = 1.1f) togetherWith
            slideOutHorizontally(targetOffsetX = { it }) + scaleOut(targetScale = 0.75f) + fadeOut()
    }
    private val noneTransition: AnimatedContentTransitionScope<*>.() -> ContentTransform = {
        ContentTransform(
            targetContentEnter = EnterTransition.None,
            initialContentExit = ExitTransition.None
        )
    }

    @OptIn(ExperimentalMaterial3AdaptiveApi::class)
    @Composable
    fun AppRoutes(navBackStack: NavBackStack) {
        val toastState = rememberToasterState()
        val settings by settingsStore.settingsFlow.collectAsStateWithLifecycle()
        val tts = rememberCustomTtsState()
        SharedTransitionLayout {
            CompositionLocalProvider(
                LocalNavController provides navBackStack,
                LocalSharedTransitionScope provides this,
                LocalSettings provides settings,
                LocalHighlighter provides highlighter,
                LocalFirebaseAnalytics provides firebaseAnalytics,
                LocalToaster provides toastState,
                LocalTTSState provides tts,
            ) {
                Toaster(
                    state = toastState,
                    darkTheme = LocalDarkMode.current,
                    richColors = true,
                )
                NavDisplay(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    backStack = navBackStack,
                    onBack = { navBackStack.popBack() },
                    entryDecorators = listOf(
                        // Add the default decorators for managing scenes and saving state
                        rememberSceneSetupNavEntryDecorator(),
                        rememberSavedStateNavEntryDecorator(),
                        // Then add the view model store decorator
                        rememberViewModelStoreNavEntryDecorator()
                    ),
                    transitionSpec = enterTransition,
                    popTransitionSpec = popTransition,
                    predictivePopTransitionSpec = popTransition,
                    entryProvider = entryProvider {
                        entry<Screen.Chat>(
                            metadata = NavDisplay.transitionSpec(noneTransition)
                        ) {
                            ChatPage(
                                id = Uuid.parse(it.id),
                                text = it.text,
                            )
                        }

                        entry<Screen.ShareHandler> {
                            ShareHandlerPage(it.text)
                        }

                        entry<Screen.History> {
                            HistoryPage()
                        }

                        entry<Screen.Assistant> {
                            AssistantPage()
                        }

                        entry<Screen.AssistantDetail> {
                            AssistantDetailPage(it.id)
                        }

                        entry<Screen.Menu> {
                            MenuPage()
                        }

                        entry<Screen.Translator> {
                            TranslatorPage()
                        }

                        entry<Screen.Setting> {
                            SettingPage()
                        }

                        entry<Screen.Backup> {
                            BackupPage()
                        }

                        entry<Screen.WebView> {
                            WebViewPage(it.url, it.content)
                        }

                        entry<Screen.SettingDisplay> {
                            SettingDisplayPage()
                        }

                        entry<Screen.SettingProvider> {
                            SettingProviderPage()
                        }

                        entry<Screen.SettingProviderDetail> {
                            val id = Uuid.parse(it.providerId)
                            SettingProviderDetailPage(id = id)
                        }

                        entry<Screen.SettingModels> {
                            SettingModelPage()
                        }

                        entry<Screen.SettingAbout> {
                            SettingAboutPage()
                        }

                        entry<Screen.SettingSearch> {
                            SettingSearchPage()
                        }

                        entry<Screen.SettingTTS> {
                            SettingTTSPage()
                        }

                        entry<Screen.SettingMcp> {
                            SettingMcpPage()
                        }

                        entry<Screen.Debug> {
                            DebugPage()
                        }
                    },
                )
            }
        }
    }
}

sealed interface Screen {
    @Serializable
    data class Chat(val id: String, val text: String? = null) : NavKey

    @Serializable
    data class ShareHandler(val text: String) : NavKey

    @Serializable
    data object History : NavKey

    @Serializable
    data object Assistant : NavKey

    @Serializable
    data class AssistantDetail(val id: String) : NavKey

    @Serializable
    data object Menu : NavKey

    @Serializable
    data object Translator : NavKey

    @Serializable
    data object Setting : NavKey

    @Serializable
    data object Backup : NavKey

    @Serializable
    data class WebView(val url: String = "", val content: String = "") : NavKey

    @Serializable
    data object SettingDisplay : NavKey

    @Serializable
    data object SettingProvider : NavKey

    @Serializable
    data class SettingProviderDetail(val providerId: String) : NavKey

    @Serializable
    data object SettingModels : NavKey

    @Serializable
    data object SettingAbout : NavKey

    @Serializable
    data object SettingSearch : NavKey

    @Serializable
    data object SettingTTS : NavKey

    @Serializable
    data class SettingTTSProviderDetail(val providerId: String) : NavKey

    @Serializable
    data object SettingMcp : NavKey

    @Serializable
    data object Debug : NavKey
}
