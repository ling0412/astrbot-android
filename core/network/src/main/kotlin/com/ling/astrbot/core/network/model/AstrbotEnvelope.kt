package com.ling.astrbot.core.network.model

import kotlinx.serialization.Serializable

/**
 * AstrBot 统一响应信封：`{"status":"ok|error","message":...,"data":...}`。
 */
@Serializable
data class AstrbotEnvelope<T>(
    val status: String? = null,
    val message: String? = null,
    val data: T? = null,
) {
    val isOk: Boolean get() = status == "ok"
}
