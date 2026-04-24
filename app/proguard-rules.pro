# Project-specific ProGuard rules.

# Apple MusicKit for Android is loaded through a reflection bridge so the public
# CI build can still compile without the locally downloaded SDK AARs.
-keep class com.apple.android.** { *; }
-keep interface com.apple.android.** { *; }
-dontwarn com.apple.android.**
