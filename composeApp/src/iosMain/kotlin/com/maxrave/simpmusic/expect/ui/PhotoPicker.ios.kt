package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable

// ponytail: no PHPickerViewController yet, so launching does nothing.
@Composable
actual fun photoPickerResult(onResultUri: (String?) -> Unit): PhotoPickerLauncher = NoOpPhotoPickerLauncher

private object NoOpPhotoPickerLauncher : PhotoPickerLauncher {
    override fun launch() = Unit
}
