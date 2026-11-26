# Media3 / ExoPlayer
-dontwarn androidx.media3.**
-keep class androidx.media3.** { *; }

# Kotlin
-dontwarn kotlin.**

# ---- MissionPlayer ----

# TagLib (tag reading/writing): its C++ side finds these Kotlin classes, fields and methods by name through JNI,
# so renaming or removing anything in them would crash the metadata editor.
-keep class com.kyant.taglib.** { *; }

# Native methods keep their names (JNI) - also part of the default optimize file, stated here on purpose.
-keepclasseswithmembernames class * {
    native <methods>;
}

# Readable stack traces from crash reports: keep file names and line numbers.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# The in-app updater and metadata/lyrics sources parse JSON with org.json (part of Android): nothing to keep.
