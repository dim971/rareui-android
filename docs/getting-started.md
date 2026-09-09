# Getting started

## Installing

The library is published through JitPack, built from the tags of this repository.

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

```kotlin
// build.gradle.kts
dependencies {
    implementation("com.github.dim971:rareui-android:0.2.0")
}
```

Compose Foundation comes with it, through `api`, so a project that only wants these
components does not have to declare Compose itself.

## Requirements

Android 8.0 (API 26) or later, Kotlin 2.4, and a project already set up for Compose.

Two things do more where the platform allows it. `FluidOrb` runs upstream's own fragment
shader from Android 13 and falls back to a drifting gradient below it. The three components
that blur do so from Android 12 and degrade rather than failing below it. Everything else
behaves the same from API 26 up.

## The first component

```kotlin
import io.github.dim971.rareui.components.display.AnimatedCounter

@Composable
fun Total(amount: Double) {
    AnimatedCounter(value = amount, decimals = 2, prefix = "$")
}
```

That is the whole of it. There is no theme to install, no provider to wrap, and no
initialisation.

## The patterns

**State is hoisted.** Every component takes its state and reports changes, in the way
Compose does everywhere:

```kotlin
var tab by remember { mutableIntStateOf(0) }
GooeyNav(items = listOf("Home", "Docs"), selection = tab, onSelect = { tab = it })
```

**Nothing reaches the network.** `GitHubActivity` takes contributions and `GridReveal`
takes an image. Upstream can fetch both; a component that makes its own requests cannot be
tested, cannot be previewed offline, and gives an application no say over caching or
failure.

**Colours come from the theme, and a parameter beats it.** See
[theming](theming.md).

**A component drawn on a canvas takes a text style rather than reading one.** The library
depends on Compose Foundation and not on Material, so there is no `LocalTextStyle` to read.
Where type matters, it is a parameter with a sensible default.

## Where to go next

| Document | What it covers |
| --- | --- |
| [Components](components.md) | Every component, its parameters and its behaviour |
| [Theming](theming.md) | The theme, the component colours, the motion vocabulary |
| [Fidelity](fidelity.md) | Every departure from upstream, and why |
| [Architecture](architecture.md) | How a component is put together, and the shared core |
| [Coding style](coding-style.md) | The conventions, and which of them a machine enforces |

The showcase app in this repository is the fastest way to see all nineteen:

```sh
./gradlew :showcase:installDebug
adb shell am start -n io.github.dim971.rareui.showcase/.MainActivity
```
