package com.ling.astrbot.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `POST /api/v1/file` 的返回 data。
 */
@Serializable
data class UploadFileResponse(
    @SerialName("attachment_id") val attachmentId: String = "",
    val filename: String? = null,
    @SerialName("stored_filename") val storedFilename: String? = null,
)
