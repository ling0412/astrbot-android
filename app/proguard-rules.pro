# AstrBot Android —— R8 / ProGuard 规则

# 保留注解与泛型签名（Retrofit、kotlinx.serialization 依赖它们做反射）
-keepattributes Signature, InnerClasses, EnclosingMethod, *Annotation*, Exception

# ---- kotlinx.serialization 官方推荐规则 ----
# 保留 @Serializable 类的 Companion 字段
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
# 保留 Companion 上的 serializer()（默认与命名）
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}
# 保留可序列化单例（object）的 INSTANCE.serializer()
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-dontnote kotlinx.serialization.**

# ---- 网络 DTO / 领域模型：反射与序列化用，保留 ----
-keep class com.ling.astrbot.core.network.model.** { *; }
-keep class com.ling.astrbot.core.model.** { *; }

# ---- 常见缺省 ----
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
-dontwarn org.jetbrains.annotations.**
