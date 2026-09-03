package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.util.Log
import androidx.core.content.FileProvider
import com.example.data.SensorReadingEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfReportGenerator {

    private const val TAG = "PdfReportGenerator"

    fun generateSensorPdfReport(
        context: Context,
        readings: List<SensorReadingEntity>,
        isFirestoreDataSource: Boolean = true
    ): Pair<File, Uri>? {
        if (readings.isEmpty()) {
            Log.w(TAG, "No sensor readings provided to export")
            return null
        }

        val pdfDocument = PdfDocument()
        val pageWidth = 595 // Standard A4 width in points
        val pageHeight = 842 // Standard A4 height in points

        // Paints for styling
        val titlePaint = Paint().apply {
            color = Color.parseColor("#1B5E20") // Dark Green
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val headerBannerPaint = Paint().apply {
            color = Color.parseColor("#E8F5E9") // Very light green banner
            style = Paint.Style.FILL
        }

        val subtitlePaint = Paint().apply {
            color = Color.parseColor("#388E3C")
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val metaPaint = Paint().apply {
            color = Color.parseColor("#555555")
            textSize = 9f
            isAntiAlias = true
        }

        val cardBgPaint = Paint().apply {
            color = Color.parseColor("#F5F5F5")
            style = Paint.Style.FILL
        }

        val cardBorderPaint = Paint().apply {
            color = Color.parseColor("#E0E0E0")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }

        val tableHeaderBgPaint = Paint().apply {
            color = Color.parseColor("#2E7D32")
            style = Paint.Style.FILL
        }

        val tableHeaderTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        val tableRowEvenBg = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }

        val tableRowOddBg = Paint().apply {
            color = Color.parseColor("#FAFAFA")
            style = Paint.Style.FILL
        }

        val tableCellTextPaint = Paint().apply {
            color = Color.parseColor("#212121")
            textSize = 9f
            isAntiAlias = true
        }

        val footerTextPaint = Paint().apply {
            color = Color.parseColor("#757575")
            textSize = 8f
            isAntiAlias = true
        }

        val linePaint = Paint().apply {
            color = Color.parseColor("#E0E0E0")
            strokeWidth = 0.5f
        }

        // Summary Statistics Calculations
        val avgTemp = readings.map { it.temperatureC }.average().toFloat()
        val minTemp = readings.minOf { it.temperatureC }
        val maxTemp = readings.maxOf { it.temperatureC }

        val avgHum = readings.map { it.humidityPercent }.average().toFloat()
        val minHum = readings.minOf { it.humidityPercent }
        val maxHum = readings.maxOf { it.humidityPercent }

        val avgCo2 = readings.map { it.co2Ppm }.average().toFloat()
        val minCo2 = readings.minOf { it.co2Ppm }
        val maxCo2 = readings.maxOf { it.co2Ppm }

        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val exportTimestamp = sdf.format(Date())

        var pageNumber = 1
        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = pdfDocument.startPage(pageInfo)
        var canvas = page.canvas

        fun drawHeaderAndSummary(c: Canvas) {
            // Top Green Banner
            c.drawRect(0f, 0f, pageWidth.toFloat(), 75f, headerBannerPaint)

            c.drawText("EcoMind Environmental Sensor History", 25f, 32f, titlePaint)
            val sourceText = if (isFirestoreDataSource) "Source: Cloud Firestore Database" else "Source: Local Room Synced Cache"
            c.drawText("Telemetry Report | $sourceText", 25f, 48f, subtitlePaint)
            c.drawText("Exported On: $exportTimestamp | Total Records: ${readings.size}", 25f, 64f, metaPaint)

            // KPI Cards Block (drawn only on Page 1)
            val cardY = 88f
            val cardHeight = 55f
            val cardWidth = (pageWidth - 50f - 20f) / 3f // 3 equal cards

            // Card 1: Temperature
            c.drawRoundRect(25f, cardY, 25f + cardWidth, cardY + cardHeight, 6f, 6f, cardBgPaint)
            c.drawRoundRect(25f, cardY, 25f + cardWidth, cardY + cardHeight, 6f, 6f, cardBorderPaint)
            c.drawText("TEMPERATURE (°C)", 33f, cardY + 16f, subtitlePaint)
            c.drawText("Avg: %.1f°C".format(avgTemp), 33f, cardY + 32f, titlePaint.apply { textSize = 13f })
            c.drawText("Min: %.1f°C | Max: %.1f°C".format(minTemp, maxTemp), 33f, cardY + 46f, metaPaint)

            // Card 2: Humidity
            val c2X = 25f + cardWidth + 10f
            c.drawRoundRect(c2X, cardY, c2X + cardWidth, cardY + cardHeight, 6f, 6f, cardBgPaint)
            c.drawRoundRect(c2X, cardY, c2X + cardWidth, cardY + cardHeight, 6f, 6f, cardBorderPaint)
            c.drawText("HUMIDITY (%)", c2X + 8f, cardY + 16f, subtitlePaint)
            c.drawText("Avg: %.1f%%".format(avgHum), c2X + 8f, cardY + 32f, titlePaint)
            c.drawText("Min: %.1f%% | Max: %.1f%%".format(minHum, maxHum), c2X + 8f, cardY + 46f, metaPaint)

            // Card 3: CO2 Level
            val c3X = c2X + cardWidth + 10f
            c.drawRoundRect(c3X, cardY, c3X + cardWidth, cardY + cardHeight, 6f, 6f, cardBgPaint)
            c.drawRoundRect(c3X, cardY, c3X + cardWidth, cardY + cardHeight, 6f, 6f, cardBorderPaint)
            c.drawText("CO2 AIR QUALITY (PPM)", c3X + 8f, cardY + 16f, subtitlePaint)
            c.drawText("Avg: %d PPM".format(avgCo2.toInt()), c3X + 8f, cardY + 32f, titlePaint)
            c.drawText("Min: %d | Max: %d PPM".format(minCo2.toInt(), maxCo2.toInt()), c3X + 8f, cardY + 46f, metaPaint)
        }

        fun drawTableHeader(c: Canvas, startY: Float): Float {
            c.drawRect(25f, startY, pageWidth - 25f, startY + 22f, tableHeaderBgPaint)

            val yText = startY + 15f
            c.drawText("#", 30f, yText, tableHeaderTextPaint)
            c.drawText("Timestamp", 60f, yText, tableHeaderTextPaint)
            c.drawText("Device Node", 190f, yText, tableHeaderTextPaint)
            c.drawText("Temp (°C)", 350f, yText, tableHeaderTextPaint)
            c.drawText("Hum (%)", 420f, yText, tableHeaderTextPaint)
            c.drawText("CO2 (PPM)", 485f, yText, tableHeaderTextPaint)

            return startY + 22f
        }

        fun drawFooter(c: Canvas, pageNum: Int) {
            c.drawLine(25f, pageHeight - 35f, pageWidth - 25f, pageHeight - 35f, linePaint)
            c.drawText("EcoMind Analytics • Environmental Telemetry Report • Page $pageNum", 25f, pageHeight - 20f, footerTextPaint)
            c.drawText("Confidential & Proprietary", pageWidth - 140f, pageHeight - 20f, footerTextPaint)
        }

        // Draw page 1 header and summary
        drawHeaderAndSummary(canvas)
        var currentY = drawTableHeader(canvas, 155f)

        val rowHeight = 20f
        val maxY = pageHeight - 45f

        readings.forEachIndexed { index, reading ->
            if (currentY + rowHeight > maxY) {
                // Finish current page
                drawFooter(canvas, pageNumber)
                pdfDocument.finishPage(page)

                // Start new page
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas

                // Header for subsequent page
                canvas.drawRect(0f, 0f, pageWidth.toFloat(), 45f, headerBannerPaint)
                canvas.drawText("EcoMind Environmental Sensor History (Cont.)", 25f, 28f, titlePaint.apply { textSize = 14f })
                currentY = drawTableHeader(canvas, 55f)
            }

            // Draw Row Background
            val rowBg = if (index % 2 == 0) tableRowEvenBg else tableRowOddBg
            canvas.drawRect(25f, currentY, pageWidth - 25f, currentY + rowHeight, rowBg)
            canvas.drawLine(25f, currentY + rowHeight, pageWidth - 25f, currentY + rowHeight, linePaint)

            val yText = currentY + 14f
            val formattedTime = sdf.format(Date(reading.timestamp))

            canvas.drawText("${index + 1}", 30f, yText, tableCellTextPaint)
            canvas.drawText(formattedTime, 60f, yText, tableCellTextPaint)

            val shortenedDevice = if (reading.deviceName.length > 22) reading.deviceName.take(20) + ".." else reading.deviceName
            canvas.drawText(shortenedDevice, 190f, yText, tableCellTextPaint)

            canvas.drawText("%.1f".format(reading.temperatureC), 350f, yText, tableCellTextPaint)
            canvas.drawText("%.1f".format(reading.humidityPercent), 420f, yText, tableCellTextPaint)

            val co2Color = when {
                reading.co2Ppm > 1000f -> Color.parseColor("#D32F2F")
                reading.co2Ppm > 800f -> Color.parseColor("#F57C00")
                else -> Color.parseColor("#2E7D32")
            }
            val co2Paint = Paint(tableCellTextPaint).apply { color = co2Color; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
            canvas.drawText("%d".format(reading.co2Ppm.toInt()), 485f, yText, co2Paint)

            currentY += rowHeight
        }

        // Draw footer on last page
        drawFooter(canvas, pageNumber)
        pdfDocument.finishPage(page)

        // Save PDF to cache dir
        return try {
            val reportsDir = File(context.cacheDir, "pdf_reports")
            if (!reportsDir.exists()) {
                reportsDir.mkdirs()
            }

            val timestampStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val pdfFile = File(reportsDir, "ecomind_sensor_telemetry_$timestampStr.pdf")

            FileOutputStream(pdfFile).use { out ->
                pdfDocument.writeTo(out)
            }
            pdfDocument.close()

            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, pdfFile)

            Log.d(TAG, "PDF generated successfully at: ${pdfFile.absolutePath}")
            Pair(pdfFile, uri)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating or saving PDF report", e)
            pdfDocument.close()
            null
        }
    }

    fun sharePdfReportViaIntent(
        context: Context,
        fileUri: Uri,
        file: File,
        subject: String = "EcoMind Environmental Telemetry Report",
        bodyText: String? = null
    ) {
        val defaultBody = bodyText ?: "Attached is the exported environmental sensor telemetry report generated by EcoMind.\n\n" +
                "• File Name: ${file.name}\n" +
                "• File Size: ${file.length() / 1024} KB\n" +
                "• Export Date: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n\n" +
                "Sent via EcoMind Environmental Sensor System."

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, fileUri)
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, defaultBody)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(shareIntent, "Share PDF Report via Email or Messaging")
        chooser.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        try {
            context.startActivity(chooser)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch intent chooser for sharing PDF", e)
        }
    }

    fun openOrSharePdf(context: Context, fileUri: Uri, file: File) {
        sharePdfReportViaIntent(context, fileUri, file)
    }
}
