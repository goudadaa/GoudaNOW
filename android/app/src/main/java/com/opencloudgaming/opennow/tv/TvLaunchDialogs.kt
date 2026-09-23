package com.opencloudgaming.opennow.tv

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.opencloudgaming.opennow.AndroidUpdateStatus
import com.opencloudgaming.opennow.AppPage
import com.opencloudgaming.opennow.GFN_MEMBERSHIP_URL
import com.opencloudgaming.opennow.OpenNowUiState
import com.opencloudgaming.opennow.OpenNowViewModel
import com.opencloudgaming.opennow.PrintedWasteZoneOption
import com.opencloudgaming.opennow.QrCode
import com.opencloudgaming.opennow.QrCodeView
import com.opencloudgaming.opennow.SessionReport
import com.opencloudgaming.opennow.StreamInputMode
import com.opencloudgaming.opennow.isStandardPrintedWasteZone
import com.opencloudgaming.opennow.launchableGameVariants
import com.opencloudgaming.opennow.openBatteryOptimizationSettings
import com.opencloudgaming.opennow.printedWasteZoneTitle
import com.opencloudgaming.opennow.printedWasteZoneUrl
import com.opencloudgaming.opennow.recommendedPrintedWasteZone
import com.opencloudgaming.opennow.visibleNoticeKey
import java.util.Locale

/** Keys of the prompts the Switch layer can show, in the order they take priority. */
internal enum class TvPrompt {
    ActiveSession, InputMode, ServerPicker, StoreChoice, Membership, BatteryOptimization,
    LaunchRecovery, GfnActivation, SessionReport, Update, AppMessage,
}

/**
 * The prompt that should be on screen now, if any. Mirrors the stock visibility rules, including
 * the session-report opt-out and holding app messages back while a game is running.
 */
internal fun tvPromptFor(state: OpenNowUiState, hiddenUpdateKey: String?): TvPrompt? {
    val streaming = state.page == AppPage.Stream || state.streamStatus != "idle"
    return when {
        state.activeSessionDecision != null -> TvPrompt.ActiveSession
        state.awaitingLaunchInputModeChoice -> TvPrompt.InputMode
        state.pendingPrintedWasteGame != null -> TvPrompt.ServerPicker
        state.pendingStoreChoiceGame != null -> TvPrompt.StoreChoice
        state.pendingMembershipNotice != null -> TvPrompt.Membership
        state.pendingBatteryOptimizationLaunch != null -> TvPrompt.BatteryOptimization
        state.pendingLaunchRecovery != null -> TvPrompt.LaunchRecovery
        state.pendingGfnMembershipActivation -> TvPrompt.GfnActivation
        streaming -> null
        state.sessionReport != null && state.settings.showSessionReportAfterStream -> TvPrompt.SessionReport
        state.androidUpdate.status in setOf(AndroidUpdateStatus.Available, AndroidUpdateStatus.Downloaded) &&
            state.androidUpdate.visibleNoticeKey(state.dismissedAndroidUpdateNoticeKey)?.let { it != hiddenUpdateKey } == true ->
            TvPrompt.Update
        state.appMessage != null && state.error == null -> TvPrompt.AppMessage
        else -> null
    }
}

/**
 * Switch-styled versions of every launch and system prompt. Every button calls the same
 * ViewModel action the stock dialog uses, so behaviour is unchanged; only the look differs.
 */
@Composable
internal fun TvPrompts(
    prompt: TvPrompt,
    state: OpenNowUiState,
    viewModel: OpenNowViewModel,
    /** True when the stock UI is underneath and may be drawing its own copy of the prompt. */
    coverUnderlying: Boolean,
    onHideUpdate: (String) -> Unit,
) {
    val context = LocalContext.current
    when (prompt) {
        TvPrompt.ActiveSession -> {
            val decision = state.activeSessionDecision ?: return
            TvDialog(
                opaqueBackdrop = true,
                onDismiss = viewModel::dismissActiveSessionDecision,
                actions = listOf(
                    TvDialogAction("Resume session", viewModel::resumeActiveSession),
                    TvDialogAction("End it and start ${decision.requestedGameTitle}", viewModel::terminateActiveSessionAndStartNew),
                    TvDialogAction("Cancel", viewModel::dismissActiveSessionDecision),
                ),
            ) {
                TvDialogTitle("A session is already running")
                TvDialogBody(
                    "You can pick up where you left off, or end that session to start ${decision.requestedGameTitle}.",
                )
            }
        }
        TvPrompt.InputMode -> TvDialog(
            opaqueBackdrop = coverUnderlying,
            onDismiss = viewModel::cancelLaunchInputModeChoice,
            actions = listOf(
                TvDialogAction("Use keyboard and mouse") { viewModel.chooseLaunchInputMode(StreamInputMode.KeyboardMouse) },
                TvDialogAction("Keep touch controls") { viewModel.chooseLaunchInputMode(StreamInputMode.NativeTouch) },
                TvDialogAction("Cancel", viewModel::cancelLaunchInputModeChoice),
            ),
        ) {
            TvDialogTitle("How do you want to play?")
            TvDialogBody("This game can be played with touch controls or with a keyboard and mouse.")
        }
        TvPrompt.ServerPicker -> {
            val game = state.pendingPrintedWasteGame ?: return
            val zones = remember(state.printedWasteQueue, state.printedWasteMapping, state.printedWastePings) {
                tvServerOptions(state)
            }
            val auto = remember(zones) { recommendedPrintedWasteZone(zones) }
            TvDialog(
                opaqueBackdrop = coverUnderlying,
                onDismiss = viewModel::dismissPrintedWasteSelector,
                actions = buildList {
                    add(
                        TvDialogAction(
                            auto?.let { "Automatic (${tvZoneLabel(it, state)})" } ?: "Automatic",
                        ) { viewModel.launchWithPrintedWaste(null) },
                    )
                    zones.take(8).forEach { zone ->
                        add(TvDialogAction(tvZoneLabel(zone, state)) { viewModel.launchWithPrintedWaste(zone.routingUrl) })
                    }
                    add(TvDialogAction("Refresh queue times", viewModel::refreshPrintedWasteQueues))
                    add(TvDialogAction("Cancel", viewModel::dismissPrintedWasteSelector))
                },
            ) {
                TvDialogTitle("Choose a server")
                TvDialogBody(
                    when {
                        state.printedWasteLoading -> "Checking queue times…"
                        !state.printedWasteError.isNullOrBlank() -> state.printedWasteError.orEmpty()
                        else -> "Pick where to play ${game.title}. Shorter queues and lower ping are better."
                    },
                )
            }
        }
        TvPrompt.StoreChoice -> {
            val game = state.pendingStoreChoiceGame ?: return
            TvDialog(
                opaqueBackdrop = coverUnderlying,
                onDismiss = viewModel::dismissStoreChoice,
                actions = launchableGameVariants(game.variants).map { variant ->
                    TvDialogAction(tvStoreLabel(variant.store)) { viewModel.playVariant(game, variant) }
                } + TvDialogAction("Cancel", viewModel::dismissStoreChoice),
            ) {
                TvDialogTitle(game.title)
                TvDialogBody("Which store's copy do you want to play?")
            }
        }
        TvPrompt.Membership -> {
            val notice = state.pendingMembershipNotice ?: return
            TvDialog(
                opaqueBackdrop = coverUnderlying,
                onDismiss = viewModel::dismissMembershipNotice,
                actions = listOf(
                    TvDialogAction("Play anyway", viewModel::continuePastMembershipNotice),
                    TvDialogAction("Cancel", viewModel::dismissMembershipNotice),
                ),
            ) {
                TvDialogTitle(notice.game.title)
                TvDialogBody(
                    "This game needs ${notice.requirement.requiredPlanLabel}. Your plan is " +
                        "${notice.requirement.currentPlanLabel}, so it may not start or may run at lower settings.",
                )
            }
        }
        TvPrompt.BatteryOptimization -> {
            val pending = state.pendingBatteryOptimizationLaunch ?: return
            TvDialog(
                opaqueBackdrop = coverUnderlying,
                onDismiss = viewModel::dismissBatteryOptimizationPrompt,
                actions = listOf(
                    TvDialogAction("Open settings") {
                        openBatteryOptimizationSettings(context)
                        viewModel.dismissBatteryOptimizationPrompt()
                    },
                    TvDialogAction("Continue without", viewModel::continueWithoutBatteryOptimization),
                    TvDialogAction("Cancel", viewModel::dismissBatteryOptimizationPrompt),
                ),
            ) {
                TvDialogTitle("Keep your place in the queue")
                TvDialogBody(
                    "Allow GoudaNOW to run in the background so ${pending.game.title} keeps its place " +
                        "in the queue if you leave the app.",
                )
            }
        }
        TvPrompt.LaunchRecovery -> {
            val recovery = state.pendingLaunchRecovery ?: return
            TvDialog(
                opaqueBackdrop = coverUnderlying,
                onDismiss = viewModel::dismissLaunchRecovery,
                actions = listOf(
                    TvDialogAction("Try again at lower settings", viewModel::retryLaunchWithLowerSettings),
                    TvDialogAction("Cancel", viewModel::dismissLaunchRecovery),
                ),
            ) {
                TvDialogTitle("${recovery.game.title} couldn't start")
                TvDialogBody(
                    "${recovery.errorMessage}\n\nTrying again at " +
                        "${recovery.lowerSettings.resolution.replace("x", " × ")}, ${recovery.lowerSettings.fps} fps often helps.",
                )
            }
        }
        TvPrompt.GfnActivation -> TvDialog(
            opaqueBackdrop = coverUnderlying,
            onDismiss = viewModel::dismissGfnMembershipActivation,
            actions = listOf(TvDialogAction("OK", viewModel::dismissGfnMembershipActivation)),
        ) {
            TvDialogTitle("No GeForce NOW membership")
            TvDialogBody("This NVIDIA account doesn't have a GeForce NOW plan yet. Scan the code to choose one, then try again.")
            val qr = remember { QrCode.encodeText(GFN_MEMBERSHIP_URL) }
            if (qr != null) {
                Spacer(Modifier.height(18.dp))
                QrCodeView(qr, Modifier.size(170.dp))
            }
        }
        TvPrompt.SessionReport -> {
            val report = state.sessionReport ?: return
            TvDialog(
                onDismiss = viewModel::dismissSessionReport,
                actions = listOf(
                    TvDialogAction("OK", viewModel::dismissSessionReport),
                    TvDialogAction("Don't show this again") {
                        viewModel.updateSettings(state.settings.copy(showSessionReportAfterStream = false))
                        viewModel.dismissSessionReport()
                    },
                ),
            ) { SessionSummary(report) }
        }
        TvPrompt.Update -> {
            val update = state.androidUpdate
            val key = update.visibleNoticeKey(state.dismissedAndroidUpdateNoticeKey) ?: return
            TvDialog(
                onDismiss = viewModel::dismissAndroidUpdateNotice,
                actions = listOf(
                    TvDialogAction(if (update.status == AndroidUpdateStatus.Downloaded) "Install now" else "Update") {
                        onHideUpdate(key)
                        when (update.status) {
                            AndroidUpdateStatus.Available -> viewModel.performAndroidUpdatePrimaryAction()
                            AndroidUpdateStatus.Downloaded -> viewModel.installAndroidUpdate()
                            else -> Unit
                        }
                    },
                    TvDialogAction("Later", viewModel::dismissAndroidUpdateNotice),
                ),
            ) {
                TvDialogTitle("Update available")
                TvDialogBody(
                    listOfNotNull(
                        update.availableVersionName?.let { "Version $it is ready." },
                        update.releaseNotes?.replace("\\n", "\n")?.take(400),
                    ).joinToString("\n\n").ifBlank { "A new version is ready." },
                )
            }
        }
        TvPrompt.AppMessage -> {
            val message = state.appMessage ?: return
            TvDialog(
                onDismiss = {},
                actions = listOf(TvDialogAction("OK") { viewModel.acknowledgeAppMessage(message) }),
            ) {
                TvDialogTitle(message.title)
                TvDialogBody(message.body.take(700))
            }
        }
    }
}

@Composable
private fun SessionSummary(report: SessionReport) {
    TvDialogTitle(report.gameTitle)
    Spacer(Modifier.height(6.dp))
    Text(
        text = "${report.rating.label} · ${tvDuration(report.durationSeconds)}",
        color = TvSwitchColors.FocusCyan,
        fontSize = 19.sp,
        fontWeight = FontWeight.Medium,
    )
    Spacer(Modifier.height(16.dp))
    listOfNotNull(
        report.averagePingMs?.let { "Ping" to "$it ms" },
        report.averageFps?.let { "Frame rate" to "${it.toInt()} / ${report.targetFps} fps" },
        report.averageBitrateKbps?.let { "Bitrate" to "%.1f Mbps".format(Locale.US, it / 1000.0) },
        report.packetLossPct?.let { "Packet loss" to "%.1f%%".format(Locale.US, it) },
        (report.deliveredResolution ?: report.requestedResolution).let { "Resolution" to it.replace("x", " × ") },
    ).forEach { (label, value) ->
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.width(380.dp)) {
            Text(label, color = TvSwitchColors.SecondaryText, fontSize = 17.sp, modifier = Modifier.weight(1f))
            Text(value, color = TvSwitchColors.PrimaryText, fontSize = 17.sp)
        }
        Spacer(Modifier.height(6.dp))
    }
}

private fun tvDuration(seconds: Int): String {
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    return if (hours > 0) "${hours} h ${minutes} min" else "${minutes.coerceAtLeast(1)} min"
}

private fun tvServerOptions(state: OpenNowUiState): List<PrintedWasteZoneOption> =
    state.printedWasteQueue
        .filter { (zoneId, _) -> isStandardPrintedWasteZone(zoneId) && state.printedWasteMapping[zoneId]?.nuked != true }
        .map { (zoneId, zone) ->
            val url = printedWasteZoneUrl(zoneId)
            PrintedWasteZoneOption(zoneId = zoneId, zone = zone, routingUrl = url, pingMs = state.printedWastePings[url])
        }
        .sortedWith(compareBy<PrintedWasteZoneOption> { it.pingMs ?: Long.MAX_VALUE }.thenBy { it.zone.QueuePosition })

private fun tvZoneLabel(zone: PrintedWasteZoneOption, state: OpenNowUiState): String {
    val title = printedWasteZoneTitle(zone.zoneId, state.printedWasteMapping[zone.zoneId])
    val queue = "queue ${zone.zone.QueuePosition}"
    val ping = zone.pingMs?.let { " · $it ms" }.orEmpty()
    return "$title — $queue$ping"
}

/** Readable store names for the store-choice buttons. Unknown keys fall back to title case. */
internal fun tvStoreLabel(store: String): String {
    val keys = store.split(',', '|', '/').map { it.trim().uppercase(Locale.US) }.filter { it.isNotEmpty() }
    return keys.joinToString(" / ") { key ->
        when (key) {
            "STEAM" -> "Steam"
            "EPIC", "EGS", "EPIC_GAMES_STORE" -> "Epic Games Store"
            "XBOX", "XBOX_GAME_PASS" -> "Xbox"
            "MICROSOFT", "MICROSOFT_STORE" -> "Microsoft Store"
            "UPLAY", "UBISOFT", "UBISOFT_CONNECT" -> "Ubisoft Connect"
            "EA", "ORIGIN", "EA_APP" -> "EA app"
            "BATTLENET", "BATTLE_NET" -> "Battle.net"
            "GOG" -> "GOG"
            "ROCKSTAR" -> "Rockstar Games Launcher"
            else -> key.lowercase(Locale.US).split('_').joinToString(" ") { part ->
                part.replaceFirstChar { it.titlecase(Locale.US) }
            }
        }
    }.ifBlank { "Play" }
}
