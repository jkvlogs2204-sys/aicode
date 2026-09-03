package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EcoBadgeBad
import com.example.ui.theme.EcoBadgeGood
import com.example.ui.theme.EcoBadgeWarning

@Composable
fun EcoGaugeCanvas(
    score: Int,
    modifier: Modifier = Modifier,
    size: Dp = 160.dp
) {
    val animatedScore by animateFloatAsState(
        targetValue = score.coerceIn(0, 100).toFloat(),
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing),
        label = "score_anim"
    )

    val gaugeColor = when {
        score >= 60 -> EcoBadgeGood
        score >= 40 -> EcoBadgeWarning
        else -> EcoBadgeBad
    }

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            val strokeWidth = 16.dp.toPx()
            val arcSize = Size(size.toPx() - strokeWidth * 2, size.toPx() - strokeWidth * 2)
            val topLeft = Offset(strokeWidth, strokeWidth)

            // Background Arc (240 degrees sweep from 150 to 390)
            drawArc(
                color = Color.LightGray.copy(alpha = 0.3f),
                startAngle = 150f,
                sweepAngle = 240f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // Animated Value Arc
            val sweepAngle = (animatedScore / 100f) * 240f
            val gradientBrush = Brush.sweepGradient(
                listOf(EcoBadgeBad, EcoBadgeWarning, EcoBadgeGood, EcoBadgeGood)
            )

            drawArc(
                brush = gradientBrush,
                startAngle = 150f,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "${animatedScore.toInt()}",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = gaugeColor
            )
            Text(
                text = "ECO SCORE",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}
