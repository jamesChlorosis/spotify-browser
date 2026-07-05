# Keep the app model names readable in crash reports and DataStore JSON migrations.
-keep class com.spotifybrowser.app.data.profile.BrowserProfile { *; }

# GeckoView ships consumer rules for its internals. Keep app-level delegates readable in
# stack traces and crash reports.
-keep class com.spotifybrowser.app.data.gecko.** { *; }
