# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# 폰트 리소스 유지
-keep class **.R$font {
    *;
}

# Keep all annotations (required for some libraries like Gson, Retrofit)
-keepattributes *Annotation*

# Keep generic type information (for Gson, Retrofit, etc.)
-keepattributes Signature

# Keep all classes with @Keep annotation
-keep @androidx.annotation.Keep class * { *; }

# Don't warn about missing references
-dontwarn javax.annotation.**
-dontwarn kotlin.**
-dontwarn org.jetbrains.**
-dontwarn androidx.**

# Retrofit + Gson
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# OkHttp
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**

# TensorFlow Lite
-keep class org.tensorflow.** { *; }
-dontwarn org.tensorflow.**

# Google Play Services Auth
-keep class com.google.android.gms.auth.** { *; }
-dontwarn com.google.android.gms.**

# Naver OAuth
-keep class com.nhn.android.naverlogin.** { *; }
-dontwarn com.nhn.android.naverlogin.**

# Kakao SDK
-keep class com.kakao.** { *; }
-dontwarn com.kakao.**

# ZXing
-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

# Circular Progress Bar
-keep class com.mikhaellopez.circularprogressbar.** { *; }
-dontwarn com.mikhaellopez.circularprogressbar.**

# Material CalendarView
-keep class com.prolificinteractive.materialcalendarview.** { *; }
-dontwarn com.prolificinteractive.materialcalendarview.**

# MPAndroidChart
-keep class com.github.mikephil.charting.** { *; }
-dontwarn com.github.mikephil.charting.**

# ThreeTenABP
-keep class org.threeten.bp.** { *; }
-dontwarn org.threeten.bp.**

# AndroidX Security Crypto
-keep class androidx.security.** { *; }
-dontwarn androidx.security.**

# ViewModel, LiveData, Fragment KTX
-keep class androidx.lifecycle.** { *; }
-dontwarn androidx.lifecycle.**
-keep class androidx.fragment.app.** { *; }
-dontwarn androidx.fragment.app.**

# Misc
-keep class * extends android.app.Activity
-keep class * extends android.app.Service
-keep class * extends android.content.BroadcastReceiver
-keep class * extends android.content.ContentProvider

# Keep application class
-keep class **.AppController { *; }

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }

# Optional: Debug logs
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}