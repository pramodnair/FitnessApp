# ProGuard rules for NutriFit AI

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep data models
-keep class com.example.fitnessapp.data.model.** { *; }
-keep class com.example.fitnessapp.**Nav { *; }

# OkHttp & CameraX
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn androidx.camera.**
