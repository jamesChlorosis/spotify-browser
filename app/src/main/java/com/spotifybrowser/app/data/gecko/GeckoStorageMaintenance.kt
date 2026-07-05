package com.spotifybrowser.app.data.gecko

import com.spotifybrowser.app.data.profile.BrowserProfile
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.StorageController

object GeckoStorageMaintenance {
    fun clearCache(runtime: GeckoRuntime): GeckoResult<Void> {
        return runtime.storageController.clearData(StorageController.ClearFlags.ALL_CACHES)
    }

    fun clearCookies(runtime: GeckoRuntime): GeckoResult<Void> {
        return runtime.storageController.clearData(StorageController.ClearFlags.COOKIES)
    }

    fun clearProfile(runtime: GeckoRuntime, profile: BrowserProfile) {
        runtime.storageController.clearDataForSessionContext(profile.storageContextId)
    }
}
