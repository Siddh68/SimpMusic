package com.maxrave.simpmusic

import androidx.compose.ui.window.ComposeUIViewController
import platform.UIKit.UIViewController

/** Entry point for the Swift app (SceneDelegate). Starts Koin and Coil once before the first App(). */
fun MainViewController(): UIViewController {
    startIosApp()
    return ComposeUIViewController { App() }
}
