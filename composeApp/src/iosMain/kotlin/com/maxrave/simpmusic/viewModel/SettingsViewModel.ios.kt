package com.maxrave.simpmusic.viewModel

import com.eygraber.uri.Uri
import com.maxrave.domain.repository.CacheRepository
import com.maxrave.domain.repository.CommonRepository
import com.maxrave.logger.Logger
import com.maxrave.simpmusic.expect.iosDocumentsPath
import platform.Foundation.NSBundle

private const val TAG = "SettingsViewModel"

actual suspend fun calculateDataFraction(cacheRepository: CacheRepository): SettingsStorageSectionFraction? = null

// ponytail: backup and restore need the document picker, which iOS does not have yet. The picker
// never returns a uri, so these are unreachable today; they log instead of touching any file.
actual suspend fun restoreNative(
    commonRepository: CommonRepository,
    uri: Uri,
    getData: () -> Unit,
) {
    Logger.w(TAG, "restoreNative is not available on iOS yet")
}

actual suspend fun readPickedFile(uri: Uri): ByteArray {
    Logger.w(TAG, "readPickedFile is not available on iOS yet")
    return ByteArray(0)
}

actual suspend fun backupNative(
    commonRepository: CommonRepository,
    uri: Uri,
    backupDownloaded: Boolean,
) {
    Logger.w(TAG, "backupNative is not available on iOS yet")
}

actual fun getPackageName(): String = NSBundle.mainBundle.bundleIdentifier ?: ""

actual fun getFileDir(): String = iosDocumentsPath()

// ponytail: iOS picks the app language from the system settings; in-app switching comes later.
actual fun changeLanguageNative(code: String) {
}
