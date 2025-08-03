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

# Keep Hilt generated classes
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ApplicationComponentManager { *; }

# Keep service classes
-keep class com.vizion.security.service.** { *; }
-keep class com.vizion.security.receiver.** { *; }

# Keep accessibility service
-keep class * extends android.accessibilityservice.AccessibilityService { *; }

# Keep device admin receiver
-keep class * extends android.app.admin.DeviceAdminReceiver { *; }

# Keep Google Error Prone annotations (required by Tink crypto library)
-dontwarn com.google.errorprone.annotations.**
-keep class com.google.errorprone.annotations.** { *; }

# Keep Tink crypto library classes
-keep class com.google.crypto.tink.** { *; }
-dontwarn com.google.crypto.tink.**

# Keep EncryptedSharedPreferences related classes
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**

# Keep annotation classes
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod

# Keep Retrofit and OkHttp classes
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-dontwarn retrofit2.**
-dontwarn okhttp3.**

# Keep Gson classes
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# Keep Room database classes
-keep class androidx.room.** { *; }
-dontwarn androidx.room.**

# Keep Hilt generated classes
-keep class **_HiltModules { *; }
-keep class **_HiltModules$* { *; }
-keep class **_Provide* { *; }
-keep class **_Factory { *; }
-keep class **_MembersInjector { *; }

# Keep WorkManager classes
-keep class androidx.work.** { *; }
-dontwarn androidx.work.**