plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}

val externalBuildRoot = providers.gradleProperty("externalBuildRoot")

allprojects {
    externalBuildRoot.orNull?.let { root ->
        val projectBuildPath = if (path == ":") {
            "root"
        } else {
            path.trim(':').replace(':', '/')
        }
        layout.buildDirectory.set(file("$root/$projectBuildPath"))
    }
}
