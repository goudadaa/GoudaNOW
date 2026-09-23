package com.opencloudgaming.opennow.tv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import androidx.tv.material3.Text
import com.opencloudgaming.opennow.AppPage
import com.opencloudgaming.opennow.GAME_BOX_ART_ASPECT_RATIO
import com.opencloudgaming.opennow.OpenNowUiState
import com.opencloudgaming.opennow.OpenNowViewModel
import com.opencloudgaming.opennow.canMinimizeStreamLaunch
import com.opencloudgaming.opennow.isNativeStreamReady
import com.opencloudgaming.opennow.isReadyForStream
import com.opencloudgaming.opennow.sessionAdItems

/**
 * Whether the Switch-style queue screen should cover the stock one.
 *
 * The stock StreamScreen keeps running underneath — it owns the stream client, microphone and
 * ad-reporting effects — and this only paints over it. Anything interactive the stock screen
 * shows (queue ads, errors, launch prompts) makes this return false so the stock UI is used.
 */
internal fun shouldShowTvQueue(state: OpenNowUiState): Boolean =
    state.page == AppPage.Stream &&
        state.streamStatus != "idle" &&
        !state.isNativeStreamReady() &&
        state.error.isNullOrBlank() &&
        state.activeSessionDecision == null &&
        !state.awaitingLaunchInputModeChoice &&
        state.pendingBatteryOptimizationLaunch == null &&
        state.pendingLaunchRecovery == null &&
        state.pendingPrintedWasteGame == null &&
        state.queueAdActiveId == null &&
        sessionAdItems(state.streamSession?.adState).isEmpty()

@Composable
internal fun TvQueueScreen(state: OpenNowUiState, viewModel: OpenNowViewModel) {
    val canMinimize = canMinimizeStreamLaunch(
        streamStatus = state.streamStatus,
        sessionReady = state.streamSession?.isReadyForStream() == true,
    )
    BackHandler(enabled = canMinimize, onBack = viewModel::minimizeStreamLaunch)
    val firstButton = remember { FocusRequester() }
    var hasFocus by remember { mutableStateOf(false) }
    // The stock queue screen underneath may request focus for its own buttons; take it back.
    LaunchedEffect(hasFocus, canMinimize) {
        if (!hasFocus) {
            withFrameNanos { }
            runCatching { firstButton.requestFocus() }
        }
    }
    val game = state.streamGame
    val position = state.queuePosition?.takeIf { it > 0 }

    TvSwitchTheme {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(TvSwitchColors.Background)
                .padding(tvSafeAreaPadding(state.settings.tvSafeAreaPaddingDp)),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(48.dp),
                // Keep D-pad focus inside this screen; the stock queue UI underneath is hidden.
                modifier = Modifier
                    .onFocusChanged { hasFocus = it.hasFocus }
                    .focusProperties { onExit = { cancelFocusChange() } }
                    .focusGroup(),
            ) {
                if (game != null) {
                    AsyncImage(
                        model = game.imageUrl ?: game.tvCardImageUrl,
                        contentDescription = game.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .height(300.dp)
                            .width(300.dp * GAME_BOX_ART_ASPECT_RATIO)
                            .clip(RoundedCornerShape(4.dp))
                            .background(TvSwitchColors.TilePlaceholder),
                    )
                }
                Column(modifier = Modifier.widthIn(max = 460.dp)) {
                    Text(
                        text = game?.title ?: "Starting your game",
                        color = TvSwitchColors.PrimaryText,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.height(20.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(color = TvSwitchColors.FocusCyan, modifier = Modifier.height(28.dp).width(28.dp))
                        Spacer(Modifier.width(16.dp))
                        Text(
                            text = when {
                                position != null -> "You're number $position in the queue"
                                state.launchPhase.isNotBlank() -> state.launchPhase
                                else -> "Getting a server ready…"
                            },
                            color = TvSwitchColors.FocusCyan,
                            fontSize = 20.sp,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = if (canMinimize) {
                            "You can go back to the HOME menu while you wait. The game will be ready when you return."
                        } else {
                            "Almost there…"
                        },
                        color = TvSwitchColors.SecondaryText,
                        fontSize = 17.sp,
                    )
                    Spacer(Modifier.height(28.dp))
                    if (canMinimize) {
                        TvListRow(
                            label = "Wait on the HOME menu",
                            onClick = viewModel::minimizeStreamLaunch,
                            modifier = Modifier.focusRequester(firstButton),
                        )
                    }
                    TvListRow(
                        label = "Cancel",
                        labelColor = TvSwitchColors.Error,
                        onClick = viewModel::stopStream,
                        modifier = if (canMinimize) Modifier else Modifier.focusRequester(firstButton),
                    )
                }
            }
        }
    }
}
