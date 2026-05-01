@file:Suppress("unused")

package com.eterultimate.eteruee.ui.components.table

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eterultimate.eteruee.ui.components.richtext.MarkdownBlock
import kotlin.math.max

/**
 * DataTable锛堣嚜瀹氫箟甯冨眬 + 妯悜婊氬姩 + 琛屽唴绛夐珮锛?
 * - 浣跨敤 SubcomposeLayout 涓ら樁娈垫祴閲忥紝閬垮厤 Lookahead 涓嬬殑閲嶅娴嬮噺寮傚父
 * - 楂樺害鑷€傚簲鍐呭锛堜笉鎻愪緵绾靛悜婊氬姩锛?
 * - 瀹藉害鍙秴鍑鸿鍙ｏ紝澶栧眰鍐呯疆 horizontalScroll
 */
@Composable
fun DataTable(
    headers: List<@Composable () -> Unit>,
    rows: List<List<@Composable () -> Unit>>,
    modifier: Modifier = Modifier,
    cellPadding: Dp = 4.dp,
    cellBorder: BorderStroke? = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
    headerBackground: Color = MaterialTheme.colorScheme.surfaceVariant,
    zebraStriping: Boolean = false,
    columnMinWidths: List<Dp> = emptyList(),
    columnMaxWidths: List<Dp> = emptyList(),
    cellAlignment: Alignment = Alignment.CenterStart,
) {
    val hScroll = rememberScrollState()
    val surfaceContainer = MaterialTheme.colorScheme.surfaceContainer

    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), MaterialTheme.shapes.small)
            .horizontalScroll(hScroll)
    ) {
        SubcomposeLayout { constraints ->
            val columnCount = max(headers.size, rows.maxOfOrNull { it.size } ?: 0)
            val rowCount = rows.size
            if (columnCount == 0) return@SubcomposeLayout layout(0, 0) {}

            // ---------- 鍙傛暟 & 涓棿缁撴灉瀹瑰櫒 ----------
            val infinity = Constraints.Infinity
            val unbounded = Constraints(0, infinity, 0, infinity)
            val minWidthsPx = IntArray(columnCount) { i -> columnMinWidths.getOrNull(i)?.roundToPx() ?: 0 }
            val maxWidthsPx = IntArray(columnCount) { i -> columnMaxWidths.getOrNull(i)?.roundToPx() ?: Int.MAX_VALUE }
            val colWidths = IntArray(columnCount) { 0 }
            val headerP1 = arrayOfNulls<Placeable>(columnCount)
            val bodyP1 = arrayOfNulls<Placeable>(rowCount * columnCount)

            // ---------- 绗竴闃舵锛氳嚜鐒跺昂瀵告祴閲忥紙浼板垪瀹姐€佺畻琛岄珮锛?----------
            fun subcomposeHeaderOnce(c: Int): Placeable {
                val measurables = subcompose("h1_$c") {
                    CellBox(
                        padding = cellPadding,
                        border = cellBorder,
                        background = headerBackground,
                        alignment = cellAlignment
                    ) {
                        headers.getOrNull(c)?.invoke()
                    }
                }
                val constraints = if (maxWidthsPx[c] != Int.MAX_VALUE) {
                    Constraints(0, maxWidthsPx[c], 0, infinity)
                } else {
                    unbounded
                }
                val p = measurables.first().measure(constraints)
                colWidths[c] = max(colWidths[c], max(p.width, minWidthsPx[c])).coerceAtMost(maxWidthsPx[c])
                return p
            }

            fun subcomposeBodyOnce(r: Int, c: Int): Placeable {
                val bg = if (zebraStriping && r % 2 == 1) surfaceContainer else Color.Transparent
                val measurables = subcompose("b1_${r}_$c") {
                    CellBox(padding = cellPadding, border = cellBorder, background = bg, alignment = cellAlignment) {
                        rows[r].getOrNull(c)?.invoke()
                    }
                }
                val constraints = if (maxWidthsPx[c] != Int.MAX_VALUE) {
                    Constraints(0, maxWidthsPx[c], 0, infinity)
                } else {
                    unbounded
                }
                val p = measurables.first().measure(constraints)
                colWidths[c] = max(colWidths[c], max(p.width, minWidthsPx[c])).coerceAtMost(maxWidthsPx[c])
                return p
            }

            for (c in 0 until columnCount) headerP1[c] = subcomposeHeaderOnce(c)
            for (r in 0 until rowCount) for (c in 0 until columnCount) bodyP1[r * columnCount + c] =
                subcomposeBodyOnce(r, c)

            val rowHeights = IntArray(rowCount) { r ->
                var h = 0
                for (c in 0 until columnCount) {
                    h = max(h, bodyP1[r * columnCount + c]!!.height)
                }
                h
            }
            val headerHeight = headerP1.maxOf { it?.height ?: 0 }

            // ---------- 绗簩闃舵锛氬浐瀹氬垪瀹?+ 缁熶竴琛岄珮閲嶆柊娴嬮噺 ----------
            fun constraintsFor(colWidth: Int, minH: Int): Constraints {
                val safeColWidth = colWidth.coerceAtLeast(0)
                val safeMinH = minH.coerceAtLeast(0)
                return Constraints(
                    minWidth = safeColWidth,
                    maxWidth = safeColWidth,
                    minHeight = safeMinH,
                    maxHeight = infinity,
                )
            }

            val headerPlaceables = Array(columnCount) { c ->
                val measurables = subcompose("h2_$c") {
                    CellBox(
                        padding = cellPadding,
                        border = cellBorder,
                        background = headerBackground,
                        alignment = cellAlignment
                    ) {
                        headers.getOrNull(c)?.invoke()
                    }
                }
                measurables.first().measure(constraintsFor(colWidths[c], headerHeight))
            }

            val bodyPlaceables = Array(rowCount * columnCount) { i ->
                val r = i / columnCount
                val c = i % columnCount
                val bg =
                    if (zebraStriping && r % 2 == 1) surfaceContainer else Color.Transparent
                val measurables = subcompose("b2_${r}_$c") {
                    CellBox(padding = cellPadding, border = cellBorder, background = bg, alignment = cellAlignment) {
                        rows[r].getOrNull(c)?.invoke()
                    }
                }
                measurables.first().measure(constraintsFor(colWidths[c], rowHeights[r]))
            }

            val tableWidth = colWidths.sum()
            val tableHeight = headerHeight + rowHeights.sum()
            val finalWidth = tableWidth.coerceIn(constraints.minWidth, constraints.maxWidth)
            val finalHeight = tableHeight.coerceIn(constraints.minHeight, constraints.maxHeight)

            // ---------- 鏀剧疆 ----------
            layout(finalWidth, finalHeight) {
                var x = 0
                for (c in 0 until columnCount) {
                    headerPlaceables[c].placeRelative(x, 0)
                    x += colWidths[c]
                }
                var y = headerHeight
                for (r in 0 until rowCount) {
                    x = 0
                    for (c in 0 until columnCount) {
                        bodyPlaceables[r * columnCount + c].placeRelative(x, y)
                        x += colWidths[c]
                    }
                    y += rowHeights[r]
                }
            }
        }
    }
}

@Composable
private fun CellBox(
    padding: Dp,
    border: BorderStroke?,
    background: Color,
    alignment: Alignment,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .then(if (background != Color.Transparent) Modifier.background(background) else Modifier)
            .then(if (border != null) Modifier.border(border) else Modifier)
            .padding(padding),
        contentAlignment = alignment,
    ) {
        content()
    }
}

// -------------------- 绀轰緥 --------------------
@Preview(showBackground = true)
@Composable
private fun DataTablePreview() {
    Surface {
        val headers = listOf<@Composable () -> Unit>(
            { Text("Semester", style = MaterialTheme.typography.labelLarge) },
            { Text("Attendance", style = MaterialTheme.typography.labelLarge) },
            { Text("Notes / Example", style = MaterialTheme.typography.labelLarge) },
        )

        val rows = listOf<List<@Composable () -> Unit>>(
            listOf<@Composable () -> Unit>(
                { Text("Fall 2024") },
                { Text("Excellent", style = MaterialTheme.typography.bodyMedium) },
                { Text("x虏 + y虏 = 1") },
            ),
            listOf(
                { Text("Fall 2024") },
                { Text("Good", style = MaterialTheme.typography.bodyMedium) },
                { Text("鈭?k = n(n+1)/2", maxLines = 2, overflow = TextOverflow.Ellipsis) },
            ),
            listOf(
                { Text("Fall 2024") },
                { Text("Fair", style = MaterialTheme.typography.bodyMedium) },
                { MarkdownBlock("杩欒鏇撮珮浼氭妸鏁磋鎷夐綈! 杩欐槸涓€涓緢闀跨殑鏂囨湰鐢ㄦ潵娴嬭瘯鎹㈣鍔熻兘!  \n>haha") },
            ),
        )

        DataTable(
            headers = headers,
            rows = rows,
            columnMinWidths = listOf(60.dp, 100.dp, 80.dp),
            columnMaxWidths = listOf(120.dp, 100.dp, 200.dp),
            zebraStriping = false,
        )
    }
}

