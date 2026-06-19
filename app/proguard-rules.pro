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

-keepattributes SourceFile,LineNumberTable,*Annotation*

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile

-keepclassmembers class com.example.loveosapk.data.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.loveosapk.data.** {
    kotlinx.serialization.KSerializer serializer(...);
}

-keep class com.example.loveosapk.data.**$$serializer { *; }
-keep class com.example.loveosapk.domain.model.**$$serializer { *; }

# Room
-keep class com.example.loveosapk.data.local.** { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Firebase
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-keepattributes Signature
-keepattributes *Annotation*

# DataStore / Preferences
-keep class androidx.datastore.** { *; }
-keep class com.example.loveosapk.data.PreferenceManager { *; }

# Kotlin Coroutines / Flow
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** { volatile <fields>; }

# Jetpack Compose
-keep class androidx.compose.** { *; }
-keep class com.example.loveosapk.ui.theme.** { *; }
-keep class com.example.loveosapk.ui.components.** { *; }

# Serializable / Parcelable
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}
-keepnames class * implements java.io.Serializable

# Navigation
-keep class com.example.loveosapk.ui.navigation.** { *; }
