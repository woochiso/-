package com.example.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.example.data.remote.dto.RecoveryInsightDataDto
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

/** 화면에 이미 표시된 서버 집계와 AI 결과만 사용한다. PDF 저장 시 서버/AI를 다시 호출하지 않는다. */
object AiEmotionAnalysisPdfExporter {
    private const val WIDTH = 1240
    private const val HEIGHT = 1754

    fun save(context: Context, data: RecoveryInsightDataDto, aiText: String?, nickname: String): Boolean = runCatching {
        val pdf = PdfDocument()
        val writer = Writer(pdf)
        writer.heading("WOOCHISO", 32f, Color.rgb(116, 175, 221))
        writer.heading("우치소 AI 감정분석 리포트", 48f, Color.rgb(25, 35, 65))
        writer.line("회원: $nickname")
        writer.line("분석기간: ${data.period.label} (${data.period.startDate} ~ ${data.period.endDate})")
        writer.line("저장일: ${SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(Date())}")
        writer.space(24f)

        writer.section("01 최근 회복활동")
        data.recentSessions.forEach { session ->
            writer.bold("${session.activityName} · ${session.recordedAt.take(16)}")
            session.changes.filter { it.delta != 0 }.take(7).forEach { writer.line("${it.categoryLabel} ${it.before} → ${it.after} (${signed(it.delta.toDouble())})") }
        }
        writer.section("02 활동 전·후 감정 변화")
        data.recentSessions.firstOrNull()?.changes?.forEach { writer.line("${it.categoryLabel}: ${it.before} → ${it.after}  ${signed(it.delta.toDouble())}") }
        writer.section("03 나에게 관찰된 회복 패턴")
        data.activityPatterns.forEach { pattern ->
            writer.bold("${pattern.activityName} · ${pattern.count}회 기록")
            pattern.emotionChanges.take(3).forEach { writer.line("${it.categoryLabel} 평균 ${signed(it.averageDelta)}, 같은 방향 ${it.sameDirectionCount}/${pattern.count}회 (${it.sameDirectionRatio}%)") }
            writer.line(if (!pattern.enoughData) "패턴 확인까지 ${(data.minimumPatternRecords-pattern.count).coerceAtLeast(0)}회 더 필요합니다." else if (pattern.hasRepeatedChange) "반복 변화 관찰됨" else "변화 방향을 더 살펴봐야 합니다.")
        }
        writer.section("04 AI가 분석한 나의 감정 변화")
        writer.paragraph(aiText?.let(AiAnalysisTextFormatter::normalize) ?: "아직 생성된 AI 분석이 없습니다.")
        writer.section("05 분석 근거")
        writer.line("분석기간: ${data.period.label}")
        writer.line("비교 가능한 전후 기록: ${data.comparableRecords}회")
        writer.line("분석된 회복활동: ${data.activityPatterns.size}종")
        data.overallChanges.forEach { writer.line("${it.categoryLabel} ${signed(it.averageDelta)}") }
        writer.section("06 장기 변화")
        if (!data.longTerm.available) writer.line("표시할 사연 회상 기록이 없습니다.")
        data.longTerm.stories.take(5).forEach { story ->
            writer.bold(story.storyTitle)
            val previousAvailable = story.previousDataAvailable ?: (story.previousDailyAverage != null)
            writer.line(if (!previousAvailable) "이전 기록 없음" else "이전 하루 평균 ${"%.2f".format(story.previousDailyAverage ?: 0.0)}회 → 최근 하루 평균 ${"%.2f".format(story.dailyAverage)}회")
            writer.line(when { !previousAvailable -> "이전 기록 없음"; story.changePercent == null && (story.previousDailyAverage ?: 0.0) == 0.0 && story.dailyAverage > 0.0 -> "회상 빈도 증가"; story.changePercent == null -> "회상 빈도 변화 비교 불가"; story.changePercent < 0 -> "회상 빈도 ↓ ${abs(story.changePercent)}%"; story.changePercent > 0 -> "회상 빈도 ↑ ${abs(story.changePercent)}%"; else -> "회상 빈도 변화 없음" })
        }
        writer.space(24f)
        writer.paragraph("이 리포트는 사용자가 기록한 회복활동 및 감정 데이터를 바탕으로 변화 패턴을 정리한 개인 참고자료이며, 의학적 진단이나 치료 결과를 의미하지 않습니다.", 23f, Color.GRAY)
        writer.finish()

        val name = "우치소_AI감정분석_${SimpleDateFormat("yyyy-MM-dd", Locale.KOREA).format(Date())}.pdf"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, name); put(MediaStore.Downloads.MIME_TYPE, "application/pdf")
                put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/Woochiso"); put(MediaStore.Downloads.IS_PENDING, 1)
            }
            val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: error("MediaStore insert failed")
            context.contentResolver.openOutputStream(uri)?.use(pdf::writeTo) ?: error("Output unavailable")
            context.contentResolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
        } else {
            val dir = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "Woochiso").apply { mkdirs() }
            FileOutputStream(File(dir, name)).use(pdf::writeTo)
        }
        pdf.close(); true
    }.getOrDefault(false)

    private class Writer(private val pdf: PdfDocument) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = android.graphics.Typeface.create("sans", 0) }
        private var pageNumber = 0; private var page: PdfDocument.Page? = null; private var y = 100f
        init { nextPage() }
        fun heading(text: String, size: Float, color: Int) { ensure(size + 28); paint.textSize=size; paint.color=color; paint.isFakeBoldText=true; page!!.canvas.drawText(text, 80f, y, paint); y += size + 22 }
        fun section(text: String) { space(18f); heading(text, 34f, Color.rgb(25,35,65)) }
        fun bold(text: String) { line(text, 27f, Color.DKGRAY, true) }
        fun line(text: String, size: Float = 25f, color: Int = Color.DKGRAY, bold: Boolean = false) { paragraph(text, size, color, bold) }
        fun paragraph(text: String, size: Float = 25f, color: Int = Color.DKGRAY, bold: Boolean = false) {
            paint.textSize=size; paint.color=color; paint.isFakeBoldText=bold
            text.lines().forEach { raw -> wrap(raw, if(size >= 32) 35 else 62).forEach { value -> ensure(size + 14); page!!.canvas.drawText(value, 80f, y, paint); y += size + 12 } }
        }
        fun space(value: Float) { y += value }
        fun finish() { page?.let(pdf::finishPage); page=null }
        private fun ensure(height: Float) { if (y + height > HEIGHT - 100) { page?.let(pdf::finishPage); nextPage() } }
        private fun nextPage() { pageNumber++; page=pdf.startPage(PdfDocument.PageInfo.Builder(WIDTH,HEIGHT,pageNumber).create()); page!!.canvas.drawColor(Color.WHITE); y=100f }
        private fun wrap(text: String, max: Int): List<String> = if(text.isBlank()) listOf("") else text.chunked(max)
    }
    private fun signed(value: Double) = (if(value>0) "+" else "") + if(value%1.0==0.0) value.toInt() else String.format(Locale.US,"%.1f",value)
}
