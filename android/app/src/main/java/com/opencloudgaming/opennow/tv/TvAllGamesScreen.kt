package com.opencloudgaming.opennow.tv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.Icon
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.opencloudgaming.opennow.GAME_BOX_ART_ASPECT_RATIO
import com.opencloudgaming.opennow.GameInfo
import com.opencloudgaming.opennow.LIBRARY_SORT_DEFAULT
import com.opencloudgaming.opennow.LIBRARY_SORT_RECENT
import com.opencloudgaming.opennow.LIBRARY_SORT_TITLE
import com.opencloudgaming.opennow.OpenNowUiState
import com.opencloudgaming.opennow.favoriteOrderedGames
import com.opencloudgaming.opennow.gameMatchesSearch
import com.opencloudgaming.opennow.sortLibraryGames

private const val GRID_COLUMNS = 6
private const val FOCUSED_TILE_SCALE = 1.08f
private val TileShape = RoundedCornerShape(4.dp)

/** Sort order cycled with Y (or the Sort button), using the library's own sort ids. */
private val LibrarySortCycle = listOf(
    LIBRARY_SORT_DEFAULT to "Library order",
    LIBRARY_SORT_RECENT to "Recently played",
    LIBRARY_SORT_TITLE to "Title",
)

/** Switch "All Software"-style grid of every linked game, in portrait box art. */
@Composable
internal fun TvAllGamesScreen(
    state: OpenNowUiState,
    focusedGameId: String?,
    /** True while a prompt is on top; when it closes, focus goes back to the highlighted game. */
    focusBlocked: Boolean,
    onGameFocused: (GameInfo) -> Unit,
    onGameSelected: (GameInfo) -> Unit,
    onSortSelected: (String) -> Unit,
    onSearchChange: (String) -> Unit,
    onBack: () -> Unit,
) {
    var searchOpen by rememberSaveable { mutableStateOf(state.librarySearch.isNotBlank()) }
    val searchFocus = remember { FocusRequester() }
    BackHandler(onBack = onBack)
    // Registered after the screen's handler, so while the search bar is open Back closes it first.
    BackHandler(enabled = searchOpen) {
        searchOpen = false
        onSearchChange("")
    }
    // Same ordering the phone Library uses: favourites first, then the chosen sort.
    val games = remember(state.libraryGames, state.settings.favoriteGameIds, state.librarySortId) {
        sortLibraryGames(
            favoriteOrderedGames(state.libraryGames, state.settings.favoriteGameIds),
            state.librarySortId,
        ).distinctBy { it.id }
    }
    val visibleGames = remember(games, state.librarySearch) {
        if (state.librarySearch.isBlank()) games else games.filter { gameMatchesSearch(it, state.librarySearch) }
    }
    val sortIndex = LibrarySortCycle.indexOfFirst { it.first == state.librarySortId }.coerceAtLeast(0)
    val cycleSort = { onSortSelected(LibrarySortCycle[(sortIndex + 1) % LibrarySortCycle.size].first) }
    val focusedIndex = visibleGames.indexOfFirst { it.id == focusedGameId }.takeIf { it >= 0 } ?: 0
    val focusedTitle = visibleGames.getOrNull(focusedIndex)?.title.orEmpty()
    val openSearch = { searchOpen = true }

    TvSwitchScreenFrame(
        title = "All Games",
        icon = Icons.Rounded.Apps,
        safeAreaDp = state.settings.tvSafeAreaPaddingDp,
        hints = listOf(
            TvButtonHint(TvButtonGlyph.X, "Search"),
            TvButtonHint(TvButtonGlyph.Y, "Sort"),
            TvButtonHint(TvButtonGlyph.B, "Back"),
            TvButtonHint(TvButtonGlyph.A, "Start"),
        ),
        headerCenter = {
            // key() restarts the scroll from the start each time focus moves to another game.
            key(focusedTitle) {
                Text(
                    text = focusedTitle,
                    color = TvSwitchColors.FocusCyan,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    // Titles too long for the space scroll slowly so the whole name can be read.
                    modifier = Modifier.basicMarquee(
                        iterations = Int.MAX_VALUE,
                        initialDelayMillis = 1_200,
                        repeatDelayMillis = 2_000,
                    ),
                )
            }
        },
        headerTrailing = {
            HeaderPill(label = "Search", icon = Icons.Rounded.Search, onClick = openSearch)
            Spacer(Modifier.width(12.dp))
            HeaderPill(label = "Sort: ${LibrarySortCycle[sortIndex].second}", icon = null, onClick = cycleSort)
        },
    ) {
        if (searchOpen) {
            TvSearchBar(
                query = state.librarySearch,
                onQueryChange = onSearchChange,
                focusRequester = searchFocus,
                resultCount = visibleGames.size,
            )
        }
        when {
            visibleGames.isNotEmpty() -> GameGrid(
                games = visibleGames,
                initialIndex = focusedIndex.coerceIn(0, visibleGames.lastIndex),
                // While typing, results must not pull focus away from the search field.
                autoFocus = !searchOpen && !focusBlocked,
                blocked = focusBlocked,
                onFocused = onGameFocused,
                onGameSelected = onGameSelected,
                onSortKey = cycleSort,
                onSearchKey = openSearch,
            )
            else -> Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(
                    text = when {
                        state.loadingGames -> "Loading your library…"
                        state.librarySearch.isNotBlank() -> "No games match “${state.librarySearch}”."
                        else -> "No games in your library yet."
                    },
                    color = TvSwitchColors.SecondaryText,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun GameGrid(
    games: List<GameInfo>,
    initialIndex: Int,
    onFocused: (GameInfo) -> Unit,
    onGameSelected: (GameInfo) -> Unit,
    onSortKey: () -> Unit,
    onSearchKey: () -> Unit,
    autoFocus: Boolean,
    blocked: Boolean,
) {
    val gridState = rememberLazyGridState(initialFirstVisibleItemIndex = initialIndex)
    val restore = rememberFocusRestore(games.getOrNull(initialIndex)?.id)
    // Returns to the last highlighted game. Re-runs when the search bar closes, so focus lands
    // back on a game instead of nowhere.
    LaunchedEffect(autoFocus, blocked) {
        when {
            autoFocus -> restore.run(games) { index ->
                // Only jump when the game is off screen, so closing a dialog doesn't shift the grid.
                if (gridState.layoutInfo.visibleItemsInfo.none { it.index == index }) gridState.scrollToItem(index)
            }
            blocked -> restore.arm()
            else -> restore.release() // search bar open: every focus change is the user's
        }
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(GRID_COLUMNS),
        state = gridState,
        contentPadding = PaddingValues(horizontal = 56.dp, vertical = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(22.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        modifier = Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                // Y is not a D-pad key, so MainActivity passes it straight through to Compose.
                when {
                    event.type != KeyEventType.KeyUp -> false
                    event.key == Key.ButtonY -> { onSortKey(); true }
                    event.key == Key.ButtonX -> { onSearchKey(); true }
                    else -> false
                }
            },
    ) {
        itemsIndexed(games, key = { _, game -> game.id }) { index, game ->
            GridTile(
                game = game,
                modifier = restore.modifierFor(game),
                onFocused = { restore.onItemFocused(game) { onFocused(game) } },
                onClick = { onGameSelected(game) },
            )
        }
    }
}

@Composable
private fun GridTile(game: GameInfo, modifier: Modifier, onFocused: () -> Unit, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    Card(
        onClick = onClick,
        shape = CardDefaults.shape(shape = TileShape),
        scale = CardDefaults.scale(focusedScale = FOCUSED_TILE_SCALE),
        border = CardDefaults.border(
            focusedBorder = Border(BorderStroke(4.dp, TvSwitchColors.FocusCyan), shape = TileShape),
        ),
        glow = CardDefaults.glow(focusedGlow = Glow(TvSwitchColors.FocusGlow, 12.dp)),
        colors = CardDefaults.colors(containerColor = TvSwitchColors.TilePlaceholder),
        modifier = modifier
            .zIndex(if (focused) 1f else 0f)
            .fillMaxWidth()
            .aspectRatio(GAME_BOX_ART_ASPECT_RATIO)
            .onFocusChanged {
                focused = it.isFocused
                if (it.isFocused) onFocused()
            },
    ) {
        Box(Modifier.fillMaxSize()) {
            AsyncImage(
                model = game.imageUrl ?: game.tvCardImageUrl,
                contentDescription = game.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun HeaderPill(label: String, icon: ImageVector?, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = RoundedCornerShape(50)),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = TvSwitchColors.NavButton,
            contentColor = TvSwitchColors.PrimaryText,
            focusedContainerColor = TvSwitchColors.NavButtonFocused,
            focusedContentColor = TvSwitchColors.FocusCyan,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.05f),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(3.dp, TvSwitchColors.FocusCyan), shape = RoundedCornerShape(50)),
        ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(label, fontSize = 16.sp)
        }
    }
}

/**
 * Search field for the TV's on-screen keyboard. It opens focused with the keyboard raised; the
 * keyboard's Search key hides it and moves down to the results. Filtering uses the same matcher
 * and the same `librarySearch` state as the phone Library.
 */
@Composable
private fun TvSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    focusRequester: FocusRequester,
    resultCount: Int,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        runCatching { focusRequester.requestFocus() }
        keyboard?.show()
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(start = 56.dp, end = 56.dp, top = 16.dp),
    ) {
        BasicTextField(
            value = query,
            onValueChange = onQueryChange,
            singleLine = true,
            textStyle = TextStyle(color = TvSwitchColors.PrimaryText, fontSize = 20.sp),
            cursorBrush = SolidColor(TvSwitchColors.FocusCyan),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    keyboard?.hide()
                    focusManager.moveFocus(FocusDirection.Down)
                },
            ),
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .onFocusChanged { focused = it.isFocused },
            decorationBox = { innerField ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TvSwitchColors.RowFocused, RoundedCornerShape(24.dp))
                        .border(
                            width = if (focused) 3.dp else 1.dp,
                            color = if (focused) TvSwitchColors.FocusCyan else TvSwitchColors.Divider,
                            shape = RoundedCornerShape(24.dp),
                        )
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                ) {
                    Icon(Icons.Rounded.Search, contentDescription = null, tint = TvSwitchColors.SecondaryText, modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(12.dp))
                    Box(Modifier.weight(1f)) {
                        if (query.isEmpty()) {
                            Text("Search your games", color = TvSwitchColors.SecondaryText, fontSize = 20.sp)
                        }
                        innerField()
                    }
                }
            },
        )
        Spacer(Modifier.width(20.dp))
        Text(
            text = if (query.isBlank()) "" else "$resultCount found",
            color = TvSwitchColors.SecondaryText,
            fontSize = 17.sp,
        )
    }
}
