# Components

All nineteen, grouped as [rareui.com](https://www.rareui.com/components) groups them. Every
default is upstream's own.

Anywhere a colour is not given, the component takes it from the theme; see
[theming.md](theming.md). Every component honours the system's animator duration scale.

## Display

### AnimatedCounter

An odometer. Each digit is a wheel of eleven faces that turns to the digit it should show:
ten digits and a repeat of zero, so the wrap from nine back to zero continues forward onto
an identical face rather than travelling the long way round.

```kotlin
AnimatedCounter(value = total)
AnimatedCounter(value = revenue, decimals = 2, prefix = "$")
AnimatedCounter(value = population, grouping = CounterGrouping.INDIAN)
```

The roll is direction aware. Places that appear as the number grows roll in from zero;
places that disappear fade out. `padStart` sets a floor on the width, which is what stops a
timer jittering.

### CodeBlock

Source code in a framed panel, with a header, a line-number gutter and a copy button. The
entire syntax theme is built from one accent: the hue and saturation are kept, the lightness
is replaced per token kind, and a light appearance is the same ramp upside down.

```kotlin
CodeBlock(code = source, language = CodeLanguage.KOTLIN, filename = "Total.kt")
CodeBlock(code = json, language = CodeLanguage.JSON, accent = Color(0xFF00A0A0), highlightedLines = setOf(3, 4))
```

Knows Swift, Kotlin, TypeScript, JSON and shell, and falls back to plain text. `showsFrame`,
`showsHeader`, `showsLineNumbers` and `showsCopyButton` each turn a part of the chrome off;
with no header at all the copy button floats over the code.

### FolderComponent

A folder whose flap tips back and whose contents fan out of it. Three states: at rest, under
a pointer, and open. A finger reaches the first and the last.

```kotlin
FolderComponent(color = FolderColor.BLUE, size = FolderSize.LARGE)
```

Each of the three colours is a complete set rather than a tint: the black folder holds pale
cards and the white one holds dark ones.

### GitHubActivity

A contribution heatmap that draws itself a column at a time, with a footer that lifts up
over the grid and becomes a ranked list of repositories.

```kotlin
GitHubActivity(contributions = days, repos = repositories, showsMonths = true)
GitHubActivity(contributions = days, accent = Color(0xFFFC4C01), cellSize = 8.dp, months = 6)
GitHubActivity(contributions = days, accentScale = listOf(dim, mid, bright, brightest))
```

`accentScale` sets the levels yourself rather than shading one colour five ways: four
colours are the four levels that have something in them, five or more set the empty level
too. `expanded` and `onExpandedChange` control the footer, which otherwise keeps its own
state.

It takes its data rather than fetching it, and the year scrolls rather than being trimmed to
fit. With no repositories there is no footer, since a footer that ranks nothing is worse
than none.

### GravityLetters

Letters that fall out of your finger and pile up. Touch drops one, holding pours them,
dragging steers the pour, and tilting the device past ten degrees makes the heap slide.

```kotlin
GravityLetters(modifier = Modifier.fillMaxWidth().height(320.dp))
GravityLetters(glyphs = GravityGlyphs.NUMBERS, gravity = 1200.0, maxGlyphs = 400)
GravityLetters(items = listOf("Rare", "UI"))
```

No physics engine: the pile is a height map eight points wide, and a glyph is told where it
will land the moment it is dropped.

### StepPlayer

A row of dots where the one being played stretches into a bar and fills. Play morphs into
pause; every measurement is a fraction of the track's height.

```kotlin
StepPlayer(
    steps = List(5) { StepPlayerStep() },
    index = step,
    playing = playing,
    onIndexChange = { step = it },
    onPlayingChange = { playing = it },
)
```

`seekable` lets a step be tapped, `loop` starts again at the end, and `controlPosition` puts
the control on either side. A step can carry a duration of its own.

## AI Kit

### FluidOrb

A circle of colour being stirred, by upstream's own fragment shader transliterated to AGSL.

```kotlin
FluidOrb()
FluidOrb(size = 120.dp, color = Color(0xFFAF52DE))
```

The shader needs Android 13. Below that the orb falls back to a gradient drifting on the
same clock; see [fidelity.md](fidelity.md).

### GridReveal

A placeholder that becomes a picture by dividing itself into it, a hundred and eighty times,
busiest parts first.

```kotlin
GridReveal(image = photo, caption = "Generating", onRevealComplete = ::ready)
GridReveal(progress = uploaded, aspect = 16f / 9f)
```

Give it a `progress` and it follows that. Leave it out and it paces itself, creeping toward
nine tenths and holding there until the picture arrives.

### MatrixOrb

A grid of dots clipped to a circle that breathes, ripples or thinks.

```kotlin
MatrixOrb(state = MatrixOrbState.LISTENING)
MatrixOrb(state = MatrixOrbState.LISTENING, level = microphone.level)
```

The three states crossfade rather than switching, so an interruption blends from whatever is
on screen. With no `level` it synthesises one, so the orb has something to do while nothing
is listening.

## Navigation

### BounceSidebar

A list with a dot in the gutter that arcs from one row to the next, swinging out by the same
amount whatever the distance.

```kotlin
BounceSidebar(items = items, selection = section, onSelect = { section = it })
```

Use `bounceSidebarHeading` for a row that takes the dot's colour and cannot be selected.

### GooeyNav

A segmented bar whose selected tile detaches, stretching the seams until they part.

```kotlin
GooeyNav(items = listOf("Home", "Docs"), selection = tab, onSelect = { tab = it })
GooeyNav(items = items, selection = tab, onSelect = { tab = it }, size = GooeyNavSize.SMALL)
```

The seam is a drawn pair of curves rather than a blur filter, and it breaks once the gap
passes a fraction of the separation.

### HookSidebar

A rail down the gutter that stops a corner short of the current row and hooks into it.

```kotlin
HookSidebar(items = items, selection = section, onSelect = { section = it }, label = "Docs")
HookSidebar(items = items, selection = section, onSelect = { section = it }, dashed = false)
```

A second, fainter rail follows a pointer, so it appears with a mouse, a trackpad or a stylus
and never under a finger. Upstream behaves the same way.

### ProximitySidebar

A page outline drawn as dashes that swell as a pointer passes them.

```kotlin
ProximitySidebar(sections = outline, selection = reading, onSelect = { scrollTo(it) })
ProximitySidebar(sections = outline, side = ProximitySidebarSide.TRAILING)
```

With no pointer the dash for whatever is being read swells instead. A finger dragging down
the column counts as one for as long as it lasts.

### ScrollProgress

A floating pill showing how far down a page the reader is, which opens into the page's
sections.

```kotlin
ScrollProgress(sections = sections, progress = read, selection = section, onSelect = { scrollTo(it) })
```

Progress and the current section are supplied rather than observed, so the pill works over
anything that scrolls. The highlight travels between the rows rather than fading in and out.

## Inputs

### DeleteButton

A bin that opens into its own confirmation rather than into a dialogue.

```kotlin
DeleteButton(onConfirm = { remove(item) }, onCancel = { keep(item) })
```

The tile grows sideways, a recessed panel appears in the space it made, and the lid swings
back past its open angle before settling while the walls redraw shorter beneath it.

### DurationPicker

Three touching panels, an hours field, a minutes field and a pen that morphs into a tick.

```kotlin
DurationPicker(value = duration, onValueChange = { duration = it }, onConfirm = ::schedule)
DurationPicker(value = duration, onValueChange = { duration = it }, maxHours = 8, maxMinutes = 59)
DurationPicker(value = duration, onValueChange = { duration = it }, defaultEditing = true)
```

Typing past a limit clamps the field and gives it a nudge, so the refusal is felt rather
than read.

### OtpInput

A row of boxes for a one time code, with characters that roll in and a caret that slides.

```kotlin
OtpInput(code = code, onCodeChange = { code = it }, onComplete = ::verify)
OtpInput(code = code, onCodeChange = { code = it }, length = 4, characterSet = OtpCharacterSet.ALPHANUMERIC)
OtpInput(code = code, onCodeChange = { code = it }, status = OtpStatus.ERROR)
OtpInput(code = code, onCodeChange = { code = it }, autoFocus = true)
```

One text field backs the whole row, which is what makes autofill and paste work. Anything
that does not belong in the code is dropped rather than refused, so a pasted code can arrive
with its own punctuation.

## Feedback

### EmojiReaction

A button that opens a bar of emoji, and sends five copies of the one you pick drifting up
the screen.

```kotlin
EmojiReaction(onReact = { emoji -> post(emoji) })
EmojiReaction(emojis = listOf("🔥", "💯", "🎉"), size = EmojiReactionSize.LARGE)
```

Holding an emoji keeps sending them. The copies rise out of the component's own bounds, so
an ancestor that clips will cut them off.

### NotificationBell

A bell that swings when its count goes up, with a clapper that trails behind it.

```kotlin
NotificationBell(count = unread)
NotificationBell(count = unread, variant = NotificationBellVariant.DOT, color = NotificationBellColor.BLUE)
```

The ring is a push rather than a destination: several notifications arriving at once make it
swing harder rather than starting the swing again, and five is as hard as it gets. The
badge's digits follow the count at each place rather than the digit, so a column always
knows which way to turn.
