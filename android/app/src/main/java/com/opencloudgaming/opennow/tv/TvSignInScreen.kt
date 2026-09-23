package com.opencloudgaming.opennow.tv

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.opencloudgaming.opennow.DeviceLoginPrompt
import com.opencloudgaming.opennow.OpenNowUiState
import com.opencloudgaming.opennow.OpenNowViewModel
import com.opencloudgaming.opennow.QrCode
import com.opencloudgaming.opennow.QrCodeView
import com.opencloudgaming.opennow.R

/**
 * Switch-style sign-in: one "Sign in" choice that starts the existing device-code flow, then the
 * code and a QR shown the way the console shows its own "link an account" screens. Other sign-in
 * methods (browser, token) stay available through the stock login screen.
 */
@Composable
internal fun TvSignInScreen(
    state: OpenNowUiState,
    viewModel: OpenNowViewModel,
    onOtherOptions: () -> Unit,
) {
    val prompt = state.deviceLoginPrompt
    val requesting = prompt == null && state.launchPhase.isNotBlank()
    BackHandler(enabled = prompt != null || requesting, onBack = viewModel::cancelLogin)

    TvSwitchScreenFrame(
        title = if (prompt != null) "Link your NVIDIA account" else stringResource(R.string.app_name),
        icon = Icons.Rounded.AccountCircle,
        safeAreaDp = state.settings.tvSafeAreaPaddingDp,
        hints = if (prompt != null || requesting) {
            listOf(TvButtonHint(TvButtonGlyph.B, "Cancel"))
        } else {
            listOf(TvButtonHint(TvButtonGlyph.A, "OK"))
        },
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize().padding(24.dp)) {
            when {
                state.initializing -> Waiting("Restoring your session…")
                prompt != null -> CodePanel(prompt = prompt, error = state.error, onCancel = viewModel::cancelLogin)
                requesting -> Waiting(state.launchPhase)
                else -> StartPanel(
                    error = state.error,
                    onSignIn = { viewModel.loginWithCode() },
                    onOtherOptions = onOtherOptions,
                )
            }
        }
    }
}

@Composable
private fun StartPanel(error: String?, onSignIn: () -> Unit, onOtherOptions: () -> Unit) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        runCatching { first.requestFocus() }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.widthIn(max = 560.dp)) {
        Image(
            painter = painterResource(R.drawable.goudanow_logo_mark),
            contentDescription = null,
            modifier = Modifier.width(220.dp).height(119.dp),
        )
        Spacer(Modifier.height(18.dp))
        Text(
            text = "Sign in with your NVIDIA account to see your games.",
            color = TvSwitchColors.PrimaryText,
            fontSize = 22.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "You'll get a code to enter on your phone or computer.",
            color = TvSwitchColors.SecondaryText,
            fontSize = 18.sp,
            textAlign = TextAlign.Center,
        )
        error?.takeIf { it.isNotBlank() }?.let {
            Spacer(Modifier.height(12.dp))
            Text(it, color = TvSwitchColors.Error, fontSize = 16.sp, textAlign = TextAlign.Center)
        }
        Spacer(Modifier.height(28.dp))
        TvListRow(label = "Sign in", onClick = onSignIn, modifier = Modifier.focusRequester(first))
        TvListRow(label = "Other sign-in options", labelColor = TvSwitchColors.SecondaryText, onClick = onOtherOptions)
    }
}

@Composable
private fun CodePanel(prompt: DeviceLoginPrompt, error: String?, onCancel: () -> Unit) {
    val qrTarget = prompt.verificationUriComplete ?: prompt.verificationUri
    val qrCode = remember(qrTarget) { QrCode.encodeText(qrTarget) ?: QrCode.encodeText(prompt.verificationUri) }
    val cancelFocus = remember { FocusRequester() }
    LaunchedEffect(prompt.userCode) {
        withFrameNanos { }
        runCatching { cancelFocus.requestFocus() }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(56.dp)) {
        Column(modifier = Modifier.widthIn(max = 520.dp)) {
            Step("1", "On your phone or computer, go to:")
            Text(
                text = prompt.verificationUri.removePrefix("https://").removePrefix("http://"),
                color = TvSwitchColors.FocusCyan,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(start = 44.dp, top = 4.dp, bottom = 22.dp),
            )
            Step("2", "Enter this code:")
            Text(
                text = prompt.userCode,
                color = TvSwitchColors.PrimaryText,
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 6.sp,
                modifier = Modifier
                    .padding(start = 44.dp, top = 10.dp)
                    .background(TvSwitchColors.RowFocused, RoundedCornerShape(6.dp))
                    .padding(horizontal = 24.dp, vertical = 10.dp),
            )
            Spacer(Modifier.height(22.dp))
            Text(
                text = "This screen updates by itself once you've signed in.",
                color = TvSwitchColors.SecondaryText,
                fontSize = 16.sp,
                modifier = Modifier.padding(start = 44.dp),
            )
            error?.takeIf { it.isNotBlank() }?.let {
                Text(it, color = TvSwitchColors.Error, fontSize = 16.sp, modifier = Modifier.padding(start = 44.dp, top = 8.dp))
            }
            Spacer(Modifier.height(24.dp))
            TvListRow(label = "Cancel", onClick = onCancel, modifier = Modifier.width(240.dp).focusRequester(cancelFocus))
        }
        if (qrCode != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                QrCodeView(qrCode, Modifier.size(220.dp))
                Spacer(Modifier.height(10.dp))
                Text("Or scan with your phone", color = TvSwitchColors.SecondaryText, fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun Step(number: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(30.dp).background(TvSwitchColors.FocusCyan, RoundedCornerShape(50)),
        ) {
            Text(number, color = TvSwitchColors.Background, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(14.dp))
        Text(text, color = TvSwitchColors.PrimaryText, fontSize = 20.sp)
    }
}

@Composable
internal fun Waiting(message: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator(color = TvSwitchColors.FocusCyan)
        Spacer(Modifier.height(20.dp))
        Text(message, color = TvSwitchColors.SecondaryText, fontSize = 19.sp, textAlign = TextAlign.Center)
    }
}
