package com.maxrave.simpmusic.ui.screen.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect

// ponytail: no forced landscape or hidden status bar on iOS yet; only the callbacks run, as on Android.
@Composable
actual fun FullScreenRotationImmersive(
    onLaunch: () -> Unit,
    onDispose: () -> Unit,
) {
    DisposableEffect(true) {
        onLaunch()
        onDispose { onDispose() }
    }
}
