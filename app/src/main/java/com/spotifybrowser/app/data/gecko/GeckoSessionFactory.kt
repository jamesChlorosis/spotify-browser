package com.spotifybrowser.app.data.gecko

import android.content.Context
import android.net.Uri
import com.spotifybrowser.app.data.preferences.BrowserSettings
import com.spotifybrowser.app.data.profile.BrowserProfile
import com.spotifybrowser.app.data.web.BrowserChromeState
import com.spotifybrowser.app.data.web.BrowserError
import com.spotifybrowser.app.data.web.ExternalLinkPolicy
import com.spotifybrowser.app.data.web.SpotifyUrls
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoRuntime
import org.mozilla.geckoview.GeckoSession
import org.mozilla.geckoview.GeckoSessionSettings
import org.mozilla.geckoview.GeckoView
import org.mozilla.geckoview.WebRequestError
import org.mozilla.geckoview.WebResponse

class GeckoSessionFactory(
    private val host: GeckoBrowserHost,
    private val downloadHandler: GeckoDownloadHandler
) {
    fun create(
        context: Context,
        runtime: GeckoRuntime,
        profile: BrowserProfile,
        settings: BrowserSettings,
        onStateChanged: (BrowserChromeState) -> Unit
    ): GeckoBrowserHandle {
        val session = GeckoSession(sessionSettings(profile, settings))
        val statePublisher = BrowserStatePublisher(onStateChanged)

        session.navigationDelegate = NavigationDelegate(host, statePublisher)
        session.progressDelegate = ProgressDelegate(statePublisher)
        session.contentDelegate = ContentDelegate(host, downloadHandler, statePublisher)
        session.promptDelegate = PromptDelegate(host)
        session.open(runtime)
        session.loadUri(SpotifyUrls.HOME)

        val view = GeckoView(context).apply {
            setSession(session)
        }

        return GeckoBrowserHandle(view, session, runtime)
    }

    private fun sessionSettings(
        profile: BrowserProfile,
        settings: BrowserSettings
    ): GeckoSessionSettings {
        return GeckoSessionSettings.Builder()
            .contextId(profile.storageContextId)
            .displayMode(GeckoSessionSettings.DISPLAY_MODE_STANDALONE)
            .userAgentMode(userAgentMode(settings.useDesktopUserAgent))
            .viewportMode(viewportMode(settings.useDesktopUserAgent))
            .allowJavascript(settings.javaScriptEnabled)
            .suspendMediaWhenInactive(!settings.autoplayEnabled)
            .build()
    }

    private class BrowserStatePublisher(
        private val onStateChanged: (BrowserChromeState) -> Unit
    ) {
        private var state = BrowserChromeState()

        fun update(change: BrowserChromeState.() -> BrowserChromeState) {
            state = state.change()
            onStateChanged(state)
        }
    }

    private class NavigationDelegate(
        private val host: GeckoBrowserHost,
        private val publisher: BrowserStatePublisher
    ) : GeckoSession.NavigationDelegate {
        override fun onLoadRequest(
            session: GeckoSession,
            request: GeckoSession.NavigationDelegate.LoadRequest
        ): GeckoResult<org.mozilla.geckoview.AllowOrDeny>? {
            val uri = Uri.parse(request.uri)
            if (ExternalLinkPolicy.shouldOpenExternally(uri)) {
                host.openExternalUri(uri)
                return GeckoResult.deny()
            }
            return null
        }

        override fun onNewSession(
            session: GeckoSession,
            uri: String
        ): GeckoResult<GeckoSession>? {
            host.openExternalUri(Uri.parse(uri))
            return null
        }

        override fun onLocationChange(
            session: GeckoSession,
            url: String?,
            perms: List<GeckoSession.PermissionDelegate.ContentPermission>,
            hasUserGesture: Boolean
        ) {
            publisher.update {
                copy(
                    currentUrl = url ?: currentUrl,
                    error = null
                )
            }
        }

        override fun onCanGoBack(session: GeckoSession, canGoBack: Boolean) {
            publisher.update { copy(canGoBack = canGoBack) }
        }

        override fun onCanGoForward(session: GeckoSession, canGoForward: Boolean) {
            publisher.update { copy(canGoForward = canGoForward) }
        }

        override fun onLoadError(
            session: GeckoSession,
            uri: String?,
            error: WebRequestError
        ): GeckoResult<String>? {
            publisher.update {
                copy(
                    isLoading = false,
                    error = BrowserError(
                        description = "Network error ${error.code}",
                        failingUrl = uri
                    )
                )
            }
            return null
        }
    }

    private class ProgressDelegate(
        private val publisher: BrowserStatePublisher
    ) : GeckoSession.ProgressDelegate {
        override fun onPageStart(session: GeckoSession, url: String) {
            publisher.update {
                copy(
                    currentUrl = url,
                    isLoading = true,
                    progress = 0,
                    error = null
                )
            }
        }

        override fun onPageStop(session: GeckoSession, success: Boolean) {
            publisher.update {
                copy(
                    isLoading = false,
                    progress = 100
                )
            }
        }

        override fun onProgressChange(session: GeckoSession, progress: Int) {
            publisher.update { copy(progress = progress.coerceIn(0, 100)) }
        }
    }

    private class ContentDelegate(
        private val host: GeckoBrowserHost,
        private val downloadHandler: GeckoDownloadHandler,
        private val publisher: BrowserStatePublisher
    ) : GeckoSession.ContentDelegate {
        override fun onTitleChange(session: GeckoSession, title: String?) {
            publisher.update { copy(title = title.orEmpty()) }
        }

        override fun onFullScreen(session: GeckoSession, fullScreen: Boolean) {
            host.setPageFullscreen(fullScreen)
        }

        override fun onExternalResponse(session: GeckoSession, response: WebResponse) {
            if (response.requestExternalApp) {
                host.openExternalUri(Uri.parse(response.uri))
            } else {
                downloadHandler.enqueue(response)
            }
        }

        override fun onCrash(session: GeckoSession) {
            publisher.update {
                copy(
                    isLoading = false,
                    error = BrowserError(
                        description = "The browser engine crashed. Reload Spotify to recover.",
                        failingUrl = currentUrl
                    )
                )
            }
        }

        override fun onKill(session: GeckoSession) {
            publisher.update {
                copy(
                    isLoading = false,
                    error = BrowserError(
                        description = "The browser engine was stopped by the system. Reload Spotify to recover.",
                        failingUrl = currentUrl
                    )
                )
            }
        }
    }

    private class PromptDelegate(
        private val host: GeckoBrowserHost
    ) : GeckoSession.PromptDelegate {
        override fun onFilePrompt(
            session: GeckoSession,
            prompt: GeckoSession.PromptDelegate.FilePrompt
        ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse> {
            return host.onFilePrompt(prompt)
        }
    }
}

fun GeckoSession.applyBrowserSettings(settings: BrowserSettings) {
    getSettings().setAllowJavascript(settings.javaScriptEnabled)
    getSettings().setSuspendMediaWhenInactive(!settings.autoplayEnabled)
    getSettings().setUserAgentMode(userAgentMode(settings.useDesktopUserAgent))
    getSettings().setViewportMode(viewportMode(settings.useDesktopUserAgent))
}

private fun userAgentMode(desktop: Boolean): Int {
    return if (desktop) {
        GeckoSessionSettings.USER_AGENT_MODE_DESKTOP
    } else {
        GeckoSessionSettings.USER_AGENT_MODE_MOBILE
    }
}

private fun viewportMode(desktop: Boolean): Int {
    return if (desktop) {
        GeckoSessionSettings.VIEWPORT_MODE_DESKTOP
    } else {
        GeckoSessionSettings.VIEWPORT_MODE_MOBILE
    }
}
