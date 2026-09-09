# Contributing

Thanks for taking a look. Issues and pull requests are both welcome.

## Getting set up

```sh
git clone https://github.com/dim971/rareui-android
cd rareui-android
./gradlew :rareui:testDebugUnitTest    # the arithmetic behind the components
./gradlew :showcase:installDebug       # build and install the catalog app
```

You need JDK 17 or later. Gradle comes with the wrapper, and the run
configurations under `.idea/runConfigurations` are checked in so the showcase
is launchable straight after cloning.

## Before you open a pull request

```sh
./gradlew ktlintCheck lint :rareui:testDebugUnitTest
```

Three things the review will look for:

**No warnings.** Not in the library, not in the showcase. A warning that is
tolerated becomes a warning that is ignored.

**The motion still matches upstream.** Every spring, easing curve, delay and
threshold in this library came out of the upstream React source. If you change
one, say which upstream value you are now following, or say plainly that you
are deliberately departing from it and why. "It felt better" is a reason, but
it has to be written down, because the next person will otherwise read the
difference as a bug.

**Parity with iOS.** This library has a
[twin](https://github.com/dim971/rareui-ios). A change to shared behaviour (a
component's states, a theme value, an animation constant) should land in both,
or say plainly why it should not.

## Conventions

[docs/coding-style.md](docs/coding-style.md) is the full version. The short one:

- Comments explain *why*, not *what*. A constant that looks arbitrary almost
  certainly came from upstream, so say which file it came from.
- The library is compiled with `-Xexplicit-api=strict`, so every public
  declaration states its visibility and its return type, and carries KDoc.
- Animation constants live in named properties, not scattered through call
  sites, so the value can be compared against upstream in one place.
- Commit messages describe the change and the reasoning, in prose.

## Reporting a bug

Motion problems are hard to describe in words. A screen recording, or the exact
parameters you passed, turns "the animation looks wrong" into something anyone
can reproduce. Saying what it does on rareui.com instead is even better.

## Code of conduct

Taking part means following the [Code of Conduct](CODE_OF_CONDUCT.md).
