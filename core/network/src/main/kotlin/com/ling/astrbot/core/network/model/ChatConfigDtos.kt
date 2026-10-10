package com.ling.astrbot.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `GET /api/v1/chat/configs` 返回体：可选的聊天配置文件列表。
 * 服务端信封 data 形如 `{"info_list":[{id,name,path}]}`。
 */
@Serializable
data class ChatConfigsDto(
    @SerialName("info_list") val infoList: List<ChatConfigProfileDto> = emptyList(),
)

@Serializable
data class ChatConfigProfileDto(
    val id: String = "",
    val name: String = "",
    val path: String? = null,
)

/**
 * `GET /api/v1/config-routes` 返回体：UMO → 配置文件 id 的绑定表。
 */
@Serializable
data class ConfigRoutesDto(
    val routing: Map<String, String> = emptyMap(),
)

/**
 * `PUT /api/v1/config-routes/{umo}` 请求体。
 */
@Serializable
data class ConfigRouteUpsertRequest(
    @SerialName("config_id") val configId: String,
)
