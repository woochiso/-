package com.example.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.data.remote.dto.EmotionGraphResponse
import java.io.File
import java.io.FileOutputStream
import kotlin.math.cos
import kotlin.math.sin

/** 화면과 동일한 서버 집계 결과로 5페이지 감정그래프 PDF를 만든다. */
object EmotionGraphPdfExporter {
    private const val PAGE_WIDTH = 1240
    private const val PAGE_HEIGHT = 1754

    fun save(context: Context, data: EmotionGraphResponse, rangeLabel: String): Boolean = runCatching {
        val document = PdfDocument()
        addSummaryPage(document, data, rangeLabel)
        addBubblePage(document, data, rangeLabel)
        addStoryPage(document, data, rangeLabel)
        addTimePage(document, data, rangeLabel)
        addDetailPage(document, data, rangeLabel)
        val name = "Woochiso_EmotionGraph_${System.currentTimeMillis()}.pdf"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name)
                put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/Woochiso")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                ?: error("MediaStore insert failed")
            context.contentResolver.openOutputStream(uri)?.use(document::writeTo) ?: error("Output stream unavailable")
            context.contentResolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
        } else {
            val directory = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Woochiso").apply { mkdirs() }
            FileOutputStream(File(directory, name)).use(document::writeTo)
        }
        document.close()
        true
    }.getOrDefault(false)

    private fun addSummaryPage(pdf: PdfDocument, data: EmotionGraphResponse, range: String) = page(pdf, 1, "감정그래프", range) { canvas, paint ->
        val summary = data.summary
        val recorded = summary?.recordedEmotions ?: data.emotions.count { it.totalCount > 0 }
        val total = summary?.totalOccurrences ?: data.totalCount
        val top = summary?.topEmotion ?: data.emotions.maxByOrNull { it.totalCount }?.let { com.example.data.remote.dto.EmotionGraphTopEmotionDto(it.emotionId, it.emotionName, it.totalCount) }
        val values = listOf("기록한 세부감정" to "${recorded}개", "총 감정 발생" to "${total}회", "가장 많이 발생한 감정" to (top?.let { "${it.emotionName} ${it.totalCount}회" } ?: "-"))
        values.forEachIndexed { index, item ->
            val topY = 330f + index * 300f
            paint.color = Color.rgb(248, 249, 255); canvas.drawRoundRect(RectF(100f, topY, 1140f, topY + 220f), 34f, 34f, paint)
            paint.color = Color.DKGRAY; paint.textSize = 38f; canvas.drawText(item.first, 150f, topY + 80f, paint)
            paint.color = Color.rgb(91, 110, 245); paint.textSize = 54f; paint.isFakeBoldText = true; canvas.drawText(item.second, 150f, topY + 165f, paint); paint.isFakeBoldText = false
        }
    }

    private fun addBubblePage(pdf: PdfDocument, data: EmotionGraphResponse, range: String) = page(pdf, 2, "01  7감정 버블 그래프", range) { canvas, paint ->
        val max = data.categories.maxOfOrNull { it.totalCount }?.coerceAtLeast(1) ?: 1
        val centerX = PAGE_WIDTH / 2f; val centerY = 720f; val ring = 360f
        data.categories.forEachIndexed { index, item ->
            val angle = Math.toRadians((-90 + index * (360.0 / data.categories.size.coerceAtLeast(1))))
            val radius = 82f + 62f * item.totalCount / max.toFloat()
            val x = centerX + cos(angle).toFloat() * ring; val y = centerY + sin(angle).toFloat() * ring
            paint.color = parse(item.colorCode); canvas.drawCircle(x, y, radius, paint)
            paint.color = Color.WHITE; paint.textAlign = Paint.Align.CENTER; paint.isFakeBoldText = true; paint.textSize = 34f; canvas.drawText(item.categoryHanja, x, y - 20f, paint)
            paint.textSize = 25f; canvas.drawText(item.categoryName, x, y + 18f, paint); canvas.drawText("${item.totalCount}회", x, y + 52f, paint)
        }
        paint.textAlign = Paint.Align.LEFT; paint.isFakeBoldText = false
    }

    private fun addStoryPage(pdf: PdfDocument, data: EmotionGraphResponse, range: String) = page(pdf, 3, "02  사연별 비율", range) { canvas, paint ->
        val active = data.storyRatios.filter { it.totalCount > 0 }
        val oval = RectF(140f, 330f, 760f, 950f); var start = -90f
        active.forEach { item -> paint.color = parse(item.color); val sweep = item.percentage.toFloat() * 3.6f; canvas.drawArc(oval, start, sweep, true, paint); start += sweep }
        active.take(12).forEachIndexed { index, item ->
            val y = 1050f + index * 48f
            paint.color = parse(item.color); canvas.drawCircle(150f, y - 8f, 10f, paint)
            paint.color = Color.DKGRAY; paint.textSize = 27f; canvas.drawText(item.storyTitle.take(34), 180f, y, paint)
            paint.textAlign = Paint.Align.RIGHT; paint.isFakeBoldText = true; canvas.drawText(String.format(java.util.Locale.US, "%.1f%%", item.percentage), 1100f, y, paint); paint.textAlign = Paint.Align.LEFT; paint.isFakeBoldText = false
        }
    }

    private fun addTimePage(pdf: PdfDocument, data: EmotionGraphResponse, range: String) = page(pdf, 4, "03  시간별 그래프", range) { canvas, paint ->
        val graph = data.timeGraphs.firstOrNull { it.mode == "hour" } ?: data.timeGraphs.firstOrNull()
        if (graph == null || !graph.hasData) { empty(canvas, paint, "이 기간에는 시간별 감정 기록이 없습니다."); return@page }
        val left = 100f; val top = 340f; val width = 1040f; val height = 720f
        paint.strokeWidth = 2f; paint.color = Color.LTGRAY
        repeat(5) { i -> val y = top + height * i / 4f; canvas.drawLine(left, y, left + width, y, paint) }
        val max = graph.series.flatMap { it.values }.maxOrNull()?.coerceAtLeast(1) ?: 1
        graph.series.forEach { series ->
            paint.color = parse(series.color); paint.style = Paint.Style.STROKE; paint.strokeWidth = 5f
            val path = Path(); series.values.forEachIndexed { index, value -> val x = left + width * index / (graph.labels.size - 1).coerceAtLeast(1); val y = top + height * (1f - value.toFloat() / max); if (index == 0) path.moveTo(x, y) else path.lineTo(x, y) }; canvas.drawPath(path, paint)
        }
        paint.style = Paint.Style.FILL
    }

    private fun addDetailPage(pdf: PdfDocument, data: EmotionGraphResponse, range: String) = page(pdf, 5, "04  세부감정 그래프", range) { canvas, paint ->
        val max = data.emotions.maxOfOrNull { it.totalCount }?.coerceAtLeast(1) ?: 1
        data.emotions.take(18).forEachIndexed { index, item ->
            val y = 300f + index * 72f
            paint.color = Color.DKGRAY; paint.textSize = 25f; canvas.drawText("${index + 1}. ${item.emotionName}", 100f, y, paint)
            paint.color = parse(item.colorCode); canvas.drawRoundRect(RectF(410f, y - 27f, 410f + 620f * item.totalCount / max, y + 6f), 16f, 16f, paint)
            paint.color = Color.DKGRAY; paint.textAlign = Paint.Align.RIGHT; canvas.drawText("${item.totalCount}회", 1120f, y, paint); paint.textAlign = Paint.Align.LEFT
        }
    }

    private inline fun page(pdf: PdfDocument, number: Int, title: String, range: String, draw: (android.graphics.Canvas, Paint) -> Unit) {
        val page = pdf.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, number).create())
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = android.graphics.Typeface.create("sans", android.graphics.Typeface.NORMAL) }
        page.canvas.drawColor(Color.WHITE); paint.color = Color.rgb(91, 110, 245); paint.textSize = 30f; paint.isFakeBoldText = true; page.canvas.drawText("WOOCHISO", 80f, 90f, paint)
        paint.color = Color.rgb(25, 35, 65); paint.textSize = 48f; page.canvas.drawText(title, 80f, 175f, paint)
        paint.color = Color.GRAY; paint.textSize = 25f; paint.isFakeBoldText = false; page.canvas.drawText(range, 80f, 225f, paint)
        draw(page.canvas, paint); pdf.finishPage(page)
    }

    private fun empty(canvas: android.graphics.Canvas, paint: Paint, message: String) { paint.color = Color.GRAY; paint.textSize = 32f; canvas.drawText(message, 120f, 520f, paint) }
    private fun parse(value: String): Int = runCatching { Color.parseColor(value) }.getOrDefault(Color.rgb(116, 175, 221))
}
