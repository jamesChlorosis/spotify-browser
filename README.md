# Spotify Browser

Spotify Browser is an unofficial dedicated Android app for the Spotify Web Player at `https://open.spotify.com/`. It uses Mozilla GeckoView, the embeddable Firefox engine, so the app can support compatible Gecko/Firefox WebExtensions without using Android WebView or Chrome extension APIs.

The app stays inside Spotify's platform boundaries: it does not inject scripts into Spotify pages, modify Spotify network traffic, bypass advertisements, bypass DRM, bypass authentication, or alter subscription behavior.

## Features

- Kotlin, AndroidX, Jetpack Compose, Material 3
- Android 10+ (`minSdk 29`)
- Full-screen immersive standalone app experience
- Mozilla GeckoView stable browser engine
- One-time first-launch extension setup for signed HTTPS `.xpi` extensions
- Extension safety policy that rejects Spotify, all-site, proxy, and request-blocking permissions
- Multiple isolated profiles using `GeckoSessionSettings.contextId`
- Persistent login, cookies, DOM storage, local storage, and cache per profile
- Back, forward, refresh, file upload, downloads, and full-screen media
- External non-Spotify links open in the user's default browser
- Settings for desktop/mobile user agent, zoom, JavaScript, autoplay behavior, theme, cache, cookies, and profile data
- DataStore-backed preferences and MVVM structure

## Project Structure

```text
app/src/main/java/com/spotifybrowser/app/
  MainActivity.kt
  data/gecko/              GeckoView runtime, sessions, downloads, extensions, storage
  data/preferences/        DataStore settings
  data/profile/            profile metadata and Gecko storage context IDs
  data/web/                shared URL/link policy and browser state models
  ui/components/           reusable Compose components
  ui/screens/              extension setup, profile selector, browser, settings
  ui/theme/                Material 3 theme
  viewmodel/               MVVM state and actions
```

## Extension Setup

On first launch, the app shows an extension setup screen. Users can install compatible signed Firefox/Gecko `.xpi` packages from direct HTTPS URLs, then tap **Continue**. After Continue is tapped, DataStore records the setup as complete and the screen does not appear again unless app data is cleared.

Chrome Web Store `.crx` extensions are not supported by GeckoView. Extensions that request Spotify host access, all-site access, proxy control, or request-blocking permissions are rejected to keep the app compliant.

The bundled companion extension under `app/src/main/assets/extensions/spotify-browser-companion/` is a no-op placeholder that proves built-in GeckoView extension loading works. It has no content scripts and no Spotify host permissions.

## Build in Android Studio

1. Open this folder in Android Studio.
2. Let Android Studio install/sync Java 17, Android SDK 35, the Android Gradle Plugin, Kotlin, GeckoView, and Compose dependencies.
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

This project intentionally avoids hidden extensions, page injection, ad blocking, traffic interception, authentication bypassing, subscription bypassing, and DRM bypassing. It uses documented GeckoView APIs for profiles, storage, downloads, file prompts, and WebExtensions.
