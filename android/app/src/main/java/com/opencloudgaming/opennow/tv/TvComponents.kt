package com.opencloudgaming.opennow.tv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import com.opencloudgaming.opennow.GameInfo
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text

/*
 * Shared building blocks for the Switch-style TV screens: the header/divider/footer frame used by
 * System Settings and All Software, focus-ring list rows, and a focus-trapping modal dialog.
 */

internal val TvRowShape = RoundedCornerShape(3.dp)

/** Letters for the footer button hints. Generic face-button glyphs, not console artwork. */
internal enum class TvButtonGlyph(val letter: String) { A("A"), B("B"), X("X"), Y("Y") }

internal data class TvButtonHint(val glyph: TvButtonGlyph, val label: String)

/**
 * Full-screen frame: icon + title header, thin divider, body, divider, and right-aligned
 * button hints — the chrome every Switch system screen shares.
 */
@Composable
internal fun TvSwitchScreenFrame(
    title: String,
    icon: ImageVector?,
    safeAreaDp: Float,
    hints: List<TvButtonHint>,
    /** Fills the space between the title and [headerTrailing], so trailing controls stay put. */
    headerCenter: (@Composable () -> Unit)? = null,
    headerTrailing: @Composable RowScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    TvSwitchTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(TvSwitchColors.Background)
                .padding(tvSafeAreaPadding(safeAreaDp)),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 40.dp),
            ) {
                if (icon != null) {
                    Icon(icon, contentDescription = null, tint = TvSwitchColors.PrimaryText, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.width(16.dp))
                }
                Text(title, color = TvSwitchColors.PrimaryText, fontSize = 26.sp, fontWeight = FontWeight.Medium)
                if (headerCenter != null) {
                    Spacer(Modifier.width(20.dp))
                    Box(Modifier.width(1.dp).height(28.dp).background(TvSwitchColors.Divider))
                    Spacer(Modifier.width(20.dp))
                    Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) { headerCenter() }
                    Spacer(Modifier.width(24.dp))
                } else {
                    Spacer(Modifier.weight(1f))
                }
                headerTrailing()
            }
            TvDivider()
            Column(Modifier.weight(1f).fillMaxWidth()) { content() }
            TvDivider()
            TvButtonHints(hints, Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 40.dp))
        }
    }
}

@Composable
internal fun TvDivider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(1.dp)
            .background(TvSwitchColors.Divider),
    )
}

@Composable
internal fun TvButtonHints(hints: List<TvButtonHint>, modifier: Modifier = Modifier) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,
    ) {
        hints.forEach { hint ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(24.dp)
                        .background(TvSwitchColors.PrimaryText, CircleShape),
                ) {
                    Text(hint.glyph.letter, color = TvSwitchColors.Background, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(8.dp))
                Text(hint.label, color = TvSwitchColors.PrimaryText, fontSize = 17.sp)
            }
        }
    }
}

/**
 * A focusable row with the Switch cyan focus ring. [value] renders right-aligned, like the
 * current value of a System Settings option.
 */
@Composable
internal fun TvListRow(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    value: String? = null,
    selected: Boolean = false,
    labelColor: Color = TvSwitchColors.PrimaryText,
    minHeight: Dp = 56.dp,
    leading: (@Composable () -> Unit)? = null,
) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = TvRowShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = labelColor,
            focusedContainerColor = TvSwitchColors.RowFocused,
            focusedContentColor = labelColor,
            pressedContainerColor = TvSwitchColors.RowFocused,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(3.dp, TvSwitchColors.FocusCyan), shape = TvRowShape),
        ),
        modifier = modifier.fillMaxWidth().heightIn(min = minHeight),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().heightIn(min = minHeight).padding(horizontal = 20.dp, vertical = 8.dp),
        ) {
            if (selected) {
                // Selected category marker: the cyan bar at the left edge of the Settings list.
                Box(Modifier.width(4.dp).height(28.dp).background(TvSwitchColors.FocusCyan))
                Spacer(Modifier.width(14.dp))
            }
            if (leading != null) {
                leading()
                Spacer(Modifier.width(14.dp))
            }
            Text(
                text = label,
                color = if (selected) TvSwitchColors.FocusCyan else labelColor,
                fontSize = 19.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            if (value != null) {
                Spacer(Modifier.width(16.dp))
                Text(value, color = TvSwitchColors.FocusCyan, fontSize = 18.sp, maxLines = 1)
            }
        }
    }
}

/** Section caption inside a settings panel. */
@Composable
internal fun TvSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = TvSwitchColors.SecondaryText,
        fontSize = 15.sp,
        modifier = modifier.padding(start = 20.dp, top = 18.dp, bottom = 6.dp),
    )
}

internal data class TvDialogAction(val label: String, val onClick: () -> Unit)

/**
 * Switch-style modal: centred grey panel, message on top, full-width button row divided by thin
 * rules. Focus is trapped inside so the D-pad can never land on hidden content underneath.
 */
@Composable
internal fun TvDialog(
    actions: List<TvDialogAction>,
    onDismiss: () -> Unit,
    opaqueBackdrop: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    // Composed after whatever sits underneath, so this Back handler wins while the dialog shows.
    BackHandler(onBack = onDismiss)
    val firstAction = remember { FocusRequester() }
    var hasFocus by remember { mutableStateOf(false) }
    // Also reclaims focus if the stock screen underneath grabs it after this dialog appears.
    LaunchedEffect(hasFocus) {
        if (!hasFocus) {
            withFrameNanos { }
            runCatching { firstAction.requestFocus() }
        }
    }
    TvSwitchTheme {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(if (opaqueBackdrop) TvSwitchColors.Background else TvSwitchColors.Scrim),
        ) {
            Column(
                modifier = Modifier
                    .widthIn(min = 480.dp, max = 640.dp)
                    .heightIn(max = 500.dp)
                    .background(TvSwitchColors.DialogPanel, RoundedCornerShape(6.dp))
                    .border(1.dp, TvSwitchColors.Divider, RoundedCornerShape(6.dp))
                    .onFocusChanged { hasFocus = it.hasFocus }
                    .focusProperties { onExit = { cancelFocusChange() } }
                    .focusGroup(),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 36.dp, vertical = 28.dp),
                    content = content,
                )
                Box(Modifier.fillMaxWidth().height(1.dp).background(TvSwitchColors.Divider))
                // Long action lists (e.g. the server picker) scroll; focus keeps the chosen row in view.
                Column(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    actions.forEachIndexed { index, action ->
                        TvDialogButton(
                            label = action.label,
                            onClick = action.onClick,
                            modifier = if (index == 0) Modifier.focusRequester(firstAction) else Modifier,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TvDialogButton(label: String, onClick: () -> Unit, modifier: Modifier) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = TvRowShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = Color.Transparent,
            contentColor = TvSwitchColors.FocusCyan,
            focusedContainerColor = TvSwitchColors.RowFocused,
            focusedContentColor = TvSwitchColors.FocusCyan,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(3.dp, TvSwitchColors.FocusCyan), shape = TvRowShape),
        ),
        modifier = modifier.fillMaxWidth().height(56.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(label, fontSize = 20.sp, textAlign = TextAlign.Center)
        }
    }
}

@Composable
internal fun TvDialogTitle(text: String) {
    Text(text, color = TvSwitchColors.PrimaryText, fontSize = 24.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center)
}

@Composable
internal fun TvDialogBody(text: String) {
    Spacer(Modifier.height(14.dp))
    Text(text, color = TvSwitchColors.SecondaryText, fontSize = 18.sp, textAlign = TextAlign.Center)
}

internal val TvContentPadding = PaddingValues(horizontal = 40.dp, vertical = 16.dp)

/**
 * Requests focus until it sticks. Returning from a stream or dialog, the target item may not be
 * composed on the first frame and Android otherwise gives focus to the first control on screen
 * (the profile icon). Stops as soon as [isFocused] reports success, so it never fights the user.
 */
internal suspend fun FocusRequester.requestFocusUntilFocused(isFocused: () -> Boolean, attempts: Int = 30) {
    repeat(attempts) {
        withFrameNanos { }
        runCatching { requestFocus() }
        withFrameNanos { }
        if (isFocused()) return
    }
}

/**
 * Puts focus back on one specific game in a lazy row or grid.
 *
 * When the screen comes back, Android gives focus to the first visible card (the start of the
 * row) during the very first layout — before any effect has run. That focus change must not be
 * recorded as the user's choice, so the restore starts "armed": focus changes are ignored from
 * construction until [run] has put focus on the exact target card (or [release] is called
 * because the screen does not want auto-focus, e.g. while searching).
 */
@Stable
internal class FocusRestore(initialTargetId: String?) {
    val requester = FocusRequester()
    var targetId by mutableStateOf(initialTargetId)
        private set
    private var focusedId by mutableStateOf<String?>(null)
    private var restoring by mutableStateOf(initialTargetId != null)

    fun modifierFor(game: GameInfo): Modifier =
        if (game.id == targetId) Modifier.focusRequester(requester) else Modifier

    /** Call from each card's focus callback; [record] saves the game as the new highlight. */
    fun onItemFocused(game: GameInfo, record: () -> Unit) {
        focusedId = game.id
        if (!restoring) {
            targetId = game.id
            record()
        }
    }

    /**
     * Start ignoring focus changes, e.g. while a dialog covers the list. When the dialog closes
     * Android hands focus to the first visible card before [run] can act; armed, that is ignored.
     */
    fun arm() {
        restoring = true
    }

    /** Stop ignoring focus changes without moving focus. */
    fun release() {
        restoring = false
    }

    suspend fun run(games: List<GameInfo>, scrollTo: suspend (Int) -> Unit) {
        if (games.isEmpty()) {
            restoring = false
            return
        }
        val index = games.indexOfFirst { it.id == targetId }.takeIf { it >= 0 } ?: 0
        val target = games[index].id
        targetId = target
        restoring = true
        try {
            scrollTo(index)
            requester.requestFocusUntilFocused(isFocused = { focusedId == target })
        } finally {
            restoring = false
        }
    }
}

@Composable
internal fun rememberFocusRestore(initialTargetId: String?): FocusRestore =
    remember { FocusRestore(initialTargetId) }
