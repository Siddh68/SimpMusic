package com.maxrave.simpmusic

import androidx.compose.ui.window.ComposeUIViewController
import com.maxrave.domain.mediaservice.player.ios.NativePlayerEngine
import com.maxrave.domain.mediaservice.player.ios.NativePlayerRegistry
import com.maxrave.simpmusic.expect.ui.OrientationLock
import platform.UIKit.UIViewController

/** Entry point for the Swift app (SceneDelegate). Starts Koin and Coil once before the first App(). */
fun MainViewController(): UIViewController {
    startIosApp()
    return ComposeUIViewController { App() }
}

/**
 * Called by the Swift app before [MainViewController]: hands over the AVPlayer-backed engine the
 * iOS player drives. Also what puts the two bridge interfaces into the framework header.
 */
fun registerNativePlayerEngine(engine: NativePlayerEngine) {
    NativePlayerRegistry.engine = engine
}

/**
 * The `UIInterfaceOrientationMask` the app allows right now, for the Swift AppDelegate's
 * `supportedInterfaceOrientationsFor`. Landscape only while the fullscreen player is open.
 */
fun orientationLockMask(): Int = OrientationLock.mask.toInt()
