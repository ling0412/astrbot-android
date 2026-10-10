package com.ling.astrbot.core.network.error

import retrofit2.HttpException
import java.io.IOException
import kotlin.coroutines.cancellation.CancellationException

/** 把异常映射为面向用户的中文提示。 */
fun userMessage(error: Throwable): String = when (error) {
    is HttpException -> when (error.code()) {
        401 -> "登录状态已失效，请重新登录"
        403 -> "权限不足，请确认账号权限"
        404 -> "接口不存在，请检查服务器地址"
        else -> "请求失败（HTTP ${error.code()}）"
    }

    is IOException -> "无法连接服务器：${error.message ?: "网络错误"}"
    else -> error.message ?: "未知错误"
}

/** `Result` 的失败统一转成带用户提示的 [IllegalStateException]。 */
fun <T> Result<T>.toUserError(): Result<T> = fold(
    onSuccess = { Result.success(it) },
    onFailure = { error ->
        // 不要吞掉协程取消，否则破坏结构化取消。
        if (error is CancellationException) throw error
        Result.failure(IllegalStateException(userMessage(error)))
    },
)
