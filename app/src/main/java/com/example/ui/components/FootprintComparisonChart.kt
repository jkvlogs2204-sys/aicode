package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.EcoBadgeBad
import com.example.ui.theme.EcoBadgeGood

@Composable
fun FootprintComparisonChart(
    productName: String,
    carbonText: String,
    waterText: String,
    ecoScore: Int,
    alternativeName: String,
    modifier: Modifier = Modifier
) {
    // Extract numerical rough values for bar width estimation
    val currentCarbonValue = parseValue(carbonText)
    val isGoodProduct = ecoScore >= 60

    val currentBarRatio = if (isGoodProduct) 0.35f else 0.85f
    val alternativeBarRatio = if (isGoodProduct) 0.20f else 0.25f

    val animCurrentRatio by animateFloatAsState(
        targetValue = currentBarRatio,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "current_bar"
    )

    val animAlternativeRatio by animateFloatAsState(
        targetValue = alternativeBarRatio,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "alt_bar"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp)
    ) {
        Text(
            text = "Carbon & Resource Intensity Comparison",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Scanned Item Bar
        BarRow(
            label = productName,
            valueText = "$carbonText | $waterText",
            ratio = animCurrentRatio,
            barColor = if (isGoodProduct) EcoBadgeGood else EcoBadgeBad
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Alternative Item Bar
        BarRow(
            label = "Alternative: $alternativeName",
            valueText = "Lower Footprint (~75% Less CO₂)",
            ratio = animAlternativeRatio,
            barColor = EcoBadgeGood
        )
    }
}

@Composable
private fun BarRow(
    label: String,
    valueText: String,
    ratio: Float,
    barColor: Color
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = valueText,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = barColor
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Color.Gray.copy(alpha = 0.2f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(ratio)
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(barColor)
            )
        }
    }
}

private fun parseValue(raw: String): Float {
    return try {
        raw.replace(Regex("[^0-9.]"), "").toFloatOrNull() ?: 50f
    } catch (e: Exception) {
        50f
    }
}
