package com.maxrave.simpmusic.expect

// ponytail: reports every network as unmetered; read NWPathMonitor's isExpensive when Smart
// downloads matter on iOS.
actual fun isOnUnmeteredNetwork(): Boolean = true
