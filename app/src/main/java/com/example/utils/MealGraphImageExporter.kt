package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import com.example.data.remote.dto.MealGraphResponse
import kotlin.math.cos
import kotlin.math.sin

object MealGraphImageExporter {
    fun save(context: Context, graph: MealGraphResponse): Boolean {
        val width = 1200
        val foods = graph.frequentFoodItems()
        val height = 1320 + foods.size * 58
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textAlign = Paint.Align.CENTER }
        paint.color = Color.rgb(6, 74, 214); paint.textSize = 54f; paint.typeface = Typeface.DEFAULT_BOLD
        canvas.drawText("${graph.nickname?.takeIf { it.isNotBlank() } ?: "나"}의 식단 영양 균형", width / 2f, 90f, paint)
        paint.color = Color.rgb(49, 95, 136); paint.textSize = 30f
        canvas.drawText("기간 ${graph.periodLabel} · ${graph.recordCount}끼", width / 2f, 145f, paint)
        val colors = mapOf("carbohydrate" to 0xFF5F8FDB, "protein" to 0xFF4FA58D, "fat" to 0xFF7C78C8, "fiber" to 0xFF67A96B, "vitamin" to 0xFFDF9A43, "mineral" to 0xFF718DA5, "water" to 0xFF4B9FCA)
        val centerX = width / 2f; val centerY = 520f; val orbit = 330f
        graph.nutrients.take(7).forEachIndexed { index, nutrient ->
            val angle = Math.toRadians(-90.0 + index * 360.0 / 7.0)
            val x = centerX + orbit * cos(angle).toFloat(); val y = centerY + orbit * sin(angle).toFloat()
            val bubble = 100f + graph.bubbleScale(nutrient.valid) * 35f
            paint.color = (colors[nutrient.key] ?: 0xFF74AFDD).toInt(); canvas.drawCircle(x, y, bubble, paint)
            paint.color = Color.WHITE; paint.typeface = Typeface.DEFAULT_BOLD; paint.textSize = 31f
            canvas.drawText(nutrient.label, x, y - 22f, paint)
            paint.typeface = Typeface.DEFAULT; paint.textSize = 25f
            canvas.drawText(nutrient.displayState, x, y + 16f, paint); canvas.drawText("${nutrient.valid}끼", x, y + 52f, paint)
        }
        paint.color = Color.WHITE; canvas.drawCircle(centerX, centerY, 90f, paint)
        paint.color = Color.rgb(6, 74, 214); paint.typeface = Typeface.DEFAULT_BOLD; paint.textSize = 34f
        canvas.drawText("영양 균형", centerX, centerY + 12f, paint)
        paint.textAlign = Paint.Align.LEFT; paint.color = Color.rgb(24, 55, 105); paint.textSize = 38f
        canvas.drawText("자주 먹는 음식 TOP 20", 90f, 1010f, paint)
        paint.textSize = 28f
        if (foods.isEmpty()) canvas.drawText("선택한 기간에 기록된 음식이 없습니다.", 90f, 1075f, paint)
        foods.forEachIndexed { index, food ->
            val y = 1075f + index * 58f
            canvas.drawText("${index + 1}. ${food.name}", 105f, y, paint)
            paint.textAlign = Paint.Align.RIGHT; canvas.drawText(food.count?.let { "${it}회" } ?: "", 1095f, y, paint); paint.textAlign = Paint.Align.LEFT
        }
        val saved = ShareUtils.saveBitmapToGallery(context, bitmap, "우치소_식단그래프")
        bitmap.recycle()
        return saved
    }
}
