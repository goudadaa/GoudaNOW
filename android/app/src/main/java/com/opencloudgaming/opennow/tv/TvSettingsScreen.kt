package com.opencloudgaming.opennow.tv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.opencloudgaming.opennow.BuildConfig
import com.opencloudgaming.opennow.OpenNowUiState
import com.opencloudgaming.opennow.OpenNowViewModel
import com.opencloudgaming.opennow.SettingsRouteTarget
import com.opencloudgaming.opennow.StreamPreset
import com.opencloudgaming.opennow.VideoCodec
import com.opencloudgaming.opennow.streamResolutionOptionsForAspect

internal enum class TvSettingsCategory(val label: String) {
    Account("User"),
    Streaming("Streaming"),
    Controllers("Controllers"),
    Display("TV Display"),
    About("System"),
    More("All Settings"),
}

private val FpsCycle = listOf(30, 60, 90, 120)
private val BitrateCycle = listOf(10, 15, 20, 25, 30, 40, 50, 60, 75, 100)
private val SafeAreaCycle = listOf(0f, 8f, 16f, 24f, 32f, 48f)
private val PresetCycle = listOf(StreamPreset.Recommended, StreamPreset.LowDataSaver, StreamPreset.Medium, StreamPreset.High)

/**
 * Switch "System Settings": categories on the left, options on the right. Moving focus over a
 * category switches the panel, as on the console. Each row writes through the same ViewModel
 * setters the phone settings use, so validation (membership caps, HDR support) still applies.
 * Anything not covered here stays reachable through "All Settings".
 */
@Composable
internal fun TvSettingsScreen(
    state: OpenNowUiState,
    viewModel: OpenNowViewModel,
    category: TvSettingsCategory,
    onCategoryChange: (TvSettingsCategory) -> Unit,
    /** Opens the stock settings screen, optionally at a specific section. */
    onOpenFullSettings: (SettingsRouteTarget?) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val openAll = { onOpenFullSettings(null) }
    var confirmSignOut by remember { mutableStateOf(false) }
    val firstCategory = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        runCatching { firstCategory.requestFocus() }
    }

    TvSwitchScreenFrame(
        title = "System Settings",
        icon = Icons.Rounded.Settings,
        safeAreaDp = state.settings.tvSafeAreaPaddingDp,
        hints = listOf(TvButtonHint(TvButtonGlyph.B, "Back"), TvButtonHint(TvButtonGlyph.A, "OK")),
    ) {
        Row(Modifier.fillMaxSize()) {
            LazyColumn(
                contentPadding = PaddingValues(start = 24.dp, end = 12.dp, top = 16.dp, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.width(300.dp).fillMaxHeight(),
            ) {
                TvSettingsCategory.entries.forEach { entry ->
                    item(key = entry.name) {
                        TvListRow(
                            label = entry.label,
                            selected = entry == category,
                            onClick = {
                                if (entry == TvSettingsCategory.More) openAll() else onCategoryChange(entry)
                            },
                            modifier = (if (entry == category) Modifier.focusRequester(firstCategory) else Modifier)
                                .onFocusChanged { if (it.isFocused && entry != TvSettingsCategory.More) onCategoryChange(entry) },
                        )
                    }
                }
            }
            Box(Modifier.width(1.dp).fillMaxHeight().padding(vertical = 16.dp).background(TvSwitchColors.Divider))
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1f).fillMaxHeight(),
            ) {
                item(key = "panel-${category.name}") {
                    Column {
                        when (category) {
                            TvSettingsCategory.Account -> AccountPanel(
                                state = state,
                                viewModel = viewModel,
                                onAccountDetails = { onOpenFullSettings(SettingsRouteTarget.Account) },
                                onSignOut = { confirmSignOut = true },
                            )
                            TvSettingsCategory.Streaming -> StreamingPanel(state, viewModel)
                            TvSettingsCategory.Controllers -> ControllersPanel(state, viewModel, onStreamInput = { onOpenFullSettings(SettingsRouteTarget.Stream) })
                            TvSettingsCategory.Display -> DisplayPanel(state, viewModel)
                            TvSettingsCategory.About -> AboutPanel(openAll)
                            TvSettingsCategory.More -> MorePanel(openAll)
                        }
                    }
                }
            }
        }
    }

    if (confirmSignOut) {
        TvDialog(
            onDismiss = { confirmSignOut = false },
            actions = listOf(
                TvDialogAction("Cancel") { confirmSignOut = false },
                TvDialogAction("Sign out") {
                    confirmSignOut = false
                    viewModel.logout()
                },
            ),
        ) {
            TvDialogTitle("Sign out?")
            TvDialogBody("You'll need to sign in again with a code to play.")
        }
    }
}

@Composable
private fun AccountPanel(
    state: OpenNowUiState,
    viewModel: OpenNowViewModel,
    onAccountDetails: () -> Unit,
    onSignOut: () -> Unit,
) {
    val user = state.authSession?.user
    val tier = state.subscriptionInfo?.membershipTier ?: user?.membershipTier
    TvSectionLabel("Signed in")
    TvListRow(label = user?.displayName ?: "Not signed in", value = tier?.let(::tvTierLabel), onClick = onAccountDetails)
    state.subscriptionInfo?.takeIf { it.totalHours > 0.0 }?.let { info ->
        TvListRow(label = "Play time left", value = "%.1f h".format(info.remainingHours), onClick = viewModel::refreshGfnMembership)
    }
    val others = state.savedAccounts.filter { it.userId != user?.userId }
    if (others.isNotEmpty()) {
        TvSectionLabel("Switch user")
        others.forEach { account ->
            TvListRow(label = account.displayName, value = tvTierLabel(account.membershipTier), onClick = { viewModel.switchAccount(account.userId) })
        }
    }
    TvSectionLabel("Account")
    TvListRow(label = "Sign out", labelColor = TvSwitchColors.Error, onClick = onSignOut)
}

@Composable
private fun StreamingPanel(state: OpenNowUiState, viewModel: OpenNowViewModel) {
    val settings = state.settings
    val stream = settings.stream
    TvSectionLabel("Quality")
    TvListRow(
        label = "Preset",
        value = tvPresetLabel(settings.streamPreset),
        onClick = { viewModel.applyStreamPreset(PresetCycle.nextAfter(settings.streamPreset)) },
    )
    val resolutions = streamResolutionOptionsForAspect(stream.aspectRatio).ifEmpty { listOf(stream.resolution) }
    TvListRow(
        label = "Resolution",
        value = stream.resolution.replace("x", " × "),
        onClick = { viewModel.updateStreamSettings { it.copy(resolution = resolutions.nextAfter(it.resolution)) } },
    )
    TvListRow(
        label = "Frame rate",
        value = "${stream.fps} fps",
        onClick = { viewModel.updateStreamSettings { it.copy(fps = FpsCycle.nextAfter(it.fps)) } },
    )
    TvListRow(
        label = "Maximum bitrate",
        value = "${stream.maxBitrateMbps} Mbps",
        onClick = { viewModel.updateStreamSettings { it.copy(maxBitrateMbps = BitrateCycle.nextAfter(it.maxBitrateMbps)) } },
    )
    TvListRow(
        label = "Video codec",
        value = tvCodecLabel(stream.codec),
        onClick = { viewModel.updateStreamSettings { it.copy(codec = VideoCodec.entries.nextAfter(it.codec)) } },
    )
    TvListRow(
        label = "HDR",
        value = onOff(stream.hdrEnabled),
        onClick = { viewModel.updateStreamSettings { it.copy(hdrEnabled = !it.hdrEnabled) } },
    )
    TvSectionLabel("While streaming")
    TvListRow(
        label = "Show stats when a game starts",
        value = onOff(settings.showStatsOnLaunch),
        onClick = { viewModel.updateSettings(settings.copy(showStatsOnLaunch = !settings.showStatsOnLaunch)) },
    )
}

@Composable
private fun ControllersPanel(state: OpenNowUiState, viewModel: OpenNowViewModel, onStreamInput: () -> Unit) {
    val settings = state.settings
    TvSectionLabel("Controllers")
    TvListRow(
        label = "Vibration",
        value = onOff(settings.vibrationEnabled),
        onClick = { viewModel.updateSettings(settings.copy(vibrationEnabled = !settings.vibrationEnabled)) },
    )
    TvListRow(
        label = "Use controller as mouse",
        value = onOff(settings.controllerMouseEmulation),
        onClick = { viewModel.updateSettings(settings.copy(controllerMouseEmulation = !settings.controllerMouseEmulation)) },
    )
    TvListRow(
        label = "Menu sounds",
        value = onOff(settings.controllerUiSounds),
        onClick = { viewModel.updateSettings(settings.copy(controllerUiSounds = !settings.controllerUiSounds)) },
    )
    TvSectionLabel("Buttons and shortcuts")
    TvListRow(label = "Stream input settings", onClick = onStreamInput)
}

@Composable
private fun DisplayPanel(state: OpenNowUiState, viewModel: OpenNowViewModel) {
    val settings = state.settings
    TvSectionLabel("Screen")
    TvListRow(
        label = "Screen edge padding",
        value = "${settings.tvSafeAreaPaddingDp.toInt()} dp",
        onClick = { viewModel.updateSettings(settings.copy(tvSafeAreaPaddingDp = SafeAreaCycle.nextAfter(settings.tvSafeAreaPaddingDp))) },
    )
    Text(
        text = "Increase this if the edges of the menu are cut off on your TV.",
        color = TvSwitchColors.SecondaryText,
        fontSize = 15.sp,
        modifier = Modifier.padding(start = 20.dp, top = 6.dp),
    )
}

@Composable
private fun AboutPanel(onOpenFullSettings: () -> Unit) {
    TvSectionLabel("System")
    TvListRow(label = "Version", value = "GoudaNOW ${BuildConfig.VERSION_NAME}", onClick = {})
    TvSectionLabel("About")
    TvListRow(label = "Based on OpenNOW by OpenCloudGaming", value = "MIT licence", onClick = {})
    TvListRow(label = "Not affiliated with NVIDIA or Nintendo", onClick = {})
    TvListRow(label = "Updates, diagnostics and more", onClick = onOpenFullSettings)
}

@Composable
private fun MorePanel(onOpenFullSettings: () -> Unit) {
    TvSectionLabel("All Settings")
    TvListRow(label = "Open all settings", onClick = onOpenFullSettings)
}

private fun <T> List<T>.nextAfter(current: T): T {
    val index = indexOf(current)
    return if (index < 0) first() else this[(index + 1) % size]
}

private fun onOff(value: Boolean) = if (value) "On" else "Off"

private fun tvPresetLabel(preset: StreamPreset): String = when (preset) {
    StreamPreset.Recommended -> "Recommended"
    StreamPreset.Custom -> "Custom"
    StreamPreset.LowDataSaver -> "Data saver"
    StreamPreset.Medium -> "Balanced"
    StreamPreset.High -> "High"
}

private fun tvCodecLabel(codec: VideoCodec): String = when (codec) {
    VideoCodec.H264 -> "H.264"
    VideoCodec.H265 -> "H.265 (HEVC)"
    VideoCodec.AV1 -> "AV1"
}

internal fun tvTierLabel(tier: String): String =
    tier.lowercase().split('_', ' ').filter { it.isNotBlank() }.joinToString(" ") { part ->
        part.replaceFirstChar { it.uppercase() }
    }
