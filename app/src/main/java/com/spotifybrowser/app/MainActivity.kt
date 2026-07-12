package com.spotifybrowser.app

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.browser.customtabs.CustomTabsIntent
import com.spotifybrowser.app.data.preferences.PreferencesRepository
import com.spotifybrowser.app.data.profile.ProfileManager
import com.spotifybrowser.app.data.webview.WebViewBrowserHost
import com.spotifybrowser.app.ui.screens.SpotifyBrowserApp
import com.spotifybrowser.app.ui.theme.SpotifyBrowserTheme
import com.spotifybrowser.app.viewmodel.MainViewModel
import com.spotifybrowser.app.viewmodel.MainViewModelFactory

class MainActivity : ComponentActivity(), WebViewBrowserHost {
    private val spotifyBrowserPackages = listOf(
        "com.android.chrome",
        "com.chrome.beta",
        "com.chrome.dev",
        "com.sec.android.app.sbrowser"
    )

    private val extensionBrowserPackages = listOf(
        "com.microsoft.emmx",
        "com.microsoft.emmx.beta",
        "com.microsoft.emmx.dev",
        "com.microsoft.emmx.canary",
        "org.mozilla.firefox",
        "org.mozilla.firefox_beta"
    )

    private val profileManager by lazy { ProfileManager(applicationContext) }
    private val preferencesRepository by lazy { PreferencesRepository(applicationContext) }

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(
            application = application,
            profileManager = profileManager,
            preferencesRepository = preferencesRepository,
            initialProfileId = null
        )
    }

    private var pendingWebViewFileCallback: ValueCallback<Array<Uri>>? = null

    private val singleFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        completeSelectedFiles(if (uri == null) emptyArray() else arrayOf(uri))
    }

    private val multipleFilesLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        completeSelectedFiles(uris.toTypedArray())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        WindowCompat.setDecorFitsSystemWindows(window, false)
        hideSystemBars()

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            SpotifyBrowserTheme(themeMode = uiState.settings.themeMode) {
                SpotifyBrowserApp(
                    uiState = uiState,
                    browserHost = this,
                    onOpenProfile = viewModel::openProfile,
                    onCreateProfile = viewModel::createProfile,
                    onRenameProfile = viewModel::renameProfile,
                    onDeleteProfile = viewModel::deleteProfile,
                    onBrowserChromeChanged = viewModel::updateBrowserChrome,
                    onDesktopUserAgentChanged = viewModel::setDesktopUserAgent,
                    onZoomChanged = viewModel::setDefaultZoomPercent,
                    onJavaScriptChanged = viewModel::setJavaScriptEnabled,
                    onAutoplayChanged = viewModel::setAutoplayEnabled,
                    onThemeChanged = viewModel::setThemeMode
                )
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    override fun onDestroy() {
        dismissPendingWebViewFilePrompt()
        super.onDestroy()
    }

    override fun openExternalUri(uri: Uri) {
        launchUri(uri, packageName = null, showError = true)
    }

    override fun openSpotifyInCompatibleBrowser(uri: Uri) {
        val launched = spotifyBrowserPackages.any { packageName ->
            launchCustomTab(uri, packageName = packageName)
        }

        if (!launched) {
            Toast.makeText(
                this,
                "Opening Spotify in your browser. Chrome or Samsung Internet is recommended for protected content.",
                Toast.LENGTH_LONG
            ).show()
            launchUri(uri, packageName = null, showError = true)
        }
    }

    override fun openExtensionUrl(url: String) {
        val uri = url.toWebUriOrNull()
        if (uri == null) {
            Toast.makeText(this, "Enter a valid http or https extension URL", Toast.LENGTH_LONG).show()
            return
        }
        val launched = extensionBrowserPackages.any { packageName ->
            launchUri(uri, packageName = packageName, showError = false)
        }

        if (!launched) {
            Toast.makeText(
                this,
                "Install Microsoft Edge or Firefox to add mobile-supported extensions.",
                Toast.LENGTH_LONG
            ).show()
            launchUri(uri, packageName = null, showError = true)
        }
    }

    private fun launchCustomTab(
        uri: Uri,
        packageName: String
    ): Boolean {
        val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setUrlBarHidingEnabled(false)
            .build()
        customTabsIntent.intent.setPackage(packageName)
        customTabsIntent.intent.addCategory(Intent.CATEGORY_BROWSABLE)

        return runCatching {
            customTabsIntent.launchUrl(this, uri)
            true
        }.getOrElse { false }
    }

    private fun launchUri(
        uri: Uri,
        packageName: String?,
        showError: Boolean
    ): Boolean {
        val intent = Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE)
        if (packageName != null) {
            intent.setPackage(packageName)
        }

        return runCatching {
            startActivity(intent)
            true
        }.getOrElse {
            if (showError && it is ActivityNotFoundException) {
                Toast.makeText(
                    this,
                    "No browser can open this link",
                    Toast.LENGTH_LONG
                ).show()
            }
            false
        }
    }

    private fun String.toWebUriOrNull(): Uri? {
        val trimmed = trim()
        if (trimmed.isEmpty()) return null

        val normalized = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }

        return runCatching {
            Uri.parse(normalized).takeIf { uri ->
                val scheme = uri.scheme?.lowercase()
                scheme == "http" || scheme == "https"
            }
        }.getOrNull()
    }

    override fun setPageFullscreen(enabled: Boolean) {
        hideSystemBars()
    }

    override fun onShowFileChooser(
        filePathCallback: ValueCallback<Array<Uri>>,
        fileChooserParams: WebChromeClient.FileChooserParams
    ): Boolean {
        dismissPendingWebViewFilePrompt()
        pendingWebViewFileCallback = filePathCallback

        val mimeTypes = fileChooserParams.acceptTypes
            ?.filter { it.isNotBlank() }
            ?.takeIf { it.isNotEmpty() }
            ?.toTypedArray()
            ?: arrayOf("*/*")

        runCatching {
            if (fileChooserParams.mode == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE) {
                multipleFilesLauncher.launch(mimeTypes)
            } else {
                singleFileLauncher.launch(mimeTypes)
            }
        }.onFailure {
            Toast.makeText(this, "No file picker is available", Toast.LENGTH_LONG).show()
            completeWebViewFilePrompt(emptyArray())
        }

        return true
    }

    private fun completeSelectedFiles(uris: Array<Uri>) {
        completeWebViewFilePrompt(uris)
    }

    private fun completeWebViewFilePrompt(uris: Array<Uri>) {
        val callback = pendingWebViewFileCallback ?: return
        pendingWebViewFileCallback = null
        if (uris.isEmpty()) {
            callback.onReceiveValue(null)
        } else {
            callback.onReceiveValue(uris)
        }
    }

    private fun dismissPendingWebViewFilePrompt() {
        val callback = pendingWebViewFileCallback ?: return
        pendingWebViewFileCallback = null
        callback.onReceiveValue(null)
    }

    private fun hideSystemBars() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}
