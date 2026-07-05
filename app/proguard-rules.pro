# Keep the app model names readable in crash reports and DataStore JSON migrations.
-keep class com.spotifybrowser.app.data.profile.BrowserProfile { *; }

# WebView clients are referenced reflectively by the Android framework on some devices.
-keep class * extends android.webkit.WebViewClient { *; }
-keep class * extends android.webkit.WebChromeClient { *; }
