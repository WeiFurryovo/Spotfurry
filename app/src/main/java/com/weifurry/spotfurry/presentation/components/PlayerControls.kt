package com.weifurry.spotfurry.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material3.Icon
import androidx.wear.compose.material3.Text

@Composable
internal fun StatusPill(
    text: String,
    active: Boolean
) {
    Box(
        modifier =
            Modifier
                .clip(RoundedCornerShape(50))
                .background(if (active) Color(0xFF142018) else Color(0xFF181818))
                .border(
                    width = 1.dp,
                    color = if (active) Color(0xFF24442E) else Color(0xFF2A2A2A),
                    shape = RoundedCornerShape(50)
                )
                .padding(horizontal = 9.dp, vertical = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 9.sp,
            color = if (active) Color(0xFF99F7B2) else Color(0xFF9A9A9A),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
internal fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val strokeWidth = 3.dp.toPx()
        val inset = strokeWidth / 2
        val arcSize =
            Size(
                width = size.width - strokeWidth,
                height = size.height - strokeWidth
            )
        val topLeft = Offset(inset, inset)

        drawArc(
            color = Color(0xFF252525),
            startAngle = -220f,
            sweepAngle = 260f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
        drawArc(
            color = Color(0xFF8CFFAF),
            startAngle = -220f,
            sweepAngle = 260f * progress.coerceIn(0f, 1f),
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

@Composable
internal fun PrimaryPlayerButton(
    icon: ImageVector,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 90.dp
) {
    Box(
        modifier =
            modifier
                .size(size)
                .clip(CircleShape)
                .background(Color(0xFF2B2B2B))
                .border(
                    width = 1.dp,
                    color = Color(0xFF373737),
                    shape = CircleShape
                )
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color(0xFFF0F0F0),
            modifier = Modifier.size(size * 0.42f)
        )
    }
}

@Composable
internal fun SecondaryPlayerButton(
    icon: ImageVector,
    onClick: () -> Unit,
    contentDescription: String,
    size: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier =
            modifier
                .size(size)
                .clip(CircleShape)
                .background(Color(0xFF1F1F1F))
                .border(
                    width = 1.dp,
                    color = Color(0xFF2B2B2B),
                    shape = CircleShape
                )
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = Color(0xFFD8D8D8),
            modifier = Modifier.size(size * 0.46f)
        )
    }
}

@Composable
internal fun SmallIconBubble(
    icon: ImageVector,
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    iconSize: Dp = 19.dp,
    highlighted: Boolean = false,
    bubbleColor: Color = Color(0xFF212121),
    borderColor: Color = Color(0xFF2D2D2D),
    iconTint: Color = Color(0xFFE5E5E5)
) {
    Box(
        modifier =
            modifier
                .size(size)
                .clip(CircleShape)
                .background(
                    if (highlighted) {
                        Color(0xFF303030)
                    } else {
                        bubbleColor
                    }
                )
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = CircleShape
                )
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(iconSize)
        )
    }
}
