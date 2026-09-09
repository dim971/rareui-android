# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project
adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.1]

### Fixed

- The published library can be resolved. In 0.1.0 the Compose bill of materials
  was declared as an implementation dependency while Compose itself was exposed
  through `api`, so the published metadata carried `androidx.compose.foundation`
  and `androidx.compose.ui` with no version at all and every consumer failed
  with `Could not find androidx.compose.foundation:foundation:`. Use this
  version rather than 0.1.0.

## [0.1.0]

First release. A Jetpack Compose port of [Rare UI](https://www.rareui.com), the
animated component registry by Swami Malode.

### Added

- All nineteen components from rareui.com, grouped as it groups them.
  - Display: `AnimatedCounter`, `CodeBlock`, `FolderComponent`, `GitHubActivity`,
    `GravityLetters`, `StepPlayer`.
  - AI Kit: `FluidOrb`, `GridReveal`, `MatrixOrb`.
  - Navigation: `BounceSidebar`, `GooeyNav`, `HookSidebar`, `ProximitySidebar`,
    `ScrollProgress`.
  - Inputs: `DeleteButton`, `DurationPicker`, `OtpInput`.
  - Feedback: `EmojiReaction`, `NotificationBell`.
- `RareUiTheme`, carrying the palette upstream repeats by hand through a
  composition local, and `RareUiEasing` with `rareUiSpring` and
  `rareUiVisualSpring`, carrying the motion vocabulary it reaches for
  repeatedly.
- A shared core with no UI in it: colour and HSL arithmetic, a path morph
  standing in for flubber, and a spring integrated by hand.
- A showcase catalog app with live examples, copyable code and an accent picker
  that restyles the whole set. It opens straight to one component when named.
- Two hundred and twenty-four tests over the parts that can be checked exactly.

### Changed from upstream

The full list is in [docs/fidelity.md](docs/fidelity.md). The ones worth knowing
about before reaching for a component:

- Nothing in this library touches the network. `GitHubActivity` and `GridReveal`
  take their data as values rather than fetching it, so an application decides
  what is loaded and when.
- `OtpInput` is backed by one text field rather than one per box, which is what
  makes autofill and paste work. Upstream's arrow key editing in the middle of a
  code goes with it.
- `EmojiReaction` draws the system emoji rather than downloading Apple's
  artwork, and takes any emoji rather than the five whose file names upstream
  ships.
- `ScrollProgress` and `ProximitySidebar` take the scroll progress and the
  current section rather than listening for them, because a Compose component
  cannot reach into somebody else's scroll state.
- `StepPlayer` states durations in seconds rather than milliseconds.
- `CodeBlock` uses an in-house highlighter covering Swift, Kotlin, TypeScript,
  JSON and shell, rather than Prism. Its palette, which is the part that makes
  the component what it is, is ported exactly.
- The squircles are rounded rectangles, since Compose has no smooth corner
  shape.
- The pointer effects in `HookSidebar` and `ProximitySidebar` need a pointer, so
  they appear with a mouse, a trackpad or a stylus. Both carry the fallback
  upstream already wrote, and the proximity sidebar also follows a finger
  dragging down it.

### Known limitations

- `FluidOrb` runs upstream's own shader from Android 13, where `RuntimeShader`
  exists. Below that it falls back to a gradient drifting on the same clock.
- The three components that blur, which are `EmojiReaction`, `FolderComponent`
  and `GitHubActivity`, do so from Android 12. Below that each degrades rather
  than failing.
- `GitHubActivity` cross fades its avatars and `ScrollProgress` draws its
  travelling highlight, rather than using shared elements, because Compose's
  shared element API is still experimental and a published library should not
  make its callers opt in to that.

[Unreleased]: https://github.com/dim971/rareui-android/compare/0.1.1...HEAD
[0.1.1]: https://github.com/dim971/rareui-android/releases/tag/0.1.1
[0.1.0]: https://github.com/dim971/rareui-android/releases/tag/0.1.0
