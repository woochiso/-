package com.example.utils

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.media.MediaScannerConnection
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.BuildConfig
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


enum class SocialPlatform(
    val id: String,
    val displayName: String,
    val packageName: String,
    val iconEmoji: String,
    val brandBgColor: Long,
    val brandTextColor: Long
) {
    KAKAO_TALK("kakao", "카카오톡", "com.kakao.talk", "💬", 0xFFFEE500, 0xFF3C1E1E),
    INSTAGRAM("instagram", "인스타그램", "com.instagram.android", "📷", 0xFFE4405F, 0xFFFFFFFF),
    NAVER_BLOG("naver_blog", "네이버 블로그", "com.nhn.android.blog", "📝", 0xFF03C75A, 0xFFFFFFFF),
    NAVER_CAFE("naver_cafe", "네이버 카페", "com.nhn.android.navercafe", "☕", 0xFF03C75A, 0xFFFFFFFF),
    BAND("band", "밴드", "com.nhn.android.band", "🟢", 0xFF00C73C, 0xFFFFFFFF),
    SYSTEM_SHARE("system", "기타 / 전체 공유", "", "🌐", 0xFF4A6572, 0xFFFFFFFF)
}

object ShareUtils {

    fun shareStoryToPlatform(
        context: Context,
        platform: SocialPlatform,
        storyTitle: String,
        storyContent: String,
        reflection: String = "",
        dateString: String = "",
        userNickname: String? = null
    ) {
        val authorPrefix = if (!userNickname.isNullOrBlank()) "${userNickname}님의 " else ""
        val fullText = buildString {
            append("📖 [우치소 ${authorPrefix}내면의 소설/사연]\n")
            if (dateString.isNotBlank()) append("작성일: $dateString\n")
            if (storyTitle.isNotBlank()) append("제목: $storyTitle\n\n")
            append(storyContent)
            if (reflection.isNotBlank()) {
                append("\n\n💡 [내면의 성찰]\n$reflection")
            }
            append("\n\n#우치소 #우울증치료방법연구소 #감정다이어리 #마음건강 #내면의소설")
        }

        if (platform == SocialPlatform.SYSTEM_SHARE || platform.packageName.isBlank()) {
            shareEmotionDiary(context, fullText, null)
            return
        }

        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, fullText)
                setPackage(platform.packageName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val pm = context.packageManager
            val matches = pm.queryIntentActivities(intent, 0)
            if (matches.isNotEmpty()) {
                context.startActivity(intent)
            } else {
                Toast.makeText(
                    context,
                    "${platform.displayName} 앱 미설치로 인해 일반 공유 창을 엽니다.",
                    Toast.LENGTH_SHORT
                ).show()
                shareEmotionDiary(context, fullText, null)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            shareEmotionDiary(context, fullText, null)
        }
    }

    fun saveBitmapToGallery(context: Context, bitmap: Bitmap, title: String): Boolean {
        val filename = "${title}_${System.currentTimeMillis()}.png"
        var insertedUri: Uri? = null

        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, filename)
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/Woochiso")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                insertedUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: throw IOException("MediaStore insert returned null")
                resolver.openOutputStream(insertedUri!!, "w")?.use { stream ->
                    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                        throw IOException("Bitmap compression failed")
                    }
                    stream.flush()
                } ?: throw IOException("MediaStore output stream unavailable")

                resolver.update(
                    insertedUri!!,
                    ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) },
                    null,
                    null
                )
            } else {
                val pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                val woochisoFolder = File(pictures, "Woochiso")
                if (!woochisoFolder.exists() && !woochisoFolder.mkdirs()) {
                    throw IOException("Pictures directory unavailable")
                }
                val imageFile = File(woochisoFolder, filename)
                FileOutputStream(imageFile).use { stream ->
                    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                        throw IOException("Bitmap compression failed")
                    }
                    stream.flush()
                }
                MediaScannerConnection.scanFile(
                    context,
                    arrayOf(imageFile.absolutePath),
                    arrayOf("image/png"),
                    null
                )
            }
            true
        } catch (error: Exception) {
            insertedUri?.let { runCatching { context.contentResolver.delete(it, null, null) } }
            if (BuildConfig.DEBUG) {
                Log.e("WOOCHISO_GRAPH", "GRAPH_IMAGE_SAVE_FAILED reason=${error.javaClass.simpleName}")
            }
            false
        }
    }

    fun shareEmotionDiary(context: Context, text: String, bitmap: Bitmap?) {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND)

            if (bitmap != null) {
                val cachePath = File(context.cacheDir, "images")
                cachePath.mkdirs()
                val imageFile = File(cachePath, "emotion_diary_share.png")
                val stream = FileOutputStream(imageFile)
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                stream.close()

                val contentUri: Uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    imageFile
                )

                shareIntent.type = "image/png"
                shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri)
                shareIntent.putExtra(Intent.EXTRA_TEXT, text)
                shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                shareIntent.type = "text/plain"
                shareIntent.putExtra(Intent.EXTRA_TEXT, text)
            }

            val chooser = Intent.createChooser(shareIntent, "감정 다이어리 공유하기")
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "공유하기 오류: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
