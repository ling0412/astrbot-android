package com.ling.astrbot.core.common.util

import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

object TimeUtils {

    private val displayFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("MM-dd HH:mm")

    private val clockFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("HH:mm")

    /** 解析 ISO-8601（含时区）字符串为 epoch 毫秒，失败返回 null。 */
    fun parseIsoToEpochMs(value: String?): Long? {
        if (value.isNullOrBlank()) return null
        return try {
            OffsetDateTime.parse(value).toInstant().toEpochMilli()
        } catch (_: DateTimeParseException) {
            try {
                Instant.parse(value).toEpochMilli()
            } catch (_: DateTimeParseException) {
                null
            }
        }
    }

    /** 将 epoch 毫秒格式化为本地时间展示文本。 */
    fun formatEpochMs(epochMs: Long?): String {
        if (epochMs == null) return ""
        return displayFormatter.format(
            Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()),
        )
    }

    /** 只格式化「时:分」，例如 20:40。 */
    fun formatClock(epochMs: Long?): String {
        if (epochMs == null) return ""
        return clockFormatter.format(
            Instant.ofEpochMilli(epochMs).atZone(ZoneId.systemDefault()),
        )
    }
}
