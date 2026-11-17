package com.maxrave.simpmusic.ui.screen.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import com.maxrave.simpmusic.expect.ui.IdleTimer
import com.maxrave.simpmusic.expect.ui.OrientationLock
import com.maxrave.simpmusic.expect.ui.keyWindowScene
import platform.UIKit.UIInterfaceOrientationMaskLandscape
import platform.UIKit.UIInterfaceOrientationMaskPortrait
import platform.UIKit.UIInterfaceOrientationPortrait
import platform.UIKit.UIInterfaceOrientationPortraitUpsideDown

/**
 * As on Android: landscape for as long as the fullscreen player is open, then back to the
 * orientation it was opened from; the screen stays awake meanwhile.
 *
 * The lock itself is the Swift AppDelegate's `supportedInterfaceOrientationsFor`, which reads
 * [OrientationLock]. iOS hides the status bar by itself in landscape on iPhone; hiding the home
 * indicator would need the root view controller's `prefersHomeIndicatorAutoHidden`, which the
 * Compose root does not expose, so it stays.
 */
@Composable
actual fun FullScreenRotationImmersive(
    onLaunch: () -> Unit,
    onDispose: () -> Unit,
) {
    DisposableEffect(true) {
        onLaunch()
        val openedInPortrait =
            when (keyWindowScene()?.effectiveGeometry?.interfaceOrientation) {
                UIInterfaceOrientationPortrait, UIInterfaceOrientationPortraitUpsideDown -> true
                else -> false
            }
        OrientationLock.set(UIInterfaceOrientationMaskLandscape)
        IdleTimer.acquire()
        onDispose {
            IdleTimer.release()
            OrientationLock.set(
                OrientationLock.DEFAULT_MASK,
                rotateTo = if (openedInPortrait) UIInterfaceOrientationMaskPortrait else null,
            )
            onDispose()
        }
    }
}
