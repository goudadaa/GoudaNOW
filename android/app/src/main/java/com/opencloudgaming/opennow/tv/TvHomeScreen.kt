package com.opencloudgaming.opennow.tv

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import coil3.compose.AsyncImage
import com.opencloudgaming.opennow.GAME_BOX_ART_ASPECT_RATIO
import com.opencloudgaming.opennow.GameInfo
import com.opencloudgaming.opennow.OpenNowUiState
import com.opencloudgaming.opennow.isReadyForStream

/** The Switch home shows a short "recent" strip; the full list lives behind All Games. */
private const val HOME_CAROUSEL_LIMIT = 12
private const val FOCUSED_CARD_SCALE = 1.18f
/** Portrait GFN box art (GAME_BOX_ART, 628x888), the same ratio the phone store uses. */
private val MaxCardHeight = 232.dp
private val MinCardHeight = 150.dp
/** Height the carousel needs besides the card: scale overflow, gap and one line of title. */
private val CarouselChrome = 52.dp
private val CardSpacing = 26.dp
private val TileShape = RoundedCornerShape(4.dp)

@Composable
internal fun TvHomeScreen(
    state: OpenNowUiState,
    focusedGameId: String?,
    /** True while a prompt is on top; when it closes, focus goes back to the highlighted game. */
    focusBlocked: Boolean,
    onGameFocused: (GameInfo) -> Unit,
    onGameSelected: (GameInfo) -> Unit,
    onAllGames: () -> Unit,
    onControllers: () -> Unit,
    onSettings: () -> Unit,
    onProfile: () -> Unit,
    onPowerOff: () -> Unit,
    onReturnToQueue: () -> Unit,
) {
    // A launch waiting in the background (queue minimised to HOME) is shown on its own card,
    // first in the row like the Switch's suspended software.
    val queuedGame = state.streamGame?.takeIf { state.streamLaunchMinimized && state.streamStatus != "idle" }
    val queueStatus = queuedGame?.let { tvQueueCardStatus(state) }
    val games = remember(state.libraryGames, queuedGame?.id) {
        // Lazy keys must be unique; merged catalogue sources can repeat an id.
        val library = state.libraryGames.distinctBy { it.id }
        if (queuedGame == null) {
            library.take(HOME_CAROUSEL_LIMIT)
        } else {
            val queuedEntry = library.firstOrNull { it.id == queuedGame.id } ?: queuedGame
            (listOf(queuedEntry) + library.filterNot { it.id == queuedGame.id }).take(HOME_CAROUSEL_LIMIT)
        }
    }
    val allGamesFocus = remember { FocusRequester() }

    TvSwitchTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(TvSwitchColors.Background)
                .padding(tvSafeAreaPadding(state.settings.tvSafeAreaPaddingDp)),
        ) {
            TvTopBar(user = state.authSession?.user, onProfile = onProfile)
            // The carousel takes whatever height is left, and sizes its cards to fit, so the
            // bottom buttons and their labels are never squeezed (larger TV edge padding included).
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            val cardHeight = ((maxHeight - CarouselChrome) / FOCUSED_CARD_SCALE).coerceIn(MinCardHeight, MaxCardHeight)
            when {
                games.isNotEmpty() -> GameCarousel(
                    cardHeight = cardHeight,
                    games = games,
                    initialFocusIndex = games.indexOfFirst { it.id == focusedGameId }.takeIf { it >= 0 } ?: 0,
                    onGameFocused = onGameFocused,
                    onGameSelected = { game ->
                        if (queuedGame != null && game.id == queuedGame.id) onReturnToQueue() else onGameSelected(game)
                    },
                    queuedGameId = queuedGame?.id,
                    queueStatus = queueStatus,
                    focusBlocked = focusBlocked,
                )
                state.loadingGames -> CarouselMessage("Loading your library…")
                else -> {
                    CarouselMessage("No games in your library yet. Open All Games to browse the catalogue.")
                    // Nothing in the row to land on, so start on All Games instead.
                    LaunchedEffect(Unit) {
                        withFrameNanos { }
                        runCatching { allGamesFocus.requestFocus() }
                    }
                }
            }
            }
            state.error?.takeIf { it.isNotBlank() }?.let { message ->
                Text(
                    text = message,
                    color = TvSwitchColors.Error,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 64.dp, vertical = 8.dp),
                )
            }
            TvBottomNav(
                allGamesFocus = allGamesFocus,
                onAllGames = onAllGames,
                onControllers = onControllers,
                onSettings = onSettings,
                onPowerOff = onPowerOff,
            )
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun GameCarousel(
    cardHeight: Dp,
    games: List<GameInfo>,
    initialFocusIndex: Int,
    onGameFocused: (GameInfo) -> Unit,
    onGameSelected: (GameInfo) -> Unit,
    queuedGameId: String?,
    queueStatus: String?,
    focusBlocked: Boolean,
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialFocusIndex)
    val restore = rememberFocusRestore(games.getOrNull(initialFocusIndex)?.id)

    // D-pad lands on the last highlighted game (first game on a fresh start) with no touch or
    // pointer needed: scroll it into composition, then focus that exact card.
    LaunchedEffect(focusBlocked) {
        if (focusBlocked) restore.arm() else restore.run(games) { index ->
            if (listState.layoutInfo.visibleItemsInfo.none { it.index == index }) listState.scrollToItem(index)
        }
    }

    LazyRow(
        state = listState,
        // Room above/below for the 1.18x scale and glow, and for the title under the card.
        // Top padding leaves room for the focused card's 1.18x scale and glow.
        contentPadding = PaddingValues(
            start = 64.dp,
            end = 64.dp,
            top = cardHeight * ((FOCUSED_CARD_SCALE - 1f) / 2f) + 6.dp,
            bottom = 4.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(CardSpacing),
        modifier = Modifier
            .fillMaxWidth()
            // Coming back up from the bottom row returns to the last focused game, not index 0.
            .focusRestorer(),
    ) {
        itemsIndexed(games, key = { _, game -> game.id }) { index, game ->
            GameCard(
                game = game,
                cardHeight = cardHeight,
                modifier = restore.modifierFor(game),
                onFocused = { restore.onItemFocused(game) { onGameFocused(game) } },
                onClick = { onGameSelected(game) },
                queueStatus = if (game.id == queuedGameId) queueStatus else null,
            )
        }
    }
}

@Composable
private fun GameCard(
    game: GameInfo,
    cardHeight: Dp,
    modifier: Modifier,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    queueStatus: String?,
) {
    val cardWidth = cardHeight * GAME_BOX_ART_ASPECT_RATIO
    var focused by remember { mutableStateOf(false) }
    val titleAlpha by animateFloatAsState(if (focused) 1f else 0f, label = "gameTitleAlpha")

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(cardWidth).zIndex(if (focused) 1f else 0f),
    ) {
        Card(
            onClick = onClick,
            shape = CardDefaults.shape(shape = TileShape),
            scale = CardDefaults.scale(focusedScale = FOCUSED_CARD_SCALE),
            border = CardDefaults.border(
                focusedBorder = Border(
                    border = BorderStroke(4.dp, TvSwitchColors.FocusCyan),
                    shape = TileShape,
                ),
            ),
            glow = CardDefaults.glow(
                focusedGlow = Glow(elevationColor = TvSwitchColors.FocusGlow, elevation = 18.dp),
            ),
            colors = CardDefaults.colors(containerColor = TvSwitchColors.TilePlaceholder),
            modifier = modifier
                .width(cardWidth)
                .height(cardHeight)
                .onFocusChanged { focusState ->
                    focused = focusState.isFocused
                    if (focusState.isFocused) onFocused()
                },
        ) {
            Box(Modifier.fillMaxSize()) {
                AsyncImage(
                    // imageUrl is the uncropped portrait box art; tvCardImageUrl may fall back to wide art.
                    model = game.imageUrl ?: game.tvCardImageUrl,
                    contentDescription = game.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                if (queueStatus != null) QueueCardOverlay(queueStatus)
            }
        }
        // Clears the scaled card: (1.18 - 1) / 2 of the card height, plus a gap.
        Spacer(Modifier.height(cardHeight * ((FOCUSED_CARD_SCALE - 1f) / 2f) + 8.dp))
        Text(
            text = game.title,
            color = TvSwitchColors.FocusText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            // Wider than the card so longer titles still fit under the enlarged tile.
            modifier = Modifier.requiredWidth(cardWidth * 2f).alpha(titleAlpha),
        )
    }
}

@Composable
private fun CarouselMessage(text: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth().height(MinCardHeight),
    ) {
        Text(
            text = text,
            color = TvSwitchColors.SecondaryText,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
    }
}

/** Dimmed artwork with a spinner and the queue state, drawn over the waiting game's card. */
@Composable
private fun QueueCardOverlay(status: String) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize().background(TvSwitchColors.Scrim.copy(alpha = 0.62f)),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = TvSwitchColors.FocusCyan,
                strokeWidth = 4.dp,
                modifier = Modifier.size(44.dp),
            )
            Spacer(Modifier.height(14.dp))
            Text(
                text = status,
                color = TvSwitchColors.PrimaryText,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 10.dp),
            )
        }
    }
}

internal fun tvQueueCardStatus(state: OpenNowUiState): String {
    val position = state.queuePosition?.takeIf { it > 0 }
    return when {
        position != null -> "#$position in queue"
        state.streamSession?.isReadyForStream() == true -> "Ready\nPress A to play"
        else -> "Getting ready"
    }
}
