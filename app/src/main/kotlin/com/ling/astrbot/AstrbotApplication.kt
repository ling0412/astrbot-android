package com.ling.astrbot

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import com.ling.astrbot.core.datastore.SettingsDataStore
import com.ling.astrbot.core.network.client.CredentialsHolder
import com.ling.astrbot.core.network.client.SessionManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltAndroidApp
class AstrbotApplication : Application(), SingletonImageLoader.Factory {

    @Inject
    lateinit var settingsDataStore: SettingsDataStore

    @Inject
    lateinit var credentialsHolder: CredentialsHolder

    @Inject
    lateinit var sessionManager: SessionManager

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            settingsDataStore.authSettings.collect { settings ->
                credentialsHolder.baseUrl = settings.baseUrl
            }
        }
        appScope.launch {
            sessionManager.state.collect {
                credentialsHolder.token = sessionManager.currentToken().orEmpty()
            }
        }
    }

    override fun newImageLoader(context: PlatformContext): ImageLoader {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val token = credentialsHolder.token
                val request = if (token.isBlank()) {
                    chain.request()
                } else {
                    chain.request().newBuilder().header("Authorization", "Bearer $token").build()
                }
                chain.proceed(request)
            }
            .build()

        return ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { client })) }
            .build()
    }
}
