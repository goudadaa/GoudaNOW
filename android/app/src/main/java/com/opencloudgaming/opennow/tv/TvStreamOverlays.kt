package com.opencloudgaming.opennow.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.opencloudgaming.opennow.StreamGuideStep

/**
 * True only inside [TvMainActivity]. The three stock stream composables that have a Switch
 * version check this and hand over to the functions below; the phone app never provides it, so
 * its stream UI is unchanged.
 */
internal val LocalTvSwitchSkin = staticCompositionLocalOf { false }

/**
 * Switch-style in-game menu: a panel on the right over the running game. It is a pure view —
 * every row calls the callback StreamScreen already passes to the stock panel, so audio,
 * microphone, keyboard and input handling are exactly the stock ones. Back is still handled by
 * StreamScreen, which closes the menu.
 */
@Composable
internal fun TvStreamQuickMenu(
    gameTitle: String,
    status: String?,
    audioMuted: Boolean,
    microphoneRequested: Boolean,
    microphoneEnabled: Boolean,
    statsVisible: Boolean,
    controllerMouseEmulationEnabled: Boolean,
    onResume: () -> Unit,
    onAudioToggle: () -> Unit,
    onMicrophoneToggle: () -> Unit,
    onStatsToggle: () -> Unit,
    onKeyboardOpen: () -> Unit,
    onSteamMenuOpen: () -> Unit,
    onControllerMouseEmulationToggle: () -> Unit,
    onEsc: () -> Unit,
    onQuit: () -> Unit,
    onButtonTone: () -> Unit,
) {
    val firstRow = remember { FocusRequester() }
    var hasFocus by remember { mutableStateOf(false) }
    LaunchedEffect(hasFocus) {
        if (!hasFocus) {
            withFrameNanos { }
            runCatching { firstRow.requestFocus() }
        }
    }
    fun tap(action: () -> Unit): () -> Unit = {
        onButtonTone()
        action()
    }
    TvSwitchTheme {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
            Column(
                modifier = Modifier
                    .width(420.dp)
                    .fillMaxHeight()
                    .background(TvSwitchColors.Background.copy(alpha = 0.96f))
                    .onFocusChanged { hasFocus = it.hasFocus }
                    .focusProperties { onExit = { cancelFocusChange() } }
                    .focusGroup()
                    .padding(horizontal = 20.dp, vertical = 28.dp),
            ) {
                Text(
                    text = gameTitle,
                    color = TvSwitchColors.PrimaryText,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 20.dp),
                )
                if (!status.isNullOrBlank()) {
                    Text(status, color = TvSwitchColors.FocusCyan, fontSize = 16.sp, modifier = Modifier.padding(start = 20.dp, top = 4.dp))
                }
                Spacer(Modifier.height(14.dp))
                TvDivider()
                Spacer(Modifier.height(8.dp))
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    TvListRow(label = "Return to game", onClick = tap(onResume), modifier = Modifier.focusRequester(firstRow))
                    TvSectionLabel("Game")
                    TvListRow(label = "Sound", value = if (audioMuted) "Muted" else "On", onClick = tap(onAudioToggle))
                    if (microphoneRequested) {
                        TvListRow(label = "Microphone", value = if (microphoneEnabled) "On" else "Off", onClick = tap(onMicrophoneToggle))
                    }
                    TvListRow(label = "Performance stats", value = if (statsVisible) "Shown" else "Hidden", onClick = tap(onStatsToggle))
                    TvSectionLabel("Input")
                    TvListRow(label = "Keyboard", onClick = tap(onKeyboardOpen))
                    TvListRow(
                        label = "Use controller as mouse",
                        value = if (controllerMouseEmulationEnabled) "On" else "Off",
                        onClick = tap(onControllerMouseEmulationToggle),
                    )
                    TvListRow(label = "Send Esc", onClick = tap(onEsc))
                    TvListRow(label = "Open Steam menu", onClick = tap(onSteamMenuOpen))
                    Spacer(Modifier.height(10.dp))
                    TvListRow(label = "Quit game", labelColor = TvSwitchColors.Error, onClick = tap(onQuit))
                }
                TvButtonHints(
                    listOf(TvButtonHint(TvButtonGlyph.B, "Back"), TvButtonHint(TvButtonGlyph.A, "OK")),
                    Modifier.fillMaxWidth().padding(top = 12.dp, end = 12.dp).height(32.dp),
                )
            }
        }
    }
}

/** Switch-style "Quit game?" confirmation. Back keeps playing, as in the stock dialog. */
@Composable
internal fun TvStreamExitConfirmation(gameTitle: String, onKeepPlaying: () -> Unit, onExit: () -> Unit) {
    TvDialog(
        onDismiss = onKeepPlaying,
        actions = listOf(
            TvDialogAction("Keep playing", onKeepPlaying),
            TvDialogAction("Quit game", onExit),
        ),
    ) {
        TvDialogTitle("Quit $gameTitle?")
        TvDialogBody("Your cloud session will end. Unsaved progress may be lost.")
    }
}

/** Shown on the stream page when there is no session to show. */
@Composable
internal fun TvNoActiveStream(
    canResumeSession: Boolean,
    canEndSession: Boolean,
    onBack: () -> Unit,
    onResumeSession: () -> Unit,
    onEndSession: () -> Unit,
) {
    TvDialog(
        opaqueBackdrop = true,
        onDismiss = onBack,
        actions = buildList {
            if (canResumeSession) add(TvDialogAction("Resume session", onResumeSession))
            if (canEndSession && canResumeSession) add(TvDialogAction("End session", onEndSession))
            add(TvDialogAction("Back to the HOME menu", onBack))
        },
    ) {
        TvDialogTitle(if (canResumeSession) "A session is still running" else "No game is running")
        TvDialogBody(
            if (canResumeSession) {
                "You can go back to it, or end it to free up your account."
            } else {
                "Choose a game on the HOME menu to start playing."
            },
        )
    }
}

/**
 * Switch-style first-stream tutorial. Same two steps and callbacks as the stock
 * `StreamFirstLaunchGuide`: step 1 teaches that Back opens the in-game menu, step 2 shows
 * while the menu is open and finishes when the player returns to the game.
 */
@Composable
internal fun TvStreamGuide(
    step: StreamGuideStep,
    controlsOpen: Boolean,
    onOpenControls: () -> Unit,
    onSkip: () -> Unit,
) {
    when {
        step == StreamGuideStep.OpenControls -> TvDialog(
            // Pressing Back here opens the menu, which is exactly what the step teaches.
            onDismiss = onOpenControls,
            actions = listOf(
                TvDialogAction("Open the menu", onOpenControls),
                TvDialogAction("Skip tutorial", onSkip),
            ),
        ) {
            Text("Step 1 of 2", color = TvSwitchColors.FocusCyan, fontSize = 17.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            TvDialogTitle("Open the in-game menu")
            TvDialogBody(
                "While you're playing, press B or Back on your controller or remote. " +
                    "The menu opens on top of your game without closing it.",
            )
        }
        controlsOpen -> TvStreamGuideHint()
        else -> TvDialog(
            onDismiss = onSkip,
            actions = listOf(
                TvDialogAction("Open the menu", onOpenControls),
                TvDialogAction("Finish tutorial", onSkip),
            ),
        ) {
            Text("Step 2 of 2", color = TvSwitchColors.FocusCyan, fontSize = 17.sp, fontWeight = FontWeight.Medium)
            Spacer(Modifier.height(8.dp))
            TvDialogTitle("Get back to your game")
            TvDialogBody("In the menu, choose Return to game or press B.")
        }
    }
}

/** Step 2 hint shown beside the open menu. Not focusable, so the menu keeps the D-pad. */
@Composable
private fun TvStreamGuideHint() {
    TvSwitchTheme {
        Box(Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.TopStart) {
            Column(
                modifier = Modifier
                    .width(360.dp)
                    .background(TvSwitchColors.DialogPanel, RoundedCornerShape(6.dp))
                    .border(2.dp, TvSwitchColors.FocusCyan, RoundedCornerShape(6.dp))
                    .padding(horizontal = 24.dp, vertical = 20.dp),
            ) {
                Text("Step 2 of 2", color = TvSwitchColors.FocusCyan, fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(6.dp))
                Text("This is the in-game menu", color = TvSwitchColors.PrimaryText, fontSize = 21.sp, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Choose Return to game, or press B, to go back to playing.",
                    color = TvSwitchColors.SecondaryText,
                    fontSize = 17.sp,
                )
            }
        }
    }
}
