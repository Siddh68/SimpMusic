package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable

// ponytail: null hides the Apple Music style's volume row; MPVolumeView can drive it later.
@Composable
actual fun rememberDeviceVolumeController(): DeviceVolumeController? = null
