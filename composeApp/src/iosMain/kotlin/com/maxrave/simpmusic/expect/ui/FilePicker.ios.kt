package com.maxrave.simpmusic.expect.ui

import androidx.compose.runtime.Composable

// ponytail: no UIDocumentPickerViewController yet, so launching does nothing and no result arrives.
@Composable
actual fun filePickerResult(onResultUri: (String?) -> Unit): FilePickerLauncher = NoOpFilePickerLauncher

@Composable
actual fun fileSaverResult(
    fileName: String,
    mimeType: String,
    onResultUri: (String?) -> Unit,
): FilePickerLauncher = NoOpFilePickerLauncher

private object NoOpFilePickerLauncher : FilePickerLauncher {
    override fun launch() = Unit
}
