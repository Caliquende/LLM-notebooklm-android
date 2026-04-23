# ResearchFlow ProGuard Rules
-keep class com.researchflow.data.remote.** { *; }
-keepclassmembers class com.researchflow.data.remote.** { *; }

# Moshi
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keep @com.squareup.moshi.JsonQualifier interface *

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
