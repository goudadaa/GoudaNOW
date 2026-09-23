package com.opencloudgaming.opennow.tv

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper
import android.text.format.DateFormat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SettingsEthernet
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Glow
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import androidx.tv.material3.darkColorScheme
import coil3.compose.AsyncImage
import com.opencloudgaming.opennow.AuthUser
import java.util.Date
import kotlinx.coroutines.delay

/** Dark home-menu palette. Focus is the cyan selection ring the home menu is known for. */
internal object TvSwitchColors {
    val Background = Color(0xFF2D2D2D)
    val TilePlaceholder = Color(0xFF3A3A3A)
    val NavButton = Color(0xFF3C3C3C)
    val NavButtonFocused = Color(0xFF4A4A4A)
    val Divider = Color(0xFF4B4B4B)
    val PrimaryText = Color(0xFFFFFFFF)
    val SecondaryText = Color(0xFFB4B4B4)
    val FocusText = Color(0xFF00C3E3)
    val FocusCyan = Color(0xFF00C3E3)
    val FocusGlow = Color(0x9900C3E3)
    val Error = Color(0xFFFF8A80)
    val RowFocused = Color(0xFF3A3A3A)
    val DialogPanel = Color(0xFF3B3B3B)
    val Scrim = Color(0xD9000000)
}

@Composable
internal fun TvSwitchTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = TvSwitchColors.FocusCyan,
            background = TvSwitchColors.Background,
            surface = TvSwitchColors.Background,
            onSurface = TvSwitchColors.PrimaryText,
        ),
        content = content,
    )
}

/** Honours the existing per-user TV overscan setting ([AppSettings.tvSafeAreaPaddingDp]). */
internal fun tvSafeAreaPadding(safeAreaDp: Float): PaddingValues =
    PaddingValues(horizontal = 24.dp + safeAreaDp.dp, vertical = 12.dp + safeAreaDp.dp)

// ---------------------------------------------------------------- Top bar

@Composable
internal fun TvTopBar(user: AuthUser?, onProfile: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 8.dp),
    ) {
        ProfileBubble(user = user, onClick = onProfile)
        Spacer(Modifier.weight(1f))
        Text(
            text = rememberClockText(),
            color = TvSwitchColors.PrimaryText,
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium,
        )
        Spacer(Modifier.width(18.dp))
        val network = rememberNetworkStatus()
        Icon(
            imageVector = network.icon,
            contentDescription = network.label,
            tint = if (network == TvNetworkStatus.Offline) TvSwitchColors.Error else TvSwitchColors.PrimaryText,
            modifier = Modifier.size(26.dp),
        )
    }
}

@Composable
private fun ProfileBubble(user: AuthUser?, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = ClickableSurfaceDefaults.shape(shape = CircleShape),
        colors = ClickableSurfaceDefaults.colors(
            containerColor = TvSwitchColors.NavButton,
            focusedContainerColor = TvSwitchColors.NavButtonFocused,
        ),
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1.1f),
        border = ClickableSurfaceDefaults.border(
            focusedBorder = Border(BorderStroke(3.dp, TvSwitchColors.FocusCyan), shape = CircleShape),
        ),
        modifier = Modifier.size(52.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            val initial = user?.displayName?.trim()?.firstOrNull()?.uppercaseChar()?.toString() ?: "?"
            Text(initial, color = TvSwitchColors.PrimaryText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            if (!user?.avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = user?.avatarUrl,
                    contentDescription = user?.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                )
            }
        }
    }
}

@Composable
private fun rememberClockText(): String {
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(60_000L - now % 60_000L) // tick on the minute boundary
        }
    }
    return remember(now / 60_000L) { DateFormat.getTimeFormat(context).format(Date(now)) }
}

internal enum class TvNetworkStatus(val icon: ImageVector, val label: String) {
    Wifi(Icons.Rounded.Wifi, "Wi-Fi connected"),
    Ethernet(Icons.Rounded.SettingsEthernet, "Ethernet connected"),
    Other(Icons.Rounded.Language, "Connected"),
    Offline(Icons.Rounded.WifiOff, "Not connected"),
}

@Composable
private fun rememberNetworkStatus(): TvNetworkStatus {
    val context = LocalContext.current.applicationContext
    var status by remember { mutableStateOf(readNetworkStatus(context)) }
    DisposableEffect(context) {
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        val mainHandler = Handler(Looper.getMainLooper())
        val refresh = Runnable { status = readNetworkStatus(context) }
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { mainHandler.post(refresh) }
            override fun onLost(network: Network) { mainHandler.post(refresh) }
            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                mainHandler.post(refresh)
            }
        }
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        val registered = connectivity != null &&
            runCatching { connectivity.registerNetworkCallback(request, callback) }.isSuccess
        onDispose {
            mainHandler.removeCallbacks(refresh)
            if (registered) runCatching { connectivity?.unregisterNetworkCallback(callback) }
        }
    }
    return status
}

private fun readNetworkStatus(context: Context): TvNetworkStatus {
    val connectivity = context.getSystemService(ConnectivityManager::class.java) ?: return TvNetworkStatus.Offline
    val capabilities = runCatching { connectivity.getNetworkCapabilities(connectivity.activeNetwork) }.getOrNull()
        ?: return TvNetworkStatus.Offline
    return when {
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> TvNetworkStatus.Ethernet
        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> TvNetworkStatus.Wifi
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) -> TvNetworkStatus.Other
        else -> TvNetworkStatus.Offline
    }
}

// ---------------------------------------------------------------- Bottom navigation

@Composable
internal fun TvBottomNav(
    allGamesFocus: FocusRequester,
    onAllGames: () -> Unit,
    onControllers: () -> Unit,
    onSettings: () -> Unit,
    onPowerOff: () -> Unit,
) {
    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp)
                .height(1.dp)
                .background(TvSwitchColors.Divider),
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
            // Bottom padding lifts the buttons off the screen edge and leaves room for the
            // label that appears under the highlighted button.
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 18.dp),
        ) {
            NavButton(Icons.Rounded.Apps, "All Games", onAllGames, Modifier.focusRequester(allGamesFocus))
            NavButton(Icons.Rounded.SportsEsports, "Controllers", onControllers)
            NavButton(Icons.Rounded.Settings, "Settings", onSettings)
            NavButton(Icons.Rounded.PowerSettingsNew, "Quit", onPowerOff)
        }
    }
}

@Composable
private fun NavButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val labelAlpha by animateFloatAsState(if (focused) 1f else 0f, label = "navLabelAlpha")
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(96.dp)) {
        Surface(
            onClick = onClick,
            shape = ClickableSurfaceDefaults.shape(shape = CircleShape),
            colors = ClickableSurfaceDefaults.colors(
                containerColor = TvSwitchColors.NavButton,
                contentColor = TvSwitchColors.PrimaryText,
                focusedContainerColor = TvSwitchColors.NavButtonFocused,
                focusedContentColor = TvSwitchColors.FocusCyan,
            ),
            scale = ClickableSurfaceDefaults.scale(focusedScale = 1.12f),
            border = ClickableSurfaceDefaults.border(
                focusedBorder = Border(BorderStroke(3.dp, TvSwitchColors.FocusCyan), shape = CircleShape),
            ),
            glow = ClickableSurfaceDefaults.glow(
                focusedGlow = Glow(elevationColor = TvSwitchColors.FocusGlow, elevation = 10.dp),
            ),
            modifier = modifier
                .size(64.dp)
                .onFocusChanged { focused = it.isFocused },
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(icon, contentDescription = label, modifier = Modifier.size(30.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = label,
            color = TvSwitchColors.FocusText,
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier.alpha(labelAlpha),
        )
    }
}
