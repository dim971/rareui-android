# Theming

Upstream hardcodes its palette inside each component, as Tailwind classes: the same
`bg-[#F4F4F9] dark:bg-[#262626]` appears in a dozen files, along with the same `#868593`
grey and the same iOS system colours. Those values are collected here once, and they travel
down a composition local the way upstream's CSS custom properties cascade.

Every default is the value the matching upstream component actually uses, so a component
left untouched looks like its counterpart on rareui.com.

## The theme

| Property | Default | What it is |
| --- | --- | --- |
| `surface` | `#F4F4F9` / `#262626` | The raised ground almost every component sits on |
| `surfaceRecessed` | `#E7E7EF` / `#1B1B1B` | The ground a raised control sinks into |
| `background` | white / `#0A0A0A` | The page behind the components |
| `foreground` | `#0A0A0A` / `#FAFAFA` | Text and icons at full strength |
| `glyph` | `#868593` / `#9B9AA7` | Inactive glyphs and labels |
| `border` | black and white at 8% | Hairlines |
| `accent` | `#FC4C01` | What a selection is marked in |
| `track` | `#3C3C43` / `#EBEBF5` | The filled part of a track |
| `red` `orange` `green` `blue` `violet` | Apple's system colours | Semantic ink, which is what upstream reaches for by hex |

## Changing it

```kotlin
RareUiTheme(colors = rareUiColors(dark = isSystemInDarkTheme(), accent = Color.Magenta)) {
    GooeyNav(items = items, selection = tab, onSelect = { tab = it })
}
```

`rareUiColors` builds a complete palette for one appearance, so the accent is the only
thing most callers change. To replace the rest, copy it:

```kotlin
val palette = rareUiColors(dark = true).copy(surface = Color(0xFF101010), glyph = Color.Gray)
```

There is no requirement to provide one at all. The composition local's default is null on
purpose, and a component with no theme around it follows the system's appearance rather
than being stuck in whichever one the default was written for.

## Component colours

Most components also take a colour directly, which wins over the theme:

```kotlin
MatrixOrb(state = MatrixOrbState.LISTENING, color = Color(0xFF30D158))
GooeyNav(items = items, selection = tab, onSelect = { tab = it }, activeColor = Color.Blue)
BounceSidebar(items = items, selection = section, onSelect = { section = it }, dotColor = Color.Magenta)
```

`CodeBlock` is the interesting one: it takes a single accent and derives an entire syntax
theme from it, keeping the hue and the saturation and replacing the lightness once per
token kind. A light appearance is the same ramp upside down. Twelve colours out of one, and
no table to drift.

```kotlin
CodeBlock(code = source, language = CodeLanguage.KOTLIN, accent = Color(0xFF00A0A0))
```

## Motion

`RareUiEasing` holds the curves upstream reaches for repeatedly, and `rareUiSpring` and
`rareUiVisualSpring` are the two conversions from the way Motion for React states a spring
to the way Compose does:

```kotlin
animationSpec = rareUiSpring(stiffness = 520f, damping = 30f)
animationSpec = rareUiVisualSpring(durationSeconds = 0.6f, bounce = 0.18f)
animationSpec = tween(220, easing = RareUiEasing.EaseOutQuint)
```

Motion gives a damping coefficient and Compose wants a damping ratio, which is that
coefficient over twice the root of the stiffness times the mass. Converting by hand in
nineteen components is how a port drifts, so it is done in one place.

## Reduced motion

`rememberRareUiReduceMotion()` reads the system's animator duration scale, which is what
Android's own developer options and accessibility settings turn down. Every component in
this library already calls it. It is public because an application drawing alongside these
components will want to agree with them.
