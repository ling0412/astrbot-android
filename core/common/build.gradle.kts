plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "com.ling.astrbot.core.common"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        minSdk {
            version = release(34)
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)
}
