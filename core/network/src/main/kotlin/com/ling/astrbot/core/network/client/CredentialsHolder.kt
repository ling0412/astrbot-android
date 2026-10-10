package com.ling.astrbot.core.network.client

import javax.inject.Inject
import javax.inject.Singleton

/**
 * 缓存当前实例的 baseUrl / JWT，供非挂起场景（如 Coil 图片加载拦截器）同步读取。
 * 由 Application 订阅设置后更新。
 */
@Singleton
class CredentialsHolder @Inject constructor() {
    @Volatile
    var baseUrl: String = ""

    @Volatile
    var token: String = ""

    /** 附件下载地址。 */
    fun attachmentUrl(attachmentId: String): String {
        val base = baseUrl.trim().trimEnd('/')
        return "$base/api/v1/file?attachment_id=$attachmentId"
    }
}
