package com.ling.astrbot.core.network.client

import com.ling.astrbot.core.network.api.AstrbotApiService
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 已按某个实例配置构建好的 API 客户端。
 */
class AstrbotClient internal constructor(
    val api: AstrbotApiService,
)

/**
 * 依据服务器地址构建 [AstrbotClient]，认证由 [SessionManager] 提供的 JWT 完成。
 */
@Singleton
class AstrbotClientFactory @Inject constructor(
    private val json: Json,
    private val sessionManager: SessionManager,
    private val cookieJar: PersistentCookieJar,
) {
    fun create(baseUrl: String): AstrbotClient {
        val client = OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .addInterceptor(AuthInterceptor(sessionManager))
            .authenticator(SessionAuthenticator(sessionManager))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(normalizeBaseUrl(baseUrl))
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

        return AstrbotClient(retrofit.create(AstrbotApiService::class.java))
    }

    private fun normalizeBaseUrl(raw: String): String {
        val trimmed = raw.trim()
        val withScheme = if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            // 默认 https：targetSdk 下默认禁止明文 http，用户漏写 scheme 时用 https 更可靠。
            "https://$trimmed"
        }
        return if (withScheme.endsWith("/")) withScheme else "$withScheme/"
    }
}

/** 为每个请求附加 `Authorization: Bearer <jwt>`。 */
internal class AuthInterceptor(
    private val sessionManager: SessionManager,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = sessionManager.currentToken()
        val request = if (token.isNullOrBlank()) {
            chain.request()
        } else {
            chain.request().newBuilder().header("Authorization", "Bearer $token").build()
        }
        return chain.proceed(request)
    }
}

/** 收到 401 时用已保存的密码自动重登并重试一次。 */
internal class SessionAuthenticator(
    private val sessionManager: SessionManager,
) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        if (response.request.header("Authorization") == null) return null
        if (responseCount(response) >= 2) return null
        val newToken = sessionManager.refreshBlocking() ?: return null
        return response.request.newBuilder()
            .header("Authorization", "Bearer $newToken")
            .build()
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
