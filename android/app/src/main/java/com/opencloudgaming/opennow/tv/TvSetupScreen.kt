package com.opencloudgaming.opennow.tv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.width
import androidx.compose.ui.res.painterResource
import com.opencloudgaming.opennow.R
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.opencloudgaming.opennow.OpenNowUiState
import com.opencloudgaming.opennow.OpenNowViewModel
import com.opencloudgaming.opennow.StreamPreset
import com.opencloudgaming.opennow.completingSetupFlow

private enum class TvSetupStep { Welcome, Streaming, Ready }

/**
 * First-run setup, Switch "System Setup" style. The phone flow's appearance and touch steps do not
 * apply on a TV, so this asks only for the streaming quality, then marks setup complete exactly as
 * the stock flow does.
 */
@Composable
internal fun TvSetupScreen(state: OpenNowUiState, viewModel: OpenNowViewModel) {
    var stepName by rememberSaveable { mutableStateOf(TvSetupStep.Welcome.name) }
    val step = TvSetupStep.valueOf(stepName)
    BackHandler(enabled = step != TvSetupStep.Welcome) {
        stepName = TvSetupStep.entries[step.ordinal - 1].name
    }
    val firstRow = remember(step) { FocusRequester() }
    LaunchedEffect(step) {
        withFrameNanos { }
        runCatching { firstRow.requestFocus() }
    }
    val user = state.authSession?.user?.displayName

    TvSwitchScreenFrame(
        title = "System Setup",
        icon = Icons.Rounded.SportsEsports,
        safeAreaDp = state.settings.tvSafeAreaPaddingDp,
        hints = buildList {
            if (step != TvSetupStep.Welcome) add(TvButtonHint(TvButtonGlyph.B, "Back"))
            add(TvButtonHint(TvButtonGlyph.A, "OK"))
        },
        headerTrailing = {
            Text("${step.ordinal + 1} / ${TvSetupStep.entries.size}", color = TvSwitchColors.SecondaryText, fontSize = 18.sp)
        },
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.widthIn(max = 620.dp)) {
                when (step) {
                    TvSetupStep.Welcome -> {
                        Image(
                            painter = painterResource(R.drawable.goudanow_logo_mark),
                            contentDescription = null,
                            modifier = Modifier.width(220.dp).height(119.dp),
                        )
                        Spacer(Modifier.height(18.dp))
                        Heading(if (user != null) "Welcome, $user" else "Welcome")
                        Body("Let's get your TV ready for cloud gaming. This only takes a moment.")
                        Spacer(Modifier.height(28.dp))
                        TvListRow(
                            label = "Start",
                            onClick = { stepName = TvSetupStep.Streaming.name },
                            modifier = Modifier.focusRequester(firstRow),
                        )
                    }
                    TvSetupStep.Streaming -> {
                        Heading("Streaming quality")
                        Body("Recommended is tuned for this TV and your connection. You can change it later in System Settings.")
                        Spacer(Modifier.height(20.dp))
                        listOf(
                            StreamPreset.Recommended to "Recommended",
                            StreamPreset.High to "High quality",
                            StreamPreset.Medium to "Balanced",
                            StreamPreset.LowDataSaver to "Data saver",
                        ).forEachIndexed { index, (preset, label) ->
                            TvListRow(
                                label = label,
                                value = if (state.settings.streamPreset == preset) "✓" else null,
                                onClick = {
                                    viewModel.applyStreamPreset(preset)
                                    stepName = TvSetupStep.Ready.name
                                },
                                modifier = if (index == 0) Modifier.focusRequester(firstRow) else Modifier,
                            )
                        }
                    }
                    TvSetupStep.Ready -> {
                        Heading("You're all set")
                        Body(
                            "Choose a game on the HOME menu and press A to play. " +
                                "Press the stream menu button during a game for options.",
                        )
                        Spacer(Modifier.height(28.dp))
                        TvListRow(
                            label = "Go to the HOME menu",
                            onClick = { viewModel.updateSettings(state.settings.completingSetupFlow()) },
                            modifier = Modifier.focusRequester(firstRow),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Heading(text: String) {
    Text(text, color = TvSwitchColors.PrimaryText, fontSize = 30.sp, textAlign = TextAlign.Center)
}

@Composable
private fun Body(text: String) {
    Spacer(Modifier.height(12.dp))
    Text(text, color = TvSwitchColors.SecondaryText, fontSize = 19.sp, textAlign = TextAlign.Center)
}
