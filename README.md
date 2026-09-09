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
[![Licence](https://img.shields.io/badge/licence-MIT-blue.svg)](LICENSE)

</div>

The components, their look and their motion are Swami Malode's work. This
repository ports them to Compose: every spring, easing curve, delay and magic
number is read out of the upstream React source rather than matched by eye, so
a component here moves the way the same component moves on rareui.com.

Its twin is [rareui-ios](https://github.com/dim971/rareui-ios). The two are
ports of the same original and are kept in step deliberately: the same
constants, the same departures, and the same numbers asserted on both sides.

## Contents

- [Requirements](#requirements)
- [Installation](#installation)
- [Components](#components)
- [Contributing](#contributing)
- [Credits](#credits)
- [Licence](#licence)

## Requirements

Android 8.0 (API 26) or later, Kotlin 2.4, Compose Foundation. No dependencies
beyond Compose itself.

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
    implementation("com.github.dim971:rareui-android:0.1.0")
}
```

## Components

The port is in progress. Components land one at a time, each with its tests,
its showcase screen and its reference entry; this table is filled in as they
arrive.

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
