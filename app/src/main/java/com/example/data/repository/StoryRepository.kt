package com.example.data.repository

import com.example.auth.TokenManager
import com.example.data.remote.ApiService
import com.example.data.remote.dto.*
import org.json.JSONObject

sealed interface StoryResult<out T> {
    data class Success<T>(val value: T, val message: String? = null): StoryResult<T>
    data class Error(val message: String): StoryResult<Nothing>
    data object Unauthorized: StoryResult<Nothing>
}
class StoryRepository(private val api: ApiService, private val tokens: TokenManager) {
    suspend fun list() = call { a -> api.getStories(a, 1, 20) }.let { r -> when(r) {
        is Raw.Success -> StoryResult.Success(r.body.stories)
        is Raw.Error -> r.toResult(); Raw.Unauthorized -> StoryResult.Unauthorized } }
    suspend fun detail(id: Long) = call { a -> api.getStory(a, id) }.storyResult()
    suspend fun create(r: StoryWriteRequest) = call { a -> api.createStory(a, r) }.storyResult()
    suspend fun update(r: StoryUpdateRequest) = call { a -> api.updateStory(a, r) }.storyResult()
    suspend fun delete(id: Long): StoryResult<Unit> = call { a -> api.deleteStory(a, StoryDeleteRequest(id)) }.let { r -> when(r) {
        is Raw.Success -> StoryResult.Success(Unit, r.body.message); is Raw.Error -> r.toResult(); Raw.Unauthorized -> StoryResult.Unauthorized } }

    private suspend fun <T> call(block: suspend (String)->retrofit2.Response<T>): Raw<T> {
        val token=tokens.accessToken() ?: return Raw.Unauthorized
        return try { val response=block("Bearer $token"); if(response.isSuccessful && response.body()!=null) Raw.Success(response.body()!!)
            else if(response.code()==401) Raw.Unauthorized else Raw.Error(response.code(), errorMessage(response))
        } catch(_: java.io.IOException){ Raw.Error(0,"인터넷 연결을 확인해주세요.") }
          catch(_:Exception){ Raw.Error(500,"사연 요청을 처리하지 못했습니다.") }
    }
    private fun errorMessage(r: retrofit2.Response<*>)=runCatching { JSONObject(r.errorBody()?.string().orEmpty()).optString("message") }.getOrNull().takeUnless{it.isNullOrBlank()} ?: when(r.code()){404->"사연을 찾을 수 없습니다."; else->"사연 요청에 실패했습니다."}
    private sealed interface Raw<out T>{data class Success<T>(val body:T):Raw<T>;data class Error(val code:Int,val message:String):Raw<Nothing>;data object Unauthorized:Raw<Nothing>}
    private fun Raw.Error.toResult()=StoryResult.Error(message)
    private fun Raw<StoryResponse>.storyResult():StoryResult<StoryDto> = when(this){is Raw.Success->body.story?.let{StoryResult.Success(it,body.message)}?:StoryResult.Error("서버 응답을 처리하지 못했습니다.");is Raw.Error->toResult();Raw.Unauthorized->StoryResult.Unauthorized}
}
