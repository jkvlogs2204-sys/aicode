package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Co2
import androidx.compose.material.icons.filled.DeviceThermostat
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bluetooth.ble.BleSensorData
import com.example.ui.SensorTelemetryPoint
import com.example.ui.theme.EcoBadgeBad
import com.example.ui.theme.EcoBadgeGood
import com.example.ui.theme.EcoBadgeWarning
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import com.example.data.SensorReadingEntity

enum class TelemetryMetric {
    TEMPERATURE,
    HUMIDITY,
    CO2
}

enum class TelemetrySource {
    LIVE_BUFFER,
    ROOM_DATABASE
}

@Composable
fun RealtimeSensorStreamCard(
    sensorData: BleSensorData?,
    telemetryHistory: List<SensorTelemetryPoint>,
    roomSensorHistory: List<SensorReadingEntity>,
    rawSerialData: String,
    onSaveSnapshotToRoom: () -> Unit,
    onClearRoomHistory: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedMetric by remember { mutableStateOf(TelemetryMetric.TEMPERATURE) }
    var selectedSource by remember { mutableStateOf(TelemetrySource.ROOM_DATABASE) }

    val currentTemp = sensorData?.temperatureC
        ?: telemetryHistory.lastOrNull()?.tempC
        ?: roomSensorHistory.firstOrNull()?.temperatureC
        ?: 24.2f

    val currentHum = sensorData?.humidityPercent
        ?: telemetryHistory.lastOrNull()?.humidityPercent
        ?: roomSensorHistory.firstOrNull()?.humidityPercent
        ?: 50.0f

    val currentCo2 = telemetryHistory.lastOrNull()?.co2Ppm
        ?: roomSensorHistory.firstOrNull()?.co2Ppm
        ?: 412f

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier.fillMaxWidth().testTag("realtime_sensor_stream_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sensors,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Arduino Environmental Sensor Stream",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (sensorData != null || rawSerialData.isNotBlank()) "Hardware Telemetry Live (Room DB Synced)" else "Local Room DB Trends & Live Hardware Stream",
                            fontSize = 10.sp,
                            color = if (sensorData != null || rawSerialData.isNotBlank()) EcoBadgeGood else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = EcoBadgeGood.copy(alpha = 0.15f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(EcoBadgeGood)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "ROOM DB",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = EcoBadgeGood
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Source Selector Row (Room DB Local History vs Live Buffer)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = selectedSource == TelemetrySource.ROOM_DATABASE,
                        onClick = { selectedSource = TelemetrySource.ROOM_DATABASE },
                        label = { Text("Room DB Trends (${roomSensorHistory.size})", fontSize = 11.sp) },
                        leadingIcon = { Icon(imageVector = Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        modifier = Modifier.testTag("filter_room_db_history")
                    )

                    FilterChip(
                        selected = selectedSource == TelemetrySource.LIVE_BUFFER,
                        onClick = { selectedSource = TelemetrySource.LIVE_BUFFER },
                        label = { Text("Live Stream", fontSize = 11.sp) },
                        modifier = Modifier.testTag("filter_live_stream_buffer")
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    AssistChip(
                        onClick = onSaveSnapshotToRoom,
                        label = { Text("Save", fontSize = 10.sp) },
                        leadingIcon = { Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(12.dp)) },
                        modifier = Modifier.testTag("btn_save_sensor_snapshot")
                    )

                    if (roomSensorHistory.isNotEmpty()) {
                        AssistChip(
                            onClick = onClearRoomHistory,
                            label = { Text("Clear", fontSize = 10.sp) },
                            leadingIcon = { Icon(imageVector = Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(12.dp)) },
                            colors = AssistChipDefaults.assistChipColors(labelColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.testTag("btn_clear_room_sensor_db")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Real-Time Gauges Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SensorGaugePill(
                    icon = Icons.Default.DeviceThermostat,
                    title = "Temperature",
                    value = String.format(Locale.US, "%.1f°C", currentTemp),
                    progress = (currentTemp / 50f).coerceIn(0f, 1f),
                    color = if (currentTemp > 30f) EcoBadgeBad else MaterialTheme.colorScheme.primary,
                    isSelected = selectedMetric == TelemetryMetric.TEMPERATURE,
                    onClick = { selectedMetric = TelemetryMetric.TEMPERATURE },
                    modifier = Modifier.weight(1f)
                )

                SensorGaugePill(
                    icon = Icons.Default.WaterDrop,
                    title = "Humidity",
                    value = String.format(Locale.US, "%.0f%%", currentHum),
                    progress = (currentHum / 100f).coerceIn(0f, 1f),
                    color = EcoBadgeGood,
                    isSelected = selectedMetric == TelemetryMetric.HUMIDITY,
                    onClick = { selectedMetric = TelemetryMetric.HUMIDITY },
                    modifier = Modifier.weight(1f)
                )

                SensorGaugePill(
                    icon = Icons.Default.Co2,
                    title = "Air Quality / CO₂",
                    value = String.format(Locale.US, "%.0f ppm", currentCo2),
                    progress = (currentCo2 / 1000f).coerceIn(0f, 1f),
                    color = if (currentCo2 > 800f) EcoBadgeBad else EcoBadgeWarning,
                    isSelected = selectedMetric == TelemetryMetric.CO2,
                    onClick = { selectedMetric = TelemetryMetric.CO2 },
                    modifier = Modifier.weight(1.1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Dynamic Canvas Line Chart based on selected Source (Room DB vs Live Stream)
            val metricName = when (selectedMetric) {
                TelemetryMetric.TEMPERATURE -> "Temperature (°C)"
                TelemetryMetric.HUMIDITY -> "Humidity (%RH)"
                TelemetryMetric.CO2 -> "CO₂ (PPM)"
            }

            val sourceName = if (selectedSource == TelemetrySource.ROOM_DATABASE) "Room Local SQLite Database" else "Live Hardware Buffer"
            val chartTitle = "$metricName Trends [$sourceName]"

            val chartColor = when (selectedMetric) {
                TelemetryMetric.TEMPERATURE -> MaterialTheme.colorScheme.primary
                TelemetryMetric.HUMIDITY -> EcoBadgeGood
                TelemetryMetric.CO2 -> EcoBadgeWarning
            }

            val points = if (selectedSource == TelemetrySource.ROOM_DATABASE) {
                roomSensorHistory.reversed().map { item ->
                    when (selectedMetric) {
                        TelemetryMetric.TEMPERATURE -> item.temperatureC
                        TelemetryMetric.HUMIDITY -> item.humidityPercent
                        TelemetryMetric.CO2 -> item.co2Ppm
                    }
                }
            } else {
                telemetryHistory.map { pt ->
                    when (selectedMetric) {
                        TelemetryMetric.TEMPERATURE -> pt.tempC
                        TelemetryMetric.HUMIDITY -> pt.humidityPercent
                        TelemetryMetric.CO2 -> pt.co2Ppm
                    }
                }
            }

            RealtimeLineChartCanvas(
                title = chartTitle,
                points = points,
                lineColor = chartColor,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
            )

            if (rawSerialData.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Raw Stream: $rawSerialData",
                        fontSize = 10.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SensorGaugePill(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    progress: Float,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "gauge_progress"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .border(
                width = if (isSelected) 2.dp else 0.5.dp,
                color = if (isSelected) color else Color.Transparent,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = title, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f), maxLines = 1)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)

            Spacer(modifier = Modifier.height(6.dp))

            // Mini Gauge Arc / Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.Gray.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(color)
                )
            }
        }
    }
}

@Composable
fun RealtimeLineChartCanvas(
    title: String,
    points: List<Float>,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    val minVal = (points.minOrNull() ?: 0f) - 1f
    val maxVal = (points.maxOrNull() ?: 100f) + 1f
    val range = (maxVal - minVal).coerceAtLeast(1f)

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = title, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(
                text = "Min: ${String.format(Locale.US, "%.1f", minVal + 1f)} | Max: ${String.format(Locale.US, "%.1f", maxVal - 1f)}",
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Grid Lines
            val gridCount = 3
            for (i in 0..gridCount) {
                val y = height * (i.toFloat() / gridCount)
                drawLine(
                    color = Color.Gray.copy(alpha = 0.15f),
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            if (points.size < 2) return@Canvas

            val dx = width / (points.size - 1)
            val path = Path()
            val fillPath = Path()

            points.forEachIndexed { index, value ->
                val x = index * dx
                val normalizedY = (value - minVal) / range
                val y = height - (normalizedY * height)

                if (index == 0) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, height)
                    fillPath.lineTo(x, y)
                } else {
                    val prevX = (index - 1) * dx
                    val prevVal = points[index - 1]
                    val prevY = height - (((prevVal - minVal) / range) * height)

                    // Smooth Bezier Curve
                    val controlX1 = prevX + (dx / 2)
                    val controlY1 = prevY
                    val controlX2 = prevX + (dx / 2)
                    val controlY2 = y

                    path.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
                    fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y)
                }

                // Draw Data Point Dots
                drawCircle(
                    color = lineColor,
                    radius = 3.dp.toPx(),
                    center = Offset(x, y)
                )
            }

            fillPath.lineTo(width, height)
            fillPath.close()

            // Fill Gradient Under Line
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.3f), lineColor.copy(alpha = 0.0f))
                )
            )

            // Stroke Chart Line
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )
        }
    }
}
