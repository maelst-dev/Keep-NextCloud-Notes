# Keep Kotlin data classes used by Retrofit/Gson (prevents field name obfuscation)
-keep class com.keepnc.data.remote.dto.** { *; }
-keep class com.keepnc.data.auth.LoginFlow** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Retrofit
-keepattributes Signature
-keepattributes Exceptions
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

# Markwon
-keep class io.noties.markwon.** { *; }

# Room — keep entity field names
-keep class com.keepnc.data.local.** { *; }
