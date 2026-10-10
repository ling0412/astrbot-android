package com.ling.astrbot.core.network.api

import com.ling.astrbot.core.network.model.LoginRequestDto
import com.ling.astrbot.core.network.model.LoginResponseDto
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

/**
 * Dashboard 登录接口。用 [Url] 承载完整地址（服务器地址由用户配置）。
 */
interface AuthApiService {
    @POST
    suspend fun login(
        @Url url: String,
        @Body body: LoginRequestDto,
    ): Response<LoginResponseDto>
}
