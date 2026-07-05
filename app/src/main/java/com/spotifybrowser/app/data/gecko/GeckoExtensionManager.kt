package com.spotifybrowser.app.data.gecko

import android.net.Uri
import org.mozilla.geckoview.AllowOrDeny
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.WebExtension
import org.mozilla.geckoview.WebExtensionController

object GeckoExtensionManager {
    private const val COMPANION_ID = "spotify-browser-companion@example.com"
    private const val COMPANION_URI = "resource://android/assets/extensions/spotify-browser-companion/"

    fun ensureBuiltIns(runtime: GeckoRuntime) {
        runtime.webExtensionController
            .ensureBuiltIn(COMPANION_URI, COMPANION_ID)
            .accept({ }, { })
    }

    fun installSignedXpi(
        runtime: GeckoRuntime,
        url: String
    ): GeckoResult<WebExtension> {
        val cleanUrl = url.trim()
        val parsed = Uri.parse(cleanUrl)
        require(parsed.scheme.equals("https", ignoreCase = true)) {
            "Only HTTPS .xpi extension URLs are supported."
        }

        runtime.webExtensionController.setPromptDelegate(SafeExtensionPromptDelegate)
        return runtime.webExtensionController.install(
            cleanUrl,
            WebExtensionController.INSTALLATION_METHOD_ONBOARDING
        )
    }

    private object SafeExtensionPromptDelegate : WebExtensionController.PromptDelegate {
        override fun onInstallPromptRequest(
            extension: WebExtension,
            permissions: Array<String>,
            origins: Array<String>,
            dataCollectionPermissions: Array<String>
        ): GeckoResult<WebExtension.PermissionPromptResponse> {
            val allowed = ExtensionPermissionPolicy.isAllowed(permissions, origins)
            return GeckoResult.fromValue(
                WebExtension.PermissionPromptResponse(
                    allowed,
                    false,
                    false
                )
            )
        }

        override fun onOptionalPrompt(
            extension: WebExtension,
            permissions: Array<String>,
            origins: Array<String>,
            dataCollectionPermissions: Array<String>
        ): GeckoResult<AllowOrDeny> {
            return if (ExtensionPermissionPolicy.isAllowed(permissions, origins)) {
                GeckoResult.allow()
            } else {
                GeckoResult.deny()
            }
        }

        override fun onUpdatePrompt(
            extension: WebExtension,
            newPermissions: Array<String>,
            newOrigins: Array<String>,
            newDataCollectionPermissions: Array<String>
        ): GeckoResult<AllowOrDeny> {
            return if (ExtensionPermissionPolicy.isAllowed(newPermissions, newOrigins)) {
                GeckoResult.allow()
            } else {
                GeckoResult.deny()
            }
        }
    }
}
