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
-keep class **.R$font { *; }

# 어노테이션 및 제네릭 정보 유지 (Gson, Retrofit 등)
-keepattributes *Annotation*, Signature

# @Keep 어노테이션 적용된 클래스 유지
-keep @androidx.annotation.Keep class * { *; }

# Kotlin metadata
-keep class kotlin.Metadata { *; }

# 로그 제거 (Debug 용도)
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Android 컴포넌트
-keep class * extends android.app.Activity
-keep class * extends android.app.Service
-keep class * extends android.content.BroadcastReceiver
-keep class * extends android.content.ContentProvider

# AppController
-keep class **.AppController { *; }

# Retrofit + OkHttp + Gson
-keep class retrofit2.** { *; }
-keep interface retrofit2.** { *; }
-dontwarn retrofit2.**

-keep class okhttp3.** { *; }
-dontwarn okhttp3.**

-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# CallAdapter/Converter 관련
-keep class **$CallAdapterFactory { *; }
-keep class **$ConverterFactory { *; }

# retrofit2.Call, retrofit2.Response
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# Kotlin + Coroutine
-keepclassmembers class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# Coroutine Continuation
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# kotlinx.coroutines.flow.Flow
-keep,allowobfuscation,allowshrinking class kotlinx.coroutines.flow.Flow

-dontwarn kotlin.**
-dontwarn org.jetbrains.**
-dontwarn javax.annotation.**
-dontwarn androidx.**
-keep class androidx.lifecycle.** { *; }
-keep class androidx.fragment.app.** { *; }
-keep class androidx.security.** { *; }

-keep class com.google.android.gms.auth.** { *; }
-keep class com.google.android.gms.auth.api.signin.** { *; }
-keep class com.google.android.gms.tasks.** { *; }
-dontwarn com.google.android.gms.**

-keep class com.kakao.** { *; }
-dontwarn com.kakao.**

-keep class com.nhn.android.naverlogin.** { *; }
-dontwarn com.nhn.android.naverlogin.**

-keep class com.google.zxing.** { *; }
-dontwarn com.google.zxing.**

-keep class com.mikhaellopez.circularprogressbar.** { *; }
-dontwarn com.mikhaellopez.circularprogressbar.**

-keep class com.prolificinteractive.materialcalendarview.** { *; }
-dontwarn com.prolificinteractive.materialcalendarview.**

-keep class com.github.mikephil.charting.** { *; }
-dontwarn com.github.mikephil.charting.**

-keep class org.threeten.bp.** { *; }
-dontwarn org.threeten.bp.**

-keep class org.tensorflow.** { *; }
-dontwarn org.tensorflow.**

-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

-keep class com.google.mlkit.** { *; }

-keep class com.google.api.client.googleapis.extensions.android.gms.auth.** { *; }
-dontwarn com.google.api.client.googleapis.extensions.android.gms.auth.**

# DTO, Entity, API
-keep class com.aitronbiz.arron.api.dto.** { *; }
-keep class com.aitronbiz.arron.model.** { *; }
-keep class com.aitronbiz.arron.database.** { *; }
-keep interface com.aitronbiz.arron.api.** { *; }

# 전체 유지 (추후 필요 시 좁혀야 함)
-keep class com.aitronbiz.arron.** { *; }
-dontwarn com.aitronbiz.arron.**

# enum 값 유지
-keepclassmembers enum * { *; }

# 기본 생성자 유지
-keepclassmembers class * {
    <init>(...);
}

# 리플렉션을 사용하는 제네릭 타입 유지
-keep public class * extends java.lang.reflect.ParameterizedType { *; }