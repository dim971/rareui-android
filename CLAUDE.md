# Rare UI for Jetpack Compose: working notes

A Compose port of the web library [Rare UI](https://www.rareui.com)
(MIT, (c) 2026 Swami Malode). Its twin is
[rareui-ios](https://github.com/dim971/rareui-ios); the two are kept in step
deliberately.

## The one rule that matters

**Motion constants come from the upstream source, not from taste.**

Every spring, easing curve, stagger delay, threshold and magic number in this
library was read out of `swamimalode07/rare-ui`'s `components/ui/*.tsx`. When
you write `spring(stiffness = 520f, dampingRatio = ...)`, those numbers are
upstream's, and the comment above them should say which file they came from.

This matters because it is the only thing that makes the port checkable. Two
springs that look similar in a screen recording can feel completely different
in the hand, and there is no way to argue about it after the fact unless the
numbers are written down and traceable.

Departing from upstream is allowed, and sometimes necessary: a pointer
proximity effect means nothing on a phone. When it happens, record it in
`docs/fidelity.md` under the component's name. Silent drift is the thing to
avoid.

The mapping between the two animation systems needs care, because Compose and
Motion for React do not state a spring the same way:

- Motion gives `{ stiffness, damping, mass }`. Compose wants a **damping
  ratio**, which is `damping / (2 * sqrt(stiffness * mass))`. There is a helper
  for it in `theme/Motion.kt`; use it rather than converting by hand.
- Motion's newer `{ duration, bounce }` has no direct counterpart. A bounce of
  `b` is a damping ratio of `1 - b`, and the duration is perceptual rather than
  literal, so the stiffness is chosen to match.
- Motion's cubic beziers are `CubicBezierEasing`. Control points outside `0..1`
  are allowed on both sides, which matters for the overshooting curves.

## Layout

```
rareui/src/main/kotlin/io/github/dim971/rareui/
  core/         pure maths and helpers with no UI in them
  theme/        RareUiTheme, the motion vocabulary, their composition locals
  components/   the nineteen, grouped as rareui.com groups them
rareui/src/test/  the parts that can be checked exactly
showcase/         the catalog app
```

## Conventions worth keeping

- **Zero warnings.** Library and showcase.
- **Reduced motion is not optional.** Upstream calls `useReducedMotion()` in
  nearly every component and degrades to a zero-duration transition; the
  canvas-driven ones render a single static frame. Do the same here, from the
  system's animator duration scale. A component that freezes mid-animation
  instead of settling is a bug.
- **Nothing is allocated in a draw pass.** The `Canvas` components run at frame
  rate. Build the geometry once with `remember`, keyed on the inputs that
  change it.
- **`-Xexplicit-api=strict`.** Every public declaration states its visibility
  and its return type, and carries KDoc.
- Comments explain why, not what. If a constant looks arbitrary, say where it
  came from.
- **No em dash (U+2014).** Not in code, comments, docs, commit messages
  or issue and PR text. Use a comma, a colon, a semicolon, parentheses
  or a full stop. CI fails the build if the character reappears.

## Verifying

```sh
./gradlew :rareui:testDebugUnitTest
./gradlew ktlintCheck lint
./gradlew :showcase:installDebug
adb shell am start -n io.github.dim971.rareui.showcase/.MainActivity
```

The showcase opens straight to one component when told which:

```sh
adb shell am start -n io.github.dim971.rareui.showcase/.MainActivity \
    -e component "Gravity Letters"
```

Screenshots: `adb exec-out screencap -p > out.png`.

## Staying in step with iOS

A change to shared behaviour (an animation constant, a theme value, a
component's states) should land in both repositories. The Swift port mirrors
this structure file for file, and the same numbers are asserted on both sides.
