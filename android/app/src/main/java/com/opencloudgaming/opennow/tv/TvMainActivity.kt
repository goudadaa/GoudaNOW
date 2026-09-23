package com.opencloudgaming.opennow.tv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.opencloudgaming.opennow.APP_MESSAGE_CHECK_INTERVAL_MS
import com.opencloudgaming.opennow.AppPage
import com.opencloudgaming.opennow.MainActivity
import com.opencloudgaming.opennow.OpenNowApp
import com.opencloudgaming.opennow.OpenNowUiState
import com.opencloudgaming.opennow.OpenNowViewModel
import com.opencloudgaming.opennow.SettingsRouteTarget
import com.opencloudgaming.opennow.shouldShowSetupFlow
import kotlinx.coroutines.delay

/**
 * Android TV entry point with the Switch-style home.
 *
 * Streaming, PiP, system UI, refresh-rate switching and controller routing are inherited from
 * [MainActivity] unchanged. Only the root composable differs: Switch-style screens own Home,
 * All Games, System Settings and sign-in; the existing [OpenNowApp] still renders the stream, the
 * rarer prompts, and the full settings. Switch-style launch prompts and the queue screen are
 * layered on top without replacing the stock screens underneath.
 */
class TvMainActivity : MainActivity() {
    @Composable
    override fun AppContent(
        viewModel: OpenNowViewModel,
        onMicrophoneCaptureActiveChange: (Boolean) -> Unit,
    ) {
        TvRoot(
            viewModel = viewModel,
            onMicrophoneCaptureActiveChange = onMicrophoneCaptureActiveChange,
            onPowerOff = ::finish,
        )
    }
}

internal enum class TvScreen { SignIn, Setup, Home, AllGames, Settings, Stock }

@Composable
internal fun TvRoot(
    viewModel: OpenNowViewModel,
    onMicrophoneCaptureActiveChange: (Boolean) -> Unit,
    onPowerOff: () -> Unit,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val signedIn = state.authSession != null
    var stockLogin by rememberSaveable { mutableStateOf(false) }
    var stockSettings by rememberSaveable { mutableStateOf(false) }
    var settingsCategoryName by rememberSaveable { mutableStateOf(TvSettingsCategory.Account.name) }
    // Remembered by game id (not position) at this level, so it survives the screen being
    // replaced by a stream and the carousel reordering around a queued game.
    var homeFocusedGameId by rememberSaveable { mutableStateOf<String?>(null) }
    var libraryFocusedGameId by rememberSaveable { mutableStateOf<String?>(null) }
    var hiddenUpdateKey by rememberSaveable { mutableStateOf<String?>(null) }

    // The TV always opens on Home; the phone "default launch page" setting does not apply here.
    LaunchedEffect(Unit) {
        val current = viewModel.state.value
        if (current.page != AppPage.Home && current.page != AppPage.Stream && current.streamStatus == "idle") {
            viewModel.setPage(AppPage.Home)
        }
    }
    LaunchedEffect(signedIn) {
        if (signedIn) stockLogin = false
    }
    LaunchedEffect(state.page) {
        if (state.page != AppPage.Settings) stockSettings = false
    }

    val screen = tvScreenFor(state, stockLogin = stockLogin, stockSettings = stockSettings)
    val prompt = if (signedIn) tvPromptFor(state, hiddenUpdateKey) else null

    // OpenNowApp polls for server messages while it is composed; do the same while a Switch
    // screen replaces it, so announcements still arrive.
    val lifecycleOwner = LocalLifecycleOwner.current
    val pollMessages = signedIn && screen != TvScreen.Stock && state.streamStatus == "idle"
    LaunchedEffect(lifecycleOwner, pollMessages) {
        if (pollMessages) {
            lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    viewModel.checkAppMessage()
                    delay(APP_MESSAGE_CHECK_INTERVAL_MS)
                }
            }
        }
    }

    CompositionLocalProvider(LocalTvSwitchSkin provides true) { Box(Modifier.fillMaxSize()) {
        when (screen) {
            TvScreen.SignIn -> TvSignInScreen(
                state = state,
                viewModel = viewModel,
                onOtherOptions = { stockLogin = true },
            )
            TvScreen.Setup -> TvSetupScreen(state = state, viewModel = viewModel)
            TvScreen.Home -> TvHomeScreen(
                state = state,
                focusedGameId = homeFocusedGameId,
                focusBlocked = prompt != null,
                onGameFocused = { homeFocusedGameId = it.id },
                onGameSelected = { game -> viewModel.play(game) },
                onAllGames = { viewModel.setPage(AppPage.Library) },
                onControllers = {
                    settingsCategoryName = TvSettingsCategory.Controllers.name
                    viewModel.setPage(AppPage.Settings)
                },
                onSettings = { viewModel.setPage(AppPage.Settings) },
                onProfile = {
                    settingsCategoryName = TvSettingsCategory.Account.name
                    viewModel.setPage(AppPage.Settings)
                },
                onPowerOff = onPowerOff,
                onReturnToQueue = viewModel::restoreStreamLaunch,
            )
            TvScreen.AllGames -> TvAllGamesScreen(
                state = state,
                focusedGameId = libraryFocusedGameId,
                focusBlocked = prompt != null,
                onGameFocused = { libraryFocusedGameId = it.id },
                onGameSelected = { game -> viewModel.play(game) },
                onSortSelected = viewModel::setLibrarySort,
                onSearchChange = viewModel::setLibrarySearch,
                onBack = {
                    viewModel.setLibrarySearch("")
                    viewModel.setPage(AppPage.Home)
                },
            )
            TvScreen.Settings -> TvSettingsScreen(
                state = state,
                viewModel = viewModel,
                category = TvSettingsCategory.valueOf(settingsCategoryName),
                onCategoryChange = { settingsCategoryName = it.name },
                onOpenFullSettings = { target ->
                    stockSettings = true
                    when (target) {
                        SettingsRouteTarget.Account -> viewModel.openAccountSettings()
                        SettingsRouteTarget.Stream -> viewModel.openStreamSettings()
                        SettingsRouteTarget.General -> viewModel.openAndroidUpdateSettings()
                        SettingsRouteTarget.Interface -> viewModel.openInterfaceSettings()
                        null -> Unit
                    }
                },
                onBack = { viewModel.setPage(AppPage.Home) },
            )
            TvScreen.Stock -> {
                // Registered before OpenNowApp, so any BackHandler inside it wins. This only catches
                // Back on the stock login screen and returns to the Switch-style sign-in.
                BackHandler(enabled = !signedIn && state.deviceLoginPrompt == null) {
                    stockLogin = false
                }
                OpenNowApp(
                    viewModel = viewModel,
                    onMicrophoneCaptureActiveChange = onMicrophoneCaptureActiveChange,
                )
                if (shouldShowTvQueue(state)) {
                    TvQueueScreen(state = state, viewModel = viewModel)
                }
            }
        }
        if (prompt != null) {
            TvPrompts(
                prompt = prompt,
                state = state,
                viewModel = viewModel,
                coverUnderlying = screen == TvScreen.Stock,
                onHideUpdate = { hiddenUpdateKey = it },
            )
        }
    } }
}

/**
 * Which screen owns the display. Switch-style screens are used while the app is otherwise quiet;
 * the setup flow, stream, and prompts that have no Switch version yet fall back to [OpenNowApp].
 */
internal fun tvScreenFor(state: OpenNowUiState, stockLogin: Boolean, stockSettings: Boolean): TvScreen {
    if (state.authSession == null) return if (stockLogin) TvScreen.Stock else TvScreen.SignIn
    if (!state.isQuietForTvScreens()) return TvScreen.Stock
    if (shouldShowSetupFlow(state.settings)) return TvScreen.Setup
    return when (state.page) {
        AppPage.Home -> TvScreen.Home
        AppPage.Library -> TvScreen.AllGames
        AppPage.Settings -> if (stockSettings) TvScreen.Stock else TvScreen.Settings
        AppPage.Stream -> TvScreen.Stock
    }
}

/**
 * No stream in the foreground. Prompts are drawn by [TvPrompts] on top of whichever screen is
 * showing, so they no longer force the stock UI. `selectedGame` (the stock game-details sheet) is
 * never set by the TV screens, but falls back safely if a deep link sets it.
 */
private fun OpenNowUiState.isQuietForTvScreens(): Boolean =
    (streamStatus == "idle" || streamLaunchMinimized) && selectedGame == null
