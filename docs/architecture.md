# Architecture

## Layout

```
rareui/src/main/kotlin/io/github/dim971/rareui/
  core/         pure maths and helpers with no UI in them
  theme/        RareUiTheme, the motion vocabulary, their composition locals
  components/   the nineteen, grouped as rareui.com groups them
rareui/src/main/res/raw/   the AGSL shader
rareui/src/test/           the parts that can be checked exactly
showcase/                  the catalog app
```

`core` is the part worth knowing about. Nothing in it imports a component, and most of it
imports nothing but the standard library:

| File | What it is |
| --- | --- |
| `RareUiColour` | Colour arithmetic and the HSL conversions the code block's palette is built on |
| `RareUiPathMorph` | Two outlines walked, sampled and lined up, standing in for flubber |
| `RareUiSpringState` | A spring integrated by hand, for when one has to be given a velocity |
| `Arithmetic` | A true modulo and a clamp, matching the JavaScript rather than the Kotlin |

Compose supplies three things this library would otherwise have had to write, and that is
worth saying because the iOS twin did have to write them: `CubicBezierEasing` solves a CSS
easing curve, `PathParser` reads SVG path data, and `PathMeasure` walks a path at even
intervals.

## How a component is put together

Most of them are ordinary Compose: state, a layout, and animations with upstream's numbers
in them. Six are not, and it is worth knowing which.

**Drawn on a clock.** `MatrixOrb`, `FluidOrb`, `GridReveal` and `NotificationBell` run a
`withFrameNanos` loop over a `Canvas`, with a small class holding the simulation between
frames. That class is never observable: stepping it must not invalidate a composition, or
the loop would ask for another frame in response to the frame it is already drawing. What
the canvas watches is the one value the loop publishes.

**Solved, then drawn.** `GravityLetters` keeps a height map of the pile and tells each
glyph where it will land the moment it is dropped. `GridReveal` builds its subdivision and
schedules every split before the first frame. In both cases the frame loop only advances
what was already decided.

**Geometry, not a path.** Anything worth testing is kept out of a `Path`. A Compose path
delegates to an Android one, and an Android one in a plain unit test is a stub that answers
every question with zero. The gooey seam, the bin's walls, the transport morph and the path
morph's rings all return plain numbers, with a thin function turning those into a path at
the draw site.

## Rules the code follows

**Nothing is allocated in a draw pass.** The canvas components run at frame rate. Geometry
is built once, kept in a `remember` or a `drawWithCache`, and rebuilt in place.

**Constants live next to what uses them, with a citation.** A spring's two numbers are
upstream's, and the comment above them says which file they came from. That is the only
thing that makes the port checkable later.

**Explicit API mode.** Every public declaration states its visibility and its return type
and carries KDoc. The compiler enforces it, for the library's own sources and not for the
tests: a test is not an API, and asking it to declare visibility on every case is noise.

**Reduced motion is not optional.** Every component degrades when the system's animator
duration scale is zero, and none of them freeze mid-animation. A component that stops where
it happens to be rather than settling is a bug.

**The library depends on Compose Foundation and nothing else.** Not Material, not Material3
and not an icon pack. Every glyph in this library is drawn, and the components take a text
style rather than reading one from a theme they do not have.
