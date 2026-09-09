# Fidelity

Every spring, easing curve, delay, threshold and magic number in this library was read out
of `swamimalode07/rare-ui`'s `components/ui/*.tsx` rather than matched by eye. That is the
claim the whole port rests on, and it is only worth anything if the places where it is not
true are written down. This is that list.

Nothing here is an accident. Each entry is a decision, and each says why.

The iOS twin keeps the same list. Where an entry appears in both, it is the same decision
taken twice, and where one platform can do something the other cannot, that is said here.

## What could not come across unchanged

**FluidOrb: the shader needs Android 13.** The GLSL is transliterated to AGSL and is
otherwise identical, but `RuntimeShader` arrived in Android 13 and there is nothing to
compile it with below that. Under API 33 the orb falls back to a gradient that reuses the
shader's own drift, so it moves on the same clock and with the same period: a bright pool
wandering across a body that darkens toward the bottom. It is not the same picture. It is
the same object in motion.

**Blur needs Android 12.** `Modifier.blur` is a render effect, and render effects arrived
in Android 12. Three components use one, and each degrades rather than failing: an emoji
climbing away loses opacity instead of sharpness, the folder shows the cards behind its
flap sharp, and the heatmap's month labels fade in rather than resolving out of a blur.

**AnimatedCounter: the face box is opened out by a quarter.** Upstream draws each digit in
a box `1.5em` tall, which leaves enough air above and below the glyph that the mask's fade
never touches it. A Compose line box is nearer `1.2em`, so the measured height is
multiplied by 1.25 to put the glyph back in the clear. Without it the fade reaches into the
digit and a resting number looks smudged.

**OtpInput: one text field behind the row, not one per box.** Upstream gives every box its
own input, because on the web that is the only way to put a caret in a particular box, and
it then reimplements most of a text field on top. Here the row is backed by a single field
with the boxes drawn over it, which is what makes the platform behave: autofill offers the
code straight from a message, and paste lands whole. The price is upstream's arrow key
editing in the middle of a code, which a touch keyboard has no way to ask for anyway.

**The squircles are rounded rectangles.** Upstream builds them with figma-squircle at full
corner smoothing, and iOS has the same curve natively. Compose has no smooth corner shape,
so `OtpInput`, `DurationPicker` and `ScrollProgress` use `RoundedCornerShape`. It is the
difference between a squircle and a circle at each corner, and it is visible if you look
for it.

**EmojiReaction: the emoji are text.** Upstream loads Apple's artwork from a CDN, because a
browser on Windows would otherwise draw something else entirely. Here the system draws
them, which is what every other Android application does. It removes a network dependency
and lets a caller pass any emoji rather than the five whose file names upstream ships.

**EmojiReaction: the bar is not clamped to the window, and it can be clipped.** Upstream
shifts the bar sideways to keep it on screen; the alignment parameter is the answer to the
same question here. The bar and the copies flying out of it are drawn inside the
component's own bounds, so an ancestor that clips will cut them off. The SwiftUI port has
the same limitation inside a scroll view.

**EmojiReaction: no press-and-drag from the trigger.** Upstream lets you press the trigger,
drag along the bar and release over an emoji. That is a pointer idiom needing a gesture
that spans views.

**HookSidebar and ProximitySidebar: the pointer effects need a pointer.** Both appear with
a mouse, a trackpad or a stylus. Upstream has the same limitation for the same reason, and
both components already carry the fallback upstream wrote: the hook sidebar shows only the
accent rail, and the proximity sidebar swells the dash for whatever is being read. The
proximity sidebar goes one further than upstream, because a finger dragging down the column
is a pointer for as long as it lasts and the dashes follow it.

**GooeyNav, HookSidebar and BounceSidebar: no route syncing.** Upstream reads Next.js's
pathname to work out which item is current. Selection is a parameter here.

**StepPlayer: durations are seconds.** Upstream states them in milliseconds. Every other
duration in this library is a number of seconds, and one that is not would be a trap rather
than a faithfulness.

**StepPlayer and DurationPicker: the icon morphs are hand-authored.** Upstream uses flubber
to interpolate between arbitrary paths. For the play triangle and the pause bars there is
no need: split the triangle down its middle and its two halves correspond to the two bars
corner for corner, which is exact rather than approximate. For the pen and the tick there
is no such correspondence, so `RareUiPathMorph` does what flubber does, by walking both
outlines, sampling the same number of evenly spaced points along each, and turning one ring
until it lines up with the other. Replay is left out of the morph exactly as upstream
leaves it out.

**DurationPicker: the sway is taken from direction rather than from velocity.** Upstream
reads the gap's own velocity and maps it to a three point lean, so the contents lag behind
the opening. The lean is taken from the direction of travel and carried by the same spring.
It comes out the same at both ends and slightly gentler in between.

**GitHubActivity: it takes data, it does not fetch it.** Upstream can fetch a year of
contributions from a public API given a username. A view that makes its own requests cannot
be tested, cannot be previewed offline, and gives an application no say over caching,
failure or timing. Nothing in this library reaches the network.

**GitHubActivity: the year scrolls rather than being trimmed.** Upstream fits the columns
to the card, because a web page is as wide as the window and a year always fits. A
component on a phone is as wide as it is given, and quietly dropping months would change
what the grid says.

**GitHubActivity and ScrollProgress: the travelling parts do not use shared elements.**
Upstream gets a highlight that travels between rows, and avatars that travel between the
two footer states, from Motion's shared layout id. Compose's counterpart is still
experimental, and a published library should not make its callers opt in to that. The
scroll pill's highlight is drawn behind the rows instead, which is the same single
travelling rectangle by a different route; the heatmap's avatars cross fade.

**ScrollProgress: it is opaque rather than glass.** Upstream floats it on a backdrop blur.
Compose has no way to blur what is behind a composable, and a translucent panel with
nothing blurring underneath reads as a fault rather than as glass, so the pill is opaque
and lifted by a shadow instead.

**GridReveal: it takes an image, not a URL.** For the same reason the heatmap takes data.

**FolderComponent: the placeholder lines are level.** Upstream draws every one of them with
a transform matrix carrying a skew of about two thousandths of a degree, the residue of
whatever drew the original artwork. It is a fifth of a pixel across the whole card.

**FolderComponent: no inner shadows.** Upstream builds them out of a blur and a composite.
Compose has no inner shadow, and the hairline borders carry the same separation without
one.

**CodeBlock: the highlighter is not Prism.** Upstream uses a library that knows nearly
three hundred languages. Taking on something comparable would break the rule this library
and its iOS twin both hold to, which is that they have no dependencies at all. What a
component gallery shows is a handful of languages, so this covers Swift, Kotlin,
TypeScript, JSON and shell, and falls back to plain text rather than guessing. The palette,
which is the part that makes the component what it is, is ported exactly.

## Things this platform makes easier

Not every difference is a loss.

**The badge's mask rides a real velocity.** The notification bell's rolling digits fade
their edges by how fast the column is moving. Compose reports an `Animatable`'s velocity
directly; SwiftUI does not, and the iOS port has to work it out from the difference between
one frame and the next.

**The counter aims from where the wheel is.** Both ports do this, but Compose hands over
the live value of a running animation, so the Kotlin one asks for it rather than tracking
it.

## What is checked rather than trusted

Two hundred and twenty-nine tests, and none of them assert that an animation looks right.
They assert the things that can be wrong without looking wrong:

- the counter's grouping, padding, rounding and wheel aiming
- the spring conversion itself, including that two positional floats mean a stiffness and a
  damping
- the matrix orb's field functions, their ranges, and that its synthesised level has no
  corner in it
- the gooey seam's waist, and which seams open at all
- the bounce dot's arc, and that its swing is the same size at every distance
- the hook rail's geometry
- what a one time code field accepts, and the proportions of its boxes
- the bell's ring: its weighting, its cap, and that it pushes the way it is already going
- the hand integrated spring, including that the bell's is underdamped
- the emoji burst's keyframe reader, and that a copy leans the way it wanders
- the heatmap's levels, week arithmetic and month labels
- the proximity dash's swelling
- which section name the scroll pill shows
- the delete button's bin geometry
- the step player's proportions and its play to pause morph
- the path morph's ring alignment and interpolation
- the folder's three states
- the code block's tokeniser, including that the tokens joined back together are exactly
  the source that went in
- the colour arithmetic the syntax theme is derived through
- the gravity height map: stacking, sliding, walls, and slopes
- the grid reveal's subdivision, its ordering, and its pacing

Everything else is checked by eye, in the showcase, against rareui.com.
