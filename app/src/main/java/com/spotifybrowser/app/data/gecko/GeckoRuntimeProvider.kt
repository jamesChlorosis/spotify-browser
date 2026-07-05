package com.spotifybrowser.app.data.gecko

import android.content.Context
import com.spotifybrowser.app.BuildConfig
import com.spotifybrowser.app.data.preferences.BrowserSettings
import com.spotifybrowser.app.data.preferences.ThemeMode
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoRuntimeSettings

object GeckoRuntimeProvider {
    private var runtime: GeckoRuntime? = null

    fun get(
        context: Context,
        settings: BrowserSettings
    ): GeckoRuntime {
        val existing = runtime
        if (existing != null) {
            applyRuntimeSettings(existing, settings)
            return existing
        }

        val runtimeSettings = GeckoRuntimeSettings.Builder()
            .javaScriptEnabled(settings.javaScriptEnabled)
            .extensionsProcessEnabled(true)
            .extensionsWebAPIEnabled(false)
            .remoteDebuggingEnabled(BuildConfig.DEBUG)
            .preferredColorScheme(colorScheme(settings.themeMode))
            .fontSizeFactor(settings.defaultZoomPercent.toFontScale())
            .webManifest(true)
            .build()

        return GeckoRuntime.create(context.applicationContext, runtimeSettings).also { created ->
            runtime = created
            GeckoExtensionManager.ensureBuiltIns(created)
        }
    }

    fun applyRuntimeSettings(
        runtime: GeckoRuntime,
        settings: BrowserSettings
    ) {
        runtime.settings
            .setJavaScriptEnabled(settings.javaScriptEnabled)
            .setPreferredColorScheme(colorScheme(settings.themeMode))
            .setFontSizeFactor(settings.defaultZoomPercent.toFontScale())
    }

    private fun colorScheme(themeMode: ThemeMode): Int {
        return when (themeMode) {
            ThemeMode.System -> GeckoRuntimeSettings.COLOR_SCHEME_SYSTEM
            ThemeMode.Light -> GeckoRuntimeSettings.COLOR_SCHEME_LIGHT
            ThemeMode.Dark -> GeckoRuntimeSettings.COLOR_SCHEME_DARK
        }
    }

    private fun Int.toFontScale(): Float = coerceIn(75, 150) / 100f
}
