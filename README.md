# Spotify Browser

Spotify Browser is an unofficial dedicated Android launcher for the Spotify Web Player at `https://open.spotify.com/`. It starts with local profiles like the original app, then opens Spotify in a compatible installed Android browser for playback so protected content/Widevine checks are handled by Chrome or Samsung Internet instead of the embedded fallback view.

The app stays inside Spotify's platform boundaries: it does not inject scripts into Spotify pages, modify Spotify network traffic, bypass advertisements, bypass DRM, bypass authentication, or alter subscription behavior.

## Features

- Kotlin, AndroidX, Jetpack Compose, Material 3
- Android 10+ (`minSdk 29`)
- Full-screen immersive standalone app experience
- Profile picker on launch for the old multi-profile flow
- Faster startup by avoiding browser engine startup until a profile is selected
- Spotify playback handoff to Chrome/Samsung Internet for protected content
- In-app WebView fallback for browsing/settings flows
- Spotify sign-in button that opens Spotify Accounts in the compatible browser
- Chrome Web Store, custom extension URL, and Kiwi Browser shortcuts for downloading browser extensions
- Persistent login, cookies, DOM storage, local storage, and cache
- Back, forward, refresh, file upload, and media playback
- External non-Spotify links open in the user's default browser
- Settings for desktop/mobile user agent, zoom, JavaScript, autoplay behavior, theme, cache, cookies, and profile data
- DataStore-backed preferences and MVVM structure

## Project Structure

```text
app/src/main/java/com/spotifybrowser/app/
  MainActivity.kt
  data/preferences/        DataStore settings
  data/profile/            profile metadata
  data/web/                shared URL/link policy and browser state models
  data/webview/            WebView fallback, settings, and browser state
  ui/components/           reusable Compose components
  ui/screens/              profile selector, browser, settings
  ui/theme/                Material 3 theme
  viewmodel/               MVVM state and actions
```

## Build in Android Studio

1. Open this folder in Android Studio.
2. Let Android Studio install/sync Java 17, Android SDK 35, the Android Gradle Plugin, Kotlin, and Compose dependencies.
3. Select a device or emulator running Android 10 or newer.
4. Click **Run** to install the debug build.

## Generate a Release Signing Key

Create a private keystore outside version control:

```powershell
New-Item -ItemType Directory -Force keystores
keytool -genkeypair `
  -v `
  -keystore keystores/spotify-browser-release.jks `
  -storetype JKS `
  -keyalg RSA `
  -keysize 4096 `
  -validity 10000 `
  -alias spotify-browser
```

Copy `signing.properties.example` to `signing.properties` and fill in the passwords you chose. Both `signing.properties` and keystores are ignored by git.

## Build a Signed Release APK

From Android Studio:

1. Open **Build > Generate Signed Bundle / APK**.
2. Choose **APK**.
3. Select your keystore and alias.
4. Choose the `release` build variant.
5. Finish the wizard.

From the terminal after `signing.properties` exists:

```powershell
.\gradlew.bat assembleRelease
```

The signed APK will be written to:

```text
app/build/outputs/apk/release/app-release.apk
```

## Compliance Notes

This project intentionally avoids hidden extensions, page injection, ad blocking, traffic interception, authentication bypassing, subscription bypassing, and DRM bypassing. Spotify playback is handed to installed supported browsers instead of trying to bypass protected-content checks in an embedded WebView.
