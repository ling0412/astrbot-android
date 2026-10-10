package com.ling.astrbot.core.network.client

import android.content.Context
import com.ling.astrbot.core.datastore.security.KeystoreCrypto
import dagger.hilt.android.qualifiers.ApplicationContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 持久化 CookieJar（保存 dashboard JWT 与「信任设备」cookie），
 * 用于在自动重登时跳过 2FA。
 *
 * 存储：以 `domain|name` 为 key（避免跨域同名覆盖），值用 [KeystoreCrypto] 加密。
 * 读取：解密后按 `Cookie.matches(url)` 过滤，只返回确实属于该 url 的 cookie
 * （避免把 JWT 发往第三方域名，例如正文里的外链图片）。
 */
@Singleton
class PersistentCookieJar @Inject constructor(
    @ApplicationContext context: Context,
) : CookieJar {

    private val prefs = context.getSharedPreferences("astrbot_cookies", Context.MODE_PRIVATE)
    private val lock = Any()

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (cookies.isEmpty()) return
        synchronized(lock) {
            val editor = prefs.edit()
            cookies.forEach { cookie ->
                editor.putString(cookieKey(cookie), KeystoreCrypto.encrypt(cookie.toString()))
            }
            editor.apply()
        }
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        synchronized(lock) {
            return prefs.all.values
                .mapNotNull { it as? String }
                .mapNotNull { value -> Cookie.parse(url, KeystoreCrypto.decrypt(value)) }
                .filter { it.matches(url) }
        }
    }

    private fun cookieKey(cookie: Cookie): String = "${cookie.domain}|${cookie.name}"
}
