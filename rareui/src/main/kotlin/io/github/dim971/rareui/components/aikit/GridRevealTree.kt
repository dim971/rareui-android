/*
 * GridRevealTree.kt
 * The subdivision behind GridReveal, ported from the `buildTree`, `measureTree` and
 * `orderByDetail` functions in upstream's `components/ui/grid-reveal.tsx`.
 *
 * The picture is not revealed by fading in. It is revealed by a rectangle splitting in two,
 * and each half splitting again, a hundred and eighty times, with the halves sliding apart
 * from wherever their parent was. The order the splits happen in is not arbitrary: the
 * parts of the picture with the most going on in them split first, so detail arrives before
 * flat colour does.
 */

package io.github.dim971.rareui.components.aikit

import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sin

/** How many cells the picture ends up divided into. */
internal const val GRID_REVEAL_CELLS = 180

/**
 * How many are already apart on the first frame, so the reveal does not start from a single
 * rectangle sitting still.
 */
internal const val GRID_REVEAL_OPENING_CELLS = 4

/** How long one cell takes to separate, in progress rather than in seconds. */
internal const val GRID_REVEAL_MORPH = 0.055

/** Where the last split happens, short of the end so the picture has somewhere to arrive. */
internal const val GRID_REVEAL_LAST_SPLIT = 0.92

/**
 * How far a self-paced reveal creeps toward. It never reaches it, so a load that outruns its
 * estimate keeps moving rather than stopping and waiting.
 */
internal const val GRID_REVEAL_HOLD = 0.9

/** Where the grid stops splitting while it waits, leaving the arrival somewhere to go. */
internal const val GRID_REVEAL_WAIT_CAP = 0.72

/** How large a square the picture is sampled into for its average colours. */
internal const val GRID_REVEAL_SAMPLE = 128

/**
 * One rectangle of the subdivision.
 *
 * A class rather than a value because the tree is walked by reference: a cell knows its
 * children and its parent, and the ordering pass rewrites split times in place.
 */
internal class GridRevealCell(
    val x: Double,
    val y: Double,
    val width: Double,
    val height: Double,
    val parent: GridRevealCell?,
) {
    /** The cell's average colour, once the picture has been sampled. */
    var red = 0.0
    var green = 0.0
    var blue = 0.0

    /** A number of its own, used to vary the grey it shows before the colours arrive. */
    val tone: Double = gridRevealHash(x + 3.1, y + 1.7, width * 31.7)

    /** How much the brightness varies inside it, which is what decides how early it splits. */
    var detail = 0.0

    /** When it splits, in progress. */
    var splitAt = 0.0

    var first: GridRevealCell? = null
    var second: GridRevealCell? = null

    val hasChildren: Boolean get() = first != null
}

/**
 * A repeatable number from three others, so a cell's tone is stable across frames without
 * having to be stored anywhere.
 *
 * @param x the first number.
 * @param y the second.
 * @param z the third.
 * @return a number in `0..1`.
 */
internal fun gridRevealHash(
    x: Double,
    y: Double,
    z: Double,
): Double {
    val n = sin(x * 127.1 + y * 311.7 + z * 74.7) * 43758.5453
    return n - floor(n)
}

/** A built subdivision: the root, and every cell that has children in the order they split. */
internal class GridRevealTree(
    val root: GridRevealCell,
    val branches: List<GridRevealCell>,
)

/**
 * Builds the subdivision.
 *
 * The biggest cell is split each time, which keeps the cells roughly square and makes the
 * count rise one at a time rather than doubling. The jitter in the area only breaks ties
 * between cells of equal size, so the pattern is varied without being random.
 *
 * @param aspect the picture's width over its height.
 * @return the root cell and every cell that has children, in the order they split.
 */
internal fun gridRevealBuildTree(aspect: Double): GridRevealTree {
    val root = GridRevealCell(0.0, 0.0, 1.0, 1.0, null)
    val leaves = mutableListOf(root)
    val branches = mutableListOf<GridRevealCell>()

    while (leaves.size < GRID_REVEAL_CELLS) {
        var pick = 0
        var widest = -1.0
        leaves.forEachIndexed { index, cell ->
            val area =
                cell.width * aspect * cell.height *
                    (1 + 0.12 * gridRevealHash(cell.x, cell.y, 7.3))
            if (area > widest) {
                widest = area
                pick = index
            }
        }

        val parent = leaves.removeAt(pick)
        // Split across the longer side, so halves stay close to square.
        val wide = parent.width * aspect >= parent.height
        val half = if (wide) parent.width / 2 else parent.height / 2
        val first =
            if (wide) {
                GridRevealCell(parent.x, parent.y, half, parent.height, parent)
            } else {
                GridRevealCell(parent.x, parent.y, parent.width, half, parent)
            }
        val second =
            if (wide) {
                GridRevealCell(parent.x + half, parent.y, half, parent.height, parent)
            } else {
                GridRevealCell(parent.x, parent.y + half, parent.width, half, parent)
            }

        parent.first = first
        parent.second = second
        branches += parent
        leaves += first
        leaves += second
    }

    val opening = GRID_REVEAL_OPENING_CELLS - 1
    val rest = maxOf(1, branches.size - opening)
    branches.forEachIndexed { index, cell ->
        // The opening splits sit before zero, so those cells are already apart on frame one.
        cell.splitAt =
            if (index < opening) {
                -GRID_REVEAL_MORPH
            } else {
                GRID_REVEAL_LAST_SPLIT * (index - opening + 1) / rest
            }
    }

    return GridRevealTree(root, branches)
}

/** Running totals for one cell and everything under it. */
private class GridRevealSums {
    var count = 0.0
    var red = 0.0
    var green = 0.0
    var blue = 0.0
    var luminance = 0.0
    var luminanceSquared = 0.0

    fun add(other: GridRevealSums) {
        count += other.count
        red += other.red
        green += other.green
        blue += other.blue
        luminance += other.luminance
        luminanceSquared += other.luminanceSquared
    }
}

/**
 * Gives every cell the average colour of the picture underneath it, and the spread of
 * brightness inside it.
 *
 * The spread is the variance, and it is what tells the ordering pass which parts of the
 * picture are worth splitting early: a patch of sky varies hardly at all, a face varies a
 * great deal.
 *
 * @param root the tree's root.
 * @param pixels the picture, sampled into a square, four bytes a pixel.
 * @param size the square's side.
 */
internal fun gridRevealMeasure(
    root: GridRevealCell,
    pixels: IntArray,
    size: Int,
) {
    fun gather(cell: GridRevealCell): GridRevealSums {
        val sums = GridRevealSums()
        val first = cell.first
        val second = cell.second

        if (first != null && second != null) {
            sums.add(gather(first))
            sums.add(gather(second))
        } else {
            val x0 = (cell.x * size).roundToInt()
            val y0 = (cell.y * size).roundToInt()
            val x1 = maxOf(x0 + 1, ((cell.x + cell.width) * size).roundToInt())
            val y1 = maxOf(y0 + 1, ((cell.y + cell.height) * size).roundToInt())

            for (y in y0 until minOf(y1, size)) {
                for (x in x0 until minOf(x1, size)) {
                    val index = y * size + x
                    if (index >= pixels.size) continue
                    val pixel = pixels[index]
                    val red = ((pixel shr 16) and 0xFF).toDouble()
                    val green = ((pixel shr 8) and 0xFF).toDouble()
                    val blue = (pixel and 0xFF).toDouble()
                    // The usual luminance weights, so brightness matches what an eye sees.
                    val luminance = 0.299 * red + 0.587 * green + 0.114 * blue
                    sums.count += 1
                    sums.red += red
                    sums.green += green
                    sums.blue += blue
                    sums.luminance += luminance
                    sums.luminanceSquared += luminance * luminance
                }
            }
        }

        val count = if (sums.count == 0.0) 1.0 else sums.count
        cell.red = sums.red / count
        cell.green = sums.green / count
        cell.blue = sums.blue / count
        // The variance of the brightness: the mean of the squares less the square of the
        // mean, which is what says how much is going on inside this cell.
        cell.detail =
            maxOf(
                0.0,
                sums.luminanceSquared / count - (sums.luminance / count) * (sums.luminance / count),
            )
        return sums
    }

    gather(root)
}

/**
 * Reorders the splits so the busiest parts of the picture come apart first.
 *
 * The times themselves are reused rather than recalculated, so only the order changes and
 * the pacing of the reveal stays exactly as it was built.
 *
 * @param branches every cell that has children.
 * @param openedBefore splits at or before this have already happened and are left alone.
 */
internal fun gridRevealOrderByDetail(
    branches: List<GridRevealCell>,
    openedBefore: Double,
) {
    val pending = branches.filter { it.splitAt > openedBefore }
    if (pending.size < 2) return

    val slots = pending.map { it.splitAt }.sorted()
    // Only cells whose parent has already come apart may go next, or a cell would be told
    // to split before it exists.
    val queue =
        pending
            .filter { cell -> cell.parent.let { it == null || it.splitAt <= openedBefore } }
            .toMutableList()

    var next = 0
    while (queue.isNotEmpty() && next < slots.size) {
        var pick = 0
        for (index in 1 until queue.size) {
            if (queue[index].detail > queue[pick].detail) pick = index
        }
        val cell = queue.removeAt(pick)
        cell.splitAt = slots[next]
        next++

        cell.first?.let { if (it.hasChildren) queue += it }
        cell.second?.let { if (it.hasChildren) queue += it }
    }
}

/**
 * The grey a cell shows before its colour arrives.
 *
 * It breathes: the sine on the clock makes the placeholder shift very slightly, which is
 * what stops a grid of flat rectangles reading as a broken image.
 *
 * @param tone the cell's own number.
 * @param dark whether the appearance is dark.
 * @param clock seconds since the reveal started.
 * @return a grey level, `0..255`.
 */
internal fun gridRevealGrey(
    tone: Double,
    dark: Boolean,
    clock: Double,
): Double = (if (dark) 30.0 else 228.0) + tone * 13 + sin(clock * 1.5 + tone * 6.28) * 3

/**
 * How far a reveal with no progress of its own has got.
 *
 * It approaches its ceiling without ever arriving, so a load that takes longer than its
 * estimate keeps creeping rather than stopping and waiting.
 *
 * @param elapsed seconds since it started.
 * @param duration the estimate, in seconds.
 * @return progress, below [GRID_REVEAL_HOLD].
 */
internal fun gridRevealSelfPaced(
    elapsed: Double,
    duration: Double,
): Double {
    val span = if (duration > 0) duration else 1.0
    return GRID_REVEAL_HOLD * (1 - exp(-elapsed / span))
}

/**
 * The smooth step upstream uses to fade the gutters and bring the photo in.
 *
 * @param from where the transition starts.
 * @param to where it finishes.
 * @param value the value being tested.
 * @return a value in `0..1`, easing at both ends.
 */
internal fun gridRevealSmoothstep(
    from: Double,
    to: Double,
    value: Double,
): Double {
    val t = ((value - from) / (to - from)).coerceIn(0.0, 1.0)
    return t * t * (3 - 2 * t)
}
