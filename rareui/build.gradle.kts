import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    // AGP 9 brings Kotlin support in-box; the separate kotlin-android plugin
    // is gone, and the Compose compiler comes with buildFeatures.compose.
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.maven.publish)
}

version = "0.2.0"
group = "io.github.dim971"

android {
    namespace = "io.github.dim971.rareui"
    compileSdk = 37

    defaultConfig {
        // API 26 rather than lower: several components lean on shaders and
        // blurs that arrived later, and each of those degrades from here
        // rather than being missing. Below it the degradations pile up.
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

// Configured for Maven Central but never run from here: publishing needs
// credentials and a signing key this repository does not carry. Releases go
// out through JitPack, which builds the tag itself.
// `./gradlew publishToMavenLocal` works for trying a consumer against an
// unreleased build.
mavenPublishing {
    publishToMavenCentral()
    // Signed only when there is something to sign with. Maven Central requires a signature
    // and nothing else does, so demanding one unconditionally breaks both a local publish
    // and JitPack, which builds this from source and has no key of ours.
    if (providers.gradleProperty("signingInMemoryKey").isPresent) {
        signAllPublications()
    }
    coordinates(group.toString(), "rareui-compose", version.toString())

    pom {
        name.set("Rare UI for Jetpack Compose")
        description.set("Nineteen animated components, ported from rareui.com with its motion intact.")
        url.set("https://github.com/dim971/rareui-android")
        licenses {
            license {
                name.set("MIT License")
                url.set("https://opensource.org/licenses/MIT")
            }
        }
        developers {
            developer {
                id.set("dim971")
                name.set("Dimitri Merault")
            }
        }
        scm {
            url.set("https://github.com/dim971/rareui-android")
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

// A component library's surface is its whole point, so it is stated rather than
// inferred. Only for the library itself: a test is not an API, and asking it to
// declare visibility on every case is noise.
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    if (!name.contains("Test")) {
        compilerOptions.freeCompilerArgs.add("-Xexplicit-api=strict")
    }
}

dependencies {
    // The platform travels with the API rather than behind it. Compose is exposed through
    // `api`, so a consumer sees those dependencies; declared as `implementation` the bill
    // of materials stays behind and they arrive with no version at all, which is a library
    // nobody can resolve.
    api(platform(libs.compose.bom))
    api(libs.compose.foundation)
    api(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.text)
    implementation(libs.androidx.core.ktx)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
}
