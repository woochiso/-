package com.example.data.repository

import com.example.data.remote.ApiService
import com.example.data.remote.dto.HelpVideo
import java.io.IOException

sealed interface HelpVideoResult {
    data class Success(val videos: List<HelpVideo>) : HelpVideoResult
    data class Failure(val message: String) : HelpVideoResult
}

class HelpVideoRepository(private val apiService: ApiService) {
    suspend fun load(): HelpVideoResult = try {
        val response = apiService.getHelpVideos()
        val body = response.body()
        if (response.isSuccessful && body?.success == true) {
            HelpVideoResult.Success(body.videos.sortedBy { it.sortOrder })
        } else {
            HelpVideoResult.Failure(body?.message ?: "사용방법 영상을 불러오지 못했습니다. 잠시 후 다시 시도해주세요.")
        }
    } catch (_: IOException) {
        HelpVideoResult.Failure("인터넷 연결을 확인해주세요.")
    } catch (_: Exception) {
        HelpVideoResult.Failure("사용방법 영상을 불러오지 못했습니다. 잠시 후 다시 시도해주세요.")
    }
}
