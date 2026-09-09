plugins {
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.maven.publish) apply false
    alias(libs.plugins.ktlint)
}

// The Kotlin coding conventions, enforced rather than just documented.
subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
}
