package com.spotifybrowser.app

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.widget.FrameLayout
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
import com.spotifybrowser.app.data.preferences.PreferencesRepository
import com.spotifybrowser.app.data.profile.AppRestarter
import com.spotifybrowser.app.data.profile.ProfileManager
import com.spotifybrowser.app.data.web.WebViewHost
import com.spotifybrowser.app.ui.screens.SpotifyBrowserApp
import com.spotifybrowser.app.ui.theme.SpotifyBrowserTheme
import com.spotifybrowser.app.viewmodel.MainViewModel
import com.spotifybrowser.app.viewmodel.MainViewModelFactory

class MainActivity : ComponentActivity(), WebViewHost {
    private val profileManager by lazy { ProfileManager(applicationContext) }
    private val preferencesRepository by lazy { PreferencesRepository(applicationContext) }

    private val viewModel: MainViewModel by viewModels {
        MainViewModelFactory(
            application = application,
            profileManager = profileManager,
            preferencesRepository = preferencesRepository,
            initialProfileId = intent.getStringExtra(AppRestarter.EXTRA_PROFILE_ID)
        )
    }

    private var filePathCallback: ValueCallback<Array<Uri>>? = null
    private var customView: View? = null
    private var customViewCallback: WebChromeClient.CustomViewCallback? = null

    private val fileChooserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val callback = filePathCallback ?: return@registerForActivityResult
        filePathCallback = null
        val uris = WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
        callback.onReceiveValue(uris ?: emptyArray())
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
                    host = this,
                    onOpenProfile = viewModel::openProfile,
                    onCreateProfile = viewModel::createProfile,
                    onRenameProfile = viewModel::renameProfile,
                    onDeleteProfile = viewModel::deleteProfile,
                    onWebViewStarted = viewModel::markWebViewStarted,
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
        filePathCallback?.onReceiveValue(emptyArray())
        filePathCallback = null
        hideFullscreenContent()
        super.onDestroy()
    }

    override fun openExternalUri(uri: Uri) {
        val intent = Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE)
        runCatching { startActivity(intent) }
            .onFailure {
                if (it is ActivityNotFoundException) {
                    Toast.makeText(this, "No app can open this link", Toast.LENGTH_LONG).show()
                }
            }
    }

    override fun openFileChooser(
        callback: ValueCallback<Array<Uri>>,
        params: WebChromeClient.FileChooserParams
    ): Boolean {
        filePathCallback?.onReceiveValue(emptyArray())
        filePathCallback = callback

        return runCatching {
            fileChooserLauncher.launch(params.createIntent())
            true
        }.getOrElse {
            filePathCallback = null
            callback.onReceiveValue(emptyArray())
            Toast.makeText(this, "No file picker is available", Toast.LENGTH_LONG).show()
            false
        }
    }

    override fun showFullscreenContent(
        view: View,
        callback: WebChromeClient.CustomViewCallback
    ) {
        if (customView != null) {
            callback.onCustomViewHidden()
            return
        }

        customView = view
        customViewCallback = callback
        val content = findViewById<FrameLayout>(android.R.id.content)
        content.addView(
            view,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        hideSystemBars()
    }

    override fun hideFullscreenContent() {
        val content = findViewById<FrameLayout>(android.R.id.content)
        customView?.let(content::removeView)
        customView = null
        customViewCallback?.onCustomViewHidden()
        customViewCallback = null
        hideSystemBars()
    }

    private fun hideSystemBars() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}
