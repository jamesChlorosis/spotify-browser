package com.spotifybrowser.app

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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
import com.spotifybrowser.app.data.gecko.ExtensionInstallHost
import com.spotifybrowser.app.data.gecko.GeckoBrowserHost
import com.spotifybrowser.app.data.gecko.GeckoExtensionManager
import com.spotifybrowser.app.data.gecko.GeckoRuntimeProvider
import com.spotifybrowser.app.data.preferences.PreferencesRepository
import com.spotifybrowser.app.data.preferences.BrowserSettings
import com.spotifybrowser.app.data.profile.ProfileManager
import com.spotifybrowser.app.ui.screens.SpotifyBrowserApp
import com.spotifybrowser.app.ui.theme.SpotifyBrowserTheme
import com.spotifybrowser.app.viewmodel.MainViewModel
import com.spotifybrowser.app.viewmodel.MainViewModelFactory
import org.mozilla.geckoview.GeckoResult
import org.mozilla.geckoview.GeckoSession

class MainActivity : ComponentActivity(), GeckoBrowserHost, ExtensionInstallHost {
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

    private var pendingFilePrompt: GeckoSession.PromptDelegate.FilePrompt? = null
    private var pendingFileResult: GeckoResult<GeckoSession.PromptDelegate.PromptResponse>? = null

    private val singleFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        completeFilePrompt(if (uri == null) emptyArray() else arrayOf(uri))
    }

    private val multipleFilesLauncher = registerForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        completeFilePrompt(uris.toTypedArray())
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
                    extensionInstallHost = this,
                    onOpenProfile = viewModel::openProfile,
                    onCreateProfile = viewModel::createProfile,
                    onRenameProfile = viewModel::renameProfile,
                    onDeleteProfile = viewModel::deleteProfile,
                    onBrowserChromeChanged = viewModel::updateBrowserChrome,
                    onExtensionSetupFinished = viewModel::finishExtensionSetup,
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
        dismissPendingFilePrompt()
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

    override fun setPageFullscreen(enabled: Boolean) {
        hideSystemBars()
    }

    override fun onFilePrompt(
        prompt: GeckoSession.PromptDelegate.FilePrompt
    ): GeckoResult<GeckoSession.PromptDelegate.PromptResponse> {
        dismissPendingFilePrompt()
        val result = GeckoResult<GeckoSession.PromptDelegate.PromptResponse>()
        pendingFilePrompt = prompt
        pendingFileResult = result

        if (prompt.type == GeckoSession.PromptDelegate.FilePrompt.Type.FOLDER) {
            Toast.makeText(this, "Folder upload is not supported", Toast.LENGTH_SHORT).show()
            completeFilePrompt(emptyArray())
            return result
        }

        val mimeTypes = prompt.mimeTypes
            ?.takeIf { it.isNotEmpty() }
            ?: arrayOf("*/*")

        runCatching {
            if (prompt.type == GeckoSession.PromptDelegate.FilePrompt.Type.MULTIPLE) {
                multipleFilesLauncher.launch(mimeTypes)
            } else {
                singleFileLauncher.launch(mimeTypes)
            }
        }.onFailure {
            Toast.makeText(this, "No file picker is available", Toast.LENGTH_LONG).show()
            completeFilePrompt(emptyArray())
        }

        return result
    }

    override fun installExtensionFromUrl(
        url: String,
        onResult: (Result<String>) -> Unit
    ) {
        val runtime = GeckoRuntimeProvider.get(
            context = applicationContext,
            settings = BrowserSettings()
        )

        runCatching {
            GeckoExtensionManager.installSignedXpi(runtime, url).accept(
                { extension ->
                    val displayName = extension?.metaData?.name
                        ?: extension?.id
                        ?: "Extension installed"
                    onResult(Result.success(displayName))
                },
                { throwable ->
                    onResult(Result.failure(throwable ?: IllegalStateException("Extension installation failed")))
                }
            )
        }.onFailure {
            onResult(Result.failure(it))
        }
    }

    private fun completeFilePrompt(uris: Array<Uri>) {
        val prompt = pendingFilePrompt ?: return
        val result = pendingFileResult ?: return
        val response = if (uris.isEmpty()) {
            prompt.dismiss()
        } else if (prompt.type == GeckoSession.PromptDelegate.FilePrompt.Type.MULTIPLE) {
            prompt.confirm(applicationContext, uris)
        } else {
            prompt.confirm(applicationContext, uris.first())
        }

        pendingFilePrompt = null
        pendingFileResult = null
        result.complete(response)
    }

    private fun dismissPendingFilePrompt() {
        val prompt = pendingFilePrompt ?: return
        val result = pendingFileResult ?: return
        pendingFilePrompt = null
        pendingFileResult = null
        result.complete(prompt.dismiss())
    }

    private fun hideSystemBars() {
        WindowInsetsControllerCompat(window, window.decorView).apply {
            systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}
