package com.ling.astrbot.core.designsystem.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.CornerRounding
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.circle
import androidx.graphics.shapes.rectangle
import androidx.graphics.shapes.star

/**
 * M3 Expressive 风格的加载指示器：在若干圆角多边形之间连续形变并旋转。
 */
@Composable
fun ExpressiveLoadingIndicator(
    modifier: Modifier = Modifier,
    diameter: Dp = 34.dp,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val shapes = remember {
        listOf(
            RoundedPolygon.circle(numVertices = 7, radius = 1f),
            RoundedPolygon.star(
                numVerticesPerRadius = 7,
                radius = 1f,
                innerRadius = 0.66f,
                rounding = CornerRounding(radius = 0.14f),
            ),
            RoundedPolygon.rectangle(
                width = 1.6f,
                height = 1.6f,
                rounding = CornerRounding(radius = 0.42f),
            ),
            RoundedPolygon.star(
                numVerticesPerRadius = 4,
                radius = 1f,
                innerRadius = 0.58f,
                rounding = CornerRounding(radius = 0.18f),
            ),
        )
    }

    val transition = rememberInfiniteTransition(label = "expressive-loading")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1700, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "progress",
    )

    val count = shapes.size
    val scaled = progress * count
    val index = scaled.toInt() % count
    val local = scaled - scaled.toInt()
    val eased = FastOutSlowInEasing.transform(local)

    val morph = remember(index) { Morph(shapes[index], shapes[(index + 1) % count]) }
    val path = remember { Path() }

    Canvas(modifier = modifier.size(diameter)) {
        // 用 Morph 的 cubics 构建 Compose Path（库的 toPath 返回的是 android.graphics.Path）。
        path.rewind()
        morph.asCubics(eased).forEachIndexed { cubicIndex, cubic ->
            if (cubicIndex == 0) {
                path.moveTo(cubic.anchor0X, cubic.anchor0Y)
            }
            path.cubicTo(
                cubic.control0X,
                cubic.control0Y,
                cubic.control1X,
                cubic.control1Y,
                cubic.anchor1X,
                cubic.anchor1Y,
            )
        }

        val centerOffset = center
        val radius = size.minDimension / 2f * 0.6f
        withTransform({
            translate(centerOffset.x, centerOffset.y)
            rotate(progress * 360f, pivot = Offset.Zero)
            scale(radius, radius, pivot = Offset.Zero)
        }) {
            drawPath(path, color)
        }
    }
}
