# Coding style

The [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html) and the
[Compose API guidelines](https://android.googlesource.com/platform/frameworks/support/+/androidx-main/compose/docs/compose-api-guidelines.md)
apply, and the places this project deliberately differs are below.

## Prose

British English, in comments, documentation and commit messages.

Comments explain **why**, not what. A constant that looks arbitrary almost certainly came
from upstream, so the comment says which file it came from.

**No em dash.** Not in code, comments, documentation, commit messages or issue and pull
request text. Use a comma, a colon, a semicolon, parentheses or a full stop. CI fails the
build if the character appears anywhere in the repository.

## Naming

Public names read as English: `AnimatedCounter(value =, decimals =, duration =)`, not
`AnimatedCounter(v =, d =, dur =)`. Where upstream's prop name is already the right word,
it is kept, so that the two can be read side by side.

Free functions that belong to one component are prefixed with it: `gridRevealSelfPaced`,
`bellClapperLag`, `counterFormat`. They are internal, they are tested, and the prefix is
what keeps a file of them legible.

Short identifiers are allowed where the maths uses them: `uv`, `dt`, `x`, `t`. Anywhere
else, a name is a word.

## Animation

Constants are named properties on the file that uses them, never literals at a call site:

```kotlin
/** Upstream's `ROLL_SPRING`. */
private const val ROLL_STIFFNESS = 500f
private const val ROLL_DAMPING = 34f
```

Two numbers at a call site cannot be checked against anything. A named constant with a
citation can.

Motion for React does not map to Compose directly, and the conversion is written down once
in `theme/Motion.kt`:

- `{ stiffness, damping, mass }` is `rareUiSpring(stiffness, damping, mass)`, which turns a
  damping coefficient into the ratio Compose wants
- `{ visualDuration, bounce }` is `rareUiVisualSpring(duration, bounce)`
- `ease: [a, b, c, d]` is `CubicBezierEasing(a, b, c, d)`

The two spring forms have separate names rather than being overloads, because two
positional floats fit both and Kotlin picks the shorter signature without a word. That once
turned a spring of 500 and 34 into a five hundred second duration.

## Testing

Tests are named as sentences that state a claim, using backticked function names:

```kotlin
@Test
fun `the bell cannot be made to spin, however many arrive`() { ... }
```

They test the things that can be wrong without looking wrong. There is no point asserting
that a spring is a spring; there is every point asserting that the grouping is right, that
the arc is the same size at every distance, and that a morph never pinches through itself.

Where a test encodes something subtle, the comment says what would break if it failed.

Anything that needs a framework class a unit test only stubs, a `Path` most of all, is
restructured until the interesting part is plain numbers.

## Commits

Commit messages describe the change and the reasoning, in prose. The subject is an
imperative sentence with no type or scope prefix; the body says why, what was rejected, and
how it was verified.

## What is enforced mechanically

| Rule | By what |
| --- | --- |
| Formatting, line width, wrapping, import order | `ktlintCheck` |
| Visibility, return types and KDoc on public declarations | `-Xexplicit-api=strict` |
| A file named after the single class in it | ktlint |
| No em dash | A `git grep` at the top of every CI run |
| No warnings | A clean rebuild, with the log checked |
| The maths | The test suite |

Everything else in this document is a review conversation.
