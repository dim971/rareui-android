<div align="center">

# Rare UI for Jetpack Compose

**A Jetpack Compose port of [Rare UI](https://www.rareui.com), the animated
component registry by [Swami Malode](https://github.com/swamimalode07), MIT
licensed.**

Nineteen components that are worth the frame budget: orbs that drift and
listen, letters that fall and pile up, a bell that swings from its crown and
drags its clapper behind it.

[![CI](https://github.com/dim971/rareui-android/actions/workflows/ci.yml/badge.svg)](https://github.com/dim971/rareui-android/actions/workflows/ci.yml)
[![Kotlin 2.4](https://img.shields.io/badge/Kotlin-2.4-7F52FF.svg)](https://kotlinlang.org)
[![minSdk 26](https://img.shields.io/badge/minSdk-26-3DDC84.svg)](#requirements)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Foundation-4285F4.svg)](https://developer.android.com/jetpack/compose)
[![JitPack](https://jitpack.io/v/dim971/rareui-android.svg)](https://jitpack.io/#dim971/rareui-android)
[![Licence](https://img.shields.io/badge/licence-MIT-blue.svg)](LICENSE)

</div>

The components, their look and their motion are Swami Malode's work. This
repository ports them to Compose: every spring, easing curve, delay and magic
number is read out of the upstream React source rather than matched by eye, so
a component here moves the way the same component moves on rareui.com.

Its twin is [rareui-ios](https://github.com/dim971/rareui-ios). The two are
ports of the same original and are kept in step deliberately: the same
constants, the same departures, and the same numbers asserted on both sides.

Where a faithful port was not possible, it is written down rather than left to
be discovered. See [Fidelity](#fidelity).

```kotlin
AnimatedCounter(value = total, prefix = "$")
```

## Contents

- [Requirements](#requirements)
- [Installation](#installation)
- [Quick start](#quick-start)
- [Components](#components)
- [Theming](#theming)
- [Motion and accessibility](#motion-and-accessibility)
- [Fidelity](#fidelity)
- [Showcase app](#showcase-app)
- [Documentation](#documentation)
- [Contributing](#contributing)
- [Credits](#credits)
- [Licence](#licence)

## Requirements

Android 8.0 (API 26) or later, Kotlin 2.4, Compose Foundation. No dependencies
beyond Compose itself, and there will not be any.

Two components do more where the platform allows it and say so where it does
not. `FluidOrb` runs upstream's own fragment shader from Android 13, where
`RuntimeShader` exists, and falls back to a drifting gradient below it. The
components that blur, which are the emoji burst, the folder's flap and the
heatmap's month labels, do so from Android 12. Everything else behaves the same
from API 26 up.

## Installation

Through JitPack:

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
    implementation("com.github.dim971:rareui-android:0.1.1")
}
```

## Quick start

```kotlin
import io.github.dim971.rareui.components.feedback.NotificationBell
import io.github.dim971.rareui.components.inputs.OtpInput
import io.github.dim971.rareui.components.navigation.GooeyNav
import io.github.dim971.rareui.theme.RareUiTheme
import io.github.dim971.rareui.theme.rareUiColors

@Composable
fun Home() {
    var tab by remember { mutableIntStateOf(0) }
    var code by remember { mutableStateOf("") }

    RareUiTheme(colors = rareUiColors(dark = isSystemInDarkTheme(), accent = Color.Magenta)) {
        Column(verticalArrangement = Arrangement.spacedBy(32.dp)) {
            GooeyNav(items = listOf("Home", "Docs", "Pricing"), selection = tab, onSelect = { tab = it })

            OtpInput(code = code, onCodeChange = { code = it }, onComplete = ::verify)

            NotificationBell(count = unread)
        }
    }
}
```

Every component works without a theme around it: with none provided they follow
the system's appearance.

## Components

All nineteen, grouped as rareui.com groups them.

| Component | What it is |
| --- | --- |
| `AnimatedCounter` | An odometer. Each digit is a wheel of eleven faces, the eleventh being a second zero so the wrap from nine forward to zero never travels backward. Direction aware, with places that roll in as the number grows. |
| `CodeBlock` | Source in a framed panel with a gutter and a copy button. The whole syntax theme is twelve shades of one accent, and a light appearance is the same ramp upside down. Knows Swift, Kotlin, TypeScript, JSON and shell. |
| `FolderComponent` | A folder whose flap tips back and whose contents fan out of it, in three states: at rest, under a pointer, and open. |
| `GitHubActivity` | A contribution heatmap that draws itself a column at a time, with a footer that lifts up over the grid and becomes a ranked list. |
| `GravityLetters` | Letters that fall out of your finger and pile up. Hold to pour, drag to steer, tilt the device to make the heap slide. No physics engine: a height map eight points wide. |
| `StepPlayer` | A row of dots where the current one stretches into a bar and fills. Play morphs into pause; every measurement is a fraction of the track's height. |
| `FluidOrb` | A circle of colour being stirred, by an AGSL transliteration of upstream's fragment shader. |
| `GridReveal` | A placeholder that becomes a picture by dividing itself into it, busiest parts first. Paces itself when there is nothing to show yet. |
| `MatrixOrb` | A grid of dots that breathes, ripples or thinks. The three states crossfade, so an interruption blends from what is on screen. |
| `BounceSidebar` | A list with a dot in the gutter that arcs between rows, swinging out by the same amount whatever the distance. |
| `GooeyNav` | A segmented bar whose selected tile detaches, stretching the seams until they part. The seam is a drawn pair of curves, not a filter. |
| `HookSidebar` | A rail down the gutter that stops at the current row and hooks into it, with a second rail that follows a pointer. |
| `ProximitySidebar` | A page outline as dashes that swell as a pointer passes. Under a finger the dash being read swells instead. |
| `ScrollProgress` | A floating pill of reading progress that opens into the page's sections, with a highlight that travels between them. |
| `DeleteButton` | A bin that opens into its own confirmation. The lid overshoots its open angle and the walls redraw shorter as it lifts. |
| `DurationPicker` | Three touching panels that separate to be edited, and a pen that morphs into a tick. |
| `OtpInput` | A row of boxes for a one time code, with characters that roll in and a caret that slides. Autofill and paste both work. |
| `EmojiReaction` | A bar of emoji, and five copies of the one you pick drifting up the screen. Hold to keep sending them. |
| `NotificationBell` | A bell that rings when its count goes up, harder when several arrive at once, with a clapper that trails behind it. |

Full reference: [docs/components.md](docs/components.md).

## Theming

`RareUiTheme` collects the palette upstream repeats by hand, and travels through
a composition local the way its CSS custom properties cascade.

| Property | Default | What it does |
| --- | --- | --- |
| `surface` | `#F4F4F9` / `#262626` | The raised ground almost every component sits on |
| `surfaceRecessed` | `#E7E7EF` / `#1B1B1B` | The ground a raised control sinks into |
| `foreground` | `#0A0A0A` / `#FAFAFA` | Text and icons at full strength |
| `glyph` | `#868593` / `#9B9AA7` | Inactive glyphs and labels |
| `accent` | `#FC4C01` | What a selection is marked in |
| `track` | `#3C3C43` / `#EBEBF5` | The filled part of a track |
| `red` `orange` `green` `blue` `violet` | Apple's system colours | Semantic ink, which is what upstream reaches for by hex |

```kotlin
RareUiTheme(colors = rareUiColors(dark = isSystemInDarkTheme(), accent = Color.Magenta)) {
    // ...
}
```

More in [docs/theming.md](docs/theming.md).

## Motion and accessibility

Motion for React and Compose do not describe a spring the same way, and that is
the one place a port like this drifts without anyone noticing. Motion gives a
damping **coefficient**; Compose wants a damping **ratio**, which is that
coefficient over twice the root of the stiffness times the mass. The conversion
is written once, in `theme/Motion.kt`, and every component calls it rather than
converting by hand.

The two forms have separate names on purpose. As overloads of one name, two
positional floats fitted both, Kotlin quietly picked the shorter signature, and
a spring of 520 and 30 became a five hundred and twenty second duration that
crawled. `rareUiSpring` takes a stiffness and a damping; `rareUiVisualSpring`
takes a visual duration and a bounce.

Two components integrate a spring by hand instead, and each has a reason. The
bell is pushed rather than aimed, and its clapper follows the bell's speed from
one frame to the next rather than heading anywhere. The matrix orb's spring
feeds a draw call rather than a view property.

The system's animator duration scale is honoured everywhere, the way upstream
honours `prefers-reduced-motion`: transitions become instant, the canvas
components draw a single still frame, and nothing freezes mid-animation. A
component that stops where it happens to be rather than settling is a bug.

Every component carries a role, a content description and a selection state
where one applies, and the ones that draw rather than lay out describe
themselves rather than their drawing.

## Fidelity

The claim this library makes is that its motion is upstream's, by value rather
than by eye. That is only worth something if the exceptions are written down, so
[docs/fidelity.md](docs/fidelity.md) lists all of them: what could not come
across, why, and what was done instead.

The parts that can be checked exactly are checked: two hundred and twenty-four
tests covering the grouping arithmetic, the field functions, the gooey seam's
waist, the gravity height map, the ring alignment behind the path morph, the
tokeniser, the colour ramp and the rest. None of them assert that an animation
looks right. They assert the things that can be wrong without looking wrong.

## Showcase app

A catalog app listing every component with a live preview, a screen per
component showing its variants with copyable code, and an accent picker that
restyles the whole set at once.

```sh
./gradlew :showcase:installDebug
adb shell am start -n io.github.dim971.rareui.showcase/.MainActivity
```

It opens straight to one screen if you name it, which is what makes the
screenshots reproducible:

```sh
adb shell "am start -n io.github.dim971.rareui.showcase/.MainActivity \
    -e component 'Gravity Letters'"
```

The quoting matters: `adb shell` re-parses the command on the device, so a name
with a space in it has to survive two shells.

## Documentation

| Document | What it covers |
| --- | --- |
| [Getting started](docs/getting-started.md) | Installing, the first component, the patterns |
| [Components](docs/components.md) | Every component, its parameters and its behaviour |
| [Theming](docs/theming.md) | The theme, the component colours, the motion vocabulary |
| [Fidelity](docs/fidelity.md) | Every departure from upstream, and why |
| [Architecture](docs/architecture.md) | How a component is put together, and the shared core |
| [Coding style](docs/coding-style.md) | The conventions, and which of them a machine enforces |

## Contributing

Issues and pull requests are both welcome. See
[CONTRIBUTING.md](CONTRIBUTING.md) for how to build, test and what the review
looks for. Everyone taking part is expected to follow the
[Code of Conduct](CODE_OF_CONDUCT.md).

## Credits

**[Rare UI](https://www.rareui.com) is by [Swami Malode](https://github.com/swamimalode07)**.
The components, the motion and the look are his.
[`swamimalode07/rare-ui`](https://github.com/swamimalode07/rare-ui) is the
original, and worth reading: the animation constants in it are unusually
deliberate, which is what makes a faithful port possible at all.

This repository is a port. It contributes a Kotlin transcription, the tests
that keep the numeric parts honest, and the Compose plumbing around them.

## Licence

MIT. See [LICENSE](LICENSE). Upstream Rare UI is (c) 2026 Swami Malode, also
MIT; [NOTICE](NOTICE) records the attribution in full.
