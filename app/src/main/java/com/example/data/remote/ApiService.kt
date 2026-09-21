package com.example.data.remote

import com.example.data.remote.dto.LoginRequest
import com.example.data.remote.dto.LoginResponse
import com.example.data.remote.dto.ProfileResponse
import com.example.data.remote.dto.ProfileUpdateRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Multipart
import retrofit2.http.Part
import okhttp3.ResponseBody
import okhttp3.MultipartBody
import okhttp3.RequestBody
import com.example.data.remote.dto.*

interface ApiService {
    @POST("login.php")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @POST("logout.php")
    suspend fun logout(@Header("Authorization") authorization: String): Response<SimpleActionResponse>

    @POST("auth/google/login.php")
    suspend fun googleLogin(@Body request: GoogleLoginRequest): Response<GoogleAuthResponse>

    @POST("auth/google/complete.php")
    suspend fun completeGoogleSignup(@Body request: GoogleSignupRequest): Response<GoogleAuthResponse>

    @POST("register.php")
    suspend fun register(@Body request: RegistrationRequest): Response<RegistrationResponse>

    @POST("auth/check-email.php")
    suspend fun checkEmail(@Body request: EmailAvailabilityRequest): Response<EmailAvailabilityResponse>

    @POST("auth/check-nickname.php")
    suspend fun checkNickname(@Body request: NicknameAvailabilityRequest): Response<NicknameAvailabilityResponse>

    @POST("auth/password/forgot.php")
    suspend fun forgotPassword(@Body request: ForgotPasswordRequest): Response<ForgotPasswordResponse>

    @GET("me.php")
    suspend fun profile(@Header("Authorization") authorization: String): Response<ProfileResponse>

    @POST("me/update.php")
    suspend fun updateProfile(
        @Header("Authorization") authorization: String,
        @Body request: ProfileUpdateRequest
    ): Response<ProfileResponse>

    @POST("me/password.php")
    suspend fun changePassword(
        @Header("Authorization") authorization: String,
        @Body request: PasswordChangeRequest
    ): Response<PasswordChangeResponse>

    @GET("support/index.php")
    suspend fun searchSupport(@Query("q") query: String): Response<SupportSearchResponse>

    @GET("help/videos.php")
    suspend fun getHelpVideos(): Response<HelpVideosResponse>

    @GET("support/notices.php")
    suspend fun getSupportNotices(
        @Query("q") query: String? = null,
        @Query("page") page: Int = 1
    ): Response<SupportNoticesResponse>

    @GET("support/notice.php")
    suspend fun getSupportNotice(@Query("noticeId") noticeId: Long): Response<SupportNoticeResponse>

    @GET("support/faqs.php")
    suspend fun getSupportFaqs(
        @Query("category") category: String? = null,
        @Query("q") query: String? = null
    ): Response<SupportFaqsResponse>

    @GET("support/inquiries.php")
    suspend fun getSupportInquiries(@Header("Authorization") authorization: String): Response<SupportInquiriesResponse>

    @GET("support/inquiry.php")
    suspend fun getSupportInquiry(
        @Header("Authorization") authorization: String,
        @Query("inquiryId") inquiryId: Long
    ): Response<SupportInquiryResponse>

    @POST("support/inquiry.php")
    suspend fun createSupportInquiry(
        @Header("Authorization") authorization: String,
        @Body request: SupportInquiryCreateRequest
    ): Response<SupportInquiryResponse>

    @GET("stories.php") suspend fun getStories(@Header("Authorization") auth: String, @Query("page") page: Int, @Query("limit") limit: Int): Response<StoryListResponse>
    @GET("stories/view.php") suspend fun getStory(@Header("Authorization") auth: String, @Query("story_id") storyId: Long): Response<StoryResponse>
    @POST("stories/create.php") suspend fun createStory(@Header("Authorization") auth: String, @Body request: StoryWriteRequest): Response<StoryResponse>
    @POST("stories/update.php") suspend fun updateStory(@Header("Authorization") auth: String, @Body request: StoryUpdateRequest): Response<StoryResponse>
    @POST("stories/delete.php") suspend fun deleteStory(@Header("Authorization") auth: String, @Body request: StoryDeleteRequest): Response<StoryActionResponse>

    @GET("emotions.php") suspend fun getEmotionMaster(@Header("Authorization") auth: String): Response<EmotionMasterResponse>
    @POST("emotions/favorites/toggle.php") suspend fun toggleEmotionFavorite(
        @Header("Authorization") auth: String,
        @Body request: EmotionFavoriteToggleRequest
    ): Response<EmotionFavoriteToggleResponse>
    @GET("emotions/favorites.php") suspend fun getFavoriteEmotions(@Header("Authorization") auth: String): Response<FavoriteEmotionsResponse>
    @GET("emotions/today.php") suspend fun getTodayEmotions(@Header("Authorization") auth: String, @Query("date") date: String): Response<TodayEmotionResponse>
    @POST("emotions/today/save.php") suspend fun saveTodayEmotions(@Header("Authorization") auth: String, @Body request: TodayEmotionSaveRequest): Response<TodayEmotionSaveResponse>
    @POST("emotions/today/add.php") suspend fun addTodayEmotion(@Header("Authorization") auth: String, @Body request: TodayEmotionAddRequest): Response<TodayEmotionAddResponse>
    @POST("emotions/today/correct.php") suspend fun correctTodayEmotion(@Header("Authorization") auth: String, @Body request: TodayEmotionCorrectionRequest): Response<TodayEmotionCorrectionResponse>
    @GET("emotions/stories.php") suspend fun getEmotionStoryOptions(@Header("Authorization") auth: String, @Query("emotionId") emotionId: Long, @Query("recordDate") recordDate: String): Response<EmotionStoryOptionsResponse>
    @POST("emotions/stories/link.php") suspend fun linkEmotionStory(@Header("Authorization") auth: String, @Body request: EmotionStoryLinkRequest): Response<EmotionStoryLinkResponse>
    @GET("emotions/graph.php") suspend fun getEmotionGraph(
        @Header("Authorization") auth: String,
        @Query("period") period: String? = null,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null
    ): Response<EmotionGraphResponse>
    @GET("stories/graph.php") suspend fun getStoryGraph(
        @Header("Authorization") auth: String,
        @Query("storyId") storyId: Long,
        @Query("period") period: String,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null
    ): Response<StoryGraphResponse>
    @GET("emotions/story-links.php") suspend fun getEmotionStoryInsights(
        @Header("Authorization") auth: String,
        @Query("categoryCode") categoryCode: String? = null,
        @Query("emotionId") emotionId: Long? = null,
        @Query("period") period: String,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null,
        @Query("storyId") storyId: Long? = null
    ): Response<EmotionStoryInsightsResponse>
    @GET("recovery/index.php") suspend fun getRecoveryOverview(@Header("Authorization") auth: String): Response<RecoveryOverviewResponse>
    @POST("recovery/session.php") suspend fun saveRecoverySession(@Header("Authorization") auth: String, @Body request: RecoverySessionRequest): Response<RecoverySessionResponse>
    @GET("analysis/index.php") suspend fun getAiEmotionAnalysis(
        @Header("Authorization") auth: String,
        @Query("period") period: String,
        @Query("granularity") granularity: String,
        @Query("graph_unit") graphUnit: String
    ): Response<AiEmotionAnalysisResponse>
    @GET("analysis/summary.php") suspend fun getAiEmotionAnalysisSummary(
        @Header("Authorization") auth: String,
        @Query("period") period: String
    ): Response<AiAnalysisSummaryResponse>
    @POST("analysis/summary.php") suspend fun createAiEmotionAnalysisSummary(
        @Header("Authorization") auth: String,
        @Body request: AiAnalysisSummaryRequest
    ): Response<AiAnalysisSummaryResponse>
    @GET("analysis/recovery.php") suspend fun getRecoveryInsight(
        @Header("Authorization") auth: String,
        @Query("period") period: String,
        @Query("startDate") startDate: String? = null,
        @Query("endDate") endDate: String? = null
    ): Response<RecoveryInsightResponse>
    @POST("analysis/recovery-ai.php") suspend fun getRecoveryAiInsight(
        @Header("Authorization") auth: String,
        @Body request: RecoveryAiRequest
    ): Response<RecoveryAiResponse>
    @GET("counseling/index.php") suspend fun getCounseling(
        @Header("Authorization") auth:String,
        @Query("sessionId") sessionId:Long? = null
    ):Response<CounselingResponse>
    @Headers("X-Woochiso-Read-Timeout: 60")
    @POST("counseling/index.php") suspend fun sendCounselingMessage(
        @Header("Authorization") auth:String,
        @Body request:CounselingMessageRequest
    ):Response<CounselingResponse>
    @POST("counseling/index.php") suspend fun deleteCounselingSession(
        @Header("Authorization") auth:String,
        @Body request:CounselingDeleteRequest
    ):Response<CounselingResponse>
    @Headers("X-Woochiso-Read-Timeout: 60")
    @POST("counseling/tts.php") suspend fun getCounselingSpeech(
        @Header("Authorization") auth:String,
        @Body request:CounselingTtsRequest
    ):Response<ResponseBody>

    @Multipart
    @Headers("X-Woochiso-Read-Timeout: 90")
    @POST("training/humor/analyze.php")
    suspend fun analyzeHumor(
        @Header("Authorization") auth: String,
        @Part audio: MultipartBody.Part,
        @Part("duration") duration: RequestBody,
        @Part("input_mode") inputMode: RequestBody
    ): Response<HumorAnalysisResponse>

    @Headers("X-Woochiso-Read-Timeout: 60")
    @POST("training/humor/tts.php")
    suspend fun getHumorSpeech(
        @Header("Authorization") auth: String,
        @Body request: HumorTtsRequest
    ): Response<ResponseBody>

    @GET("training/dating/index.php")
    suspend fun getDatingConfig(@Header("Authorization") auth: String): Response<DatingConfigResponse>

    @Headers("X-Woochiso-Read-Timeout: 60")
    @POST("training/dating/index.php")
    suspend fun datingAction(
        @Header("Authorization") auth: String,
        @Body request: DatingActionRequest
    ): Response<DatingActionResponse>

    @Headers("X-Woochiso-Read-Timeout: 60")
    @POST("training/dating/tts.php")
    suspend fun getDatingSpeech(
        @Header("Authorization") auth: String,
        @Body request: DatingTtsRequest
    ): Response<ResponseBody>

    @GET("training/expression/index.php")
    suspend fun getExpressionConfig(@Header("Authorization") auth: String): Response<ExpressionConfigResponse>

    @Headers("X-Woochiso-Read-Timeout: 60")
    @POST("training/expression/index.php")
    suspend fun expressionAction(
        @Header("Authorization") auth: String,
        @Body request: ExpressionActionRequest
    ): Response<ExpressionActionResponse>

    @Headers("X-Woochiso-Read-Timeout: 60")
    @POST("training/expression/tts.php")
    suspend fun getExpressionSpeech(
        @Header("Authorization") auth: String,
        @Body request: ExpressionTtsRequest
    ): Response<ResponseBody>

    @GET("training/quiz/index.php")
    suspend fun getQuizConfig(@Header("Authorization") auth: String): Response<QuizConfigResponse>

    @Headers("X-Woochiso-Read-Timeout: 60")
    @POST("training/quiz/index.php")
    suspend fun quizAction(
        @Header("Authorization") auth: String,
        @Body request: QuizActionRequest
    ): Response<QuizActionResponse>

    @Headers("X-Woochiso-Read-Timeout: 60")
    @POST("training/quiz/tts.php")
    suspend fun getQuizSpeech(
        @Header("Authorization") auth: String,
        @Body request: QuizTtsRequest
    ): Response<ResponseBody>

    @GET("training/debate/index.php") suspend fun getDebateConfig(@Header("Authorization") auth:String):Response<DebateConfigResponse>
    @Headers("X-Woochiso-Read-Timeout: 60") @POST("training/debate/index.php") suspend fun debateAction(@Header("Authorization") auth:String,@Body request:DebateActionRequest):Response<DebateActionResponse>
    @Headers("X-Woochiso-Read-Timeout: 60") @POST("training/debate/tts.php") suspend fun getDebateSpeech(@Header("Authorization") auth:String,@Body request:DebateTtsRequest):Response<ResponseBody>

    @Multipart
    @Headers("X-Woochiso-Read-Timeout: 120")
    @POST("training/vocal/analyze.php")
    suspend fun analyzeVocal(
        @Header("Authorization") auth: String,
        @Part audio: MultipartBody.Part,
        @Part mixAudio: MultipartBody.Part?,
        @Part("duration") duration: RequestBody,
        @Part("input_mode") inputMode: RequestBody,
        @Part("performance_mode") performanceMode: RequestBody,
        @Part("mr_loaded") mrLoaded: RequestBody,
        @Part("mix_created") mixCreated: RequestBody,
        @Part("mix_source_valid") mixSourceValid: RequestBody,
        @Part("mr_audio_tracks") mrAudioTracks: RequestBody,
        @Part("mic_audio_tracks") micAudioTracks: RequestBody,
        @Part("mix_destination_tracks") mixDestinationTracks: RequestBody,
        @Part("client_wav") clientWav: RequestBody,
        @Part("client_build") clientBuild: RequestBody
    ):Response<VocalAnalysisResponse>
    @Headers("X-Woochiso-Read-Timeout: 60")
    @POST("training/vocal/tts.php") suspend fun getVocalSpeech(@Header("Authorization") auth:String,@Body request:VocalTtsRequest):Response<ResponseBody>

    @GET("training/wit/index.php") suspend fun getWitConfig(@Header("Authorization") auth:String):Response<WitConfigResponse>
    @Headers("X-Woochiso-Read-Timeout: 60") @POST("training/wit/index.php") suspend fun witAction(@Header("Authorization") auth:String,@Body request:WitActionRequest):Response<WitActionResponse>
    @Headers("X-Woochiso-Read-Timeout: 60") @POST("training/wit/tts.php") suspend fun getWitSpeech(@Header("Authorization") auth:String,@Body request:WitTtsRequest):Response<ResponseBody>
}
