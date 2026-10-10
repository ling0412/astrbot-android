package com.ling.astrbot.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequestDto(
    val username: String,
    val password: String,
    val code: String? = null,
    @SerialName("trust_device_flag") val trustDeviceFlag: Boolean = false,
)

@Serializable
data class LoginResponseDto(
    val status: String? = null,
    val message: String? = null,
    val data: LoginDataDto? = null,
)

@Serializable
data class LoginDataDto(
    val token: String? = null,
    val username: String? = null,
    @SerialName("totp_required") val totpRequired: Boolean? = null,
)
