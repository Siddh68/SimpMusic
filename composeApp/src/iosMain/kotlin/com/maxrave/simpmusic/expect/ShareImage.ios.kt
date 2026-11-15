package com.maxrave.simpmusic.expect

// Photo library and share sheet are not wired up on iOS yet. False lets the caller tell the user.
actual suspend fun saveImageToDevice(
    bytes: ByteArray,
    fileName: String,
): Boolean = false

actual suspend fun shareImage(
    bytes: ByteArray,
    fileName: String,
    chooserTitle: String,
    mimeType: String,
): Boolean = false
