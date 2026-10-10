package com.ling.astrbot.core.network.api

import com.ling.astrbot.core.network.model.AstrbotEnvelope
import com.ling.astrbot.core.network.model.ChatConfigsDto
import com.ling.astrbot.core.network.model.ChatProjectDto
import com.ling.astrbot.core.network.model.ChatProjectRequest
import com.ling.astrbot.core.network.model.ChatRequestDto
import com.ling.astrbot.core.network.model.ChatSessionDetailDto
import com.ling.astrbot.core.network.model.ChatSessionDto
import com.ling.astrbot.core.network.model.ConfigRouteUpsertRequest
import com.ling.astrbot.core.network.model.ConfigRoutesDto
import com.ling.astrbot.core.network.model.ConversationDetailDto
import com.ling.astrbot.core.network.model.ConversationFilterOptionsDto
import com.ling.astrbot.core.network.model.ConversationsPageDto
import com.ling.astrbot.core.network.model.NewSessionDto
import com.ling.astrbot.core.network.model.RenameSessionRequest
import com.ling.astrbot.core.network.model.UploadFileResponse
import com.ling.astrbot.core.network.model.WorkspaceFileDto
import com.ling.astrbot.core.network.model.WorkspaceListingDto
import kotlinx.serialization.json.JsonElement
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

/**
 * AstrBot HTTP API（`/api/v1`）。baseUrl 由 [com.ling.astrbot.core.network.client.AstrbotClientFactory] 动态提供。
 */
interface AstrbotApiService {

    /** 列出某个 username 名下的 WebChat 会话（返回体可能是分页对象或 legacy 数组）。 */
    @GET("api/v1/chat/sessions")
    suspend fun getChatSessions(
        @Query("username") username: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 50,
    ): AstrbotEnvelope<JsonElement>

    /** 新建 WebChat 会话。 */
    @GET("api/v1/chat/sessions/new")
    suspend fun createChatSession(
        @Query("platform_id") platformId: String = "webchat",
    ): AstrbotEnvelope<NewSessionDto>

    /** 会话详情 + 分页历史。 */
    @GET("api/v1/chat/sessions/{sessionId}")
    suspend fun getChatSession(
        @Path("sessionId") sessionId: String,
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 1000,
    ): AstrbotEnvelope<ChatSessionDetailDto>

    /** 删除会话。 */
    @DELETE("api/v1/chat/sessions/{sessionId}")
    suspend fun deleteChatSession(
        @Path("sessionId") sessionId: String,
    ): AstrbotEnvelope<JsonElement>

    /** 重命名会话。 */
    @PATCH("api/v1/chat/sessions/{sessionId}")
    suspend fun renameChatSession(
        @Path("sessionId") sessionId: String,
        @Body body: RenameSessionRequest,
    ): AstrbotEnvelope<JsonElement>

    /** 停止生成。 */
    @POST("api/v1/chat/sessions/{sessionId}/stop")
    suspend fun stopChatSession(
        @Path("sessionId") sessionId: String,
    ): AstrbotEnvelope<JsonElement>

    /** 发送消息，返回 SSE 流。 */
    @Streaming
    @POST("api/v1/chat")
    suspend fun sendChat(
        @Body request: ChatRequestDto,
    ): ResponseBody

    /** 重新生成某条消息，返回 SSE 流。 */
    @Streaming
    @POST("api/v1/chat/sessions/{sessionId}/messages/{messageId}/regenerate")
    suspend fun regenerateChatMessage(
        @Path("sessionId") sessionId: String,
        @Path("messageId") messageId: Long,
    ): ResponseBody

    /** 续订正在运行的一次生成，返回 SSE 流（首帧为 run_snapshot）。 */
    @Streaming
    @GET("api/v1/chat/runs/{runId}/stream")
    suspend fun resumeChatRun(
        @Path("runId") runId: String,
    ): ResponseBody

    /** 上传附件，返回 attachment_id。 */
    @Multipart
    @POST("api/v1/file")
    suspend fun uploadFile(
        @Part file: MultipartBody.Part,
    ): AstrbotEnvelope<UploadFileResponse>

    // ---------- 配置文件（配置档案）----------

    /** 列出可选的聊天配置文件。 */
    @GET("api/v1/chat/configs")
    suspend fun getChatConfigs(): AstrbotEnvelope<ChatConfigsDto>

    /** 读取 UMO → 配置文件 id 的绑定表。 */
    @GET("api/v1/config-routes")
    suspend fun getConfigRoutes(): AstrbotEnvelope<ConfigRoutesDto>

    /** 把某个会话（UMO）绑定到指定配置文件；`default` 表示解除绑定。 */
    @PUT("api/v1/config-routes/{umo}")
    suspend fun upsertConfigRoute(
        @Path("umo") umo: String,
        @Body body: ConfigRouteUpsertRequest,
    ): AstrbotEnvelope<JsonElement>

    /** 列出 IM 平台会话（只读）。 */
    @GET("api/v1/conversations")
    suspend fun getConversations(
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 30,
        @Query("platforms") platforms: String = "",
        @Query("sort_by") sortBy: String = "updated_at",
        @Query("sort_order") sortOrder: String = "desc",
        @Query("include_history") includeHistory: Boolean = false,
    ): AstrbotEnvelope<ConversationsPageDto>

    /** 会话筛选项（平台机器人列表）。 */
    @GET("api/v1/conversations/filter-options")
    suspend fun getConversationFilterOptions(): AstrbotEnvelope<ConversationFilterOptionsDto>

    /** 会话详情（含历史）。 */
    @GET("api/v1/conversations/{cid}")
    suspend fun getConversationDetail(
        @Path("cid") cid: String,
        @Query("user_id") userId: String,
    ): AstrbotEnvelope<ConversationDetailDto>

    // ---------- ChatUI 项目（Projects） ----------

    /** 列出当前用户的所有项目。 */
    @GET("api/v1/chat/projects")
    suspend fun getChatProjects(): AstrbotEnvelope<List<ChatProjectDto>>

    /** 新建项目。 */
    @POST("api/v1/chat/projects")
    suspend fun createChatProject(
        @Body body: ChatProjectRequest,
    ): AstrbotEnvelope<ChatProjectDto>

    /** 获取单个项目。 */
    @GET("api/v1/chat/projects/{projectId}")
    suspend fun getChatProject(
        @Path("projectId") projectId: String,
    ): AstrbotEnvelope<ChatProjectDto>

    /** 更新项目。 */
    @PATCH("api/v1/chat/projects/{projectId}")
    suspend fun updateChatProject(
        @Path("projectId") projectId: String,
        @Body body: ChatProjectRequest,
    ): AstrbotEnvelope<JsonElement>

    /** 删除项目。 */
    @DELETE("api/v1/chat/projects/{projectId}")
    suspend fun deleteChatProject(
        @Path("projectId") projectId: String,
    ): AstrbotEnvelope<JsonElement>

    /** 列出项目内会话。 */
    @GET("api/v1/chat/projects/{projectId}/sessions")
    suspend fun getChatProjectSessions(
        @Path("projectId") projectId: String,
    ): AstrbotEnvelope<List<ChatSessionDto>>

    /** 把会话加入项目。 */
    @POST("api/v1/chat/projects/{projectId}/sessions/{sessionId}")
    suspend fun addChatProjectSession(
        @Path("projectId") projectId: String,
        @Path("sessionId") sessionId: String,
    ): AstrbotEnvelope<JsonElement>

    /** 把会话移出所属项目。 */
    @DELETE("api/v1/chat/projects/sessions/{sessionId}")
    suspend fun removeChatProjectSession(
        @Path("sessionId") sessionId: String,
    ): AstrbotEnvelope<JsonElement>

    /** 列出项目工作区目录。 */
    @GET("api/v1/chat/projects/{projectId}/workspace/files")
    suspend fun listWorkspaceFiles(
        @Path("projectId") projectId: String,
        @Query("path") path: String = "",
    ): AstrbotEnvelope<WorkspaceListingDto>

    /** 读取项目工作区文本文件。 */
    @GET("api/v1/chat/projects/{projectId}/workspace/file")
    suspend fun readWorkspaceFile(
        @Path("projectId") projectId: String,
        @Query("path") path: String,
    ): AstrbotEnvelope<WorkspaceFileDto>

    /** 下载项目工作区文件（二进制流）。 */
    @Streaming
    @GET("api/v1/chat/projects/{projectId}/workspace/file/download")
    suspend fun downloadWorkspaceFile(
        @Path("projectId") projectId: String,
        @Query("path") path: String,
    ): ResponseBody
}
