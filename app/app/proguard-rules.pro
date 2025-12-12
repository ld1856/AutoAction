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
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# --- AutoAction Specific Rules ---

# Keep Data Models for Gson Serialization/Deserialization
-keep class com.autoaction.data.model.** { *; }
-keep class com.autoaction.data.local.** { *; }

# Keep Gson internals
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class sun.misc.Unsafe { *; }

# Keep Room Database entities (redundant with above local.** but good for clarity)
-keep class androidx.room.** { *; }

# Keep Lifecycle components if necessary (usually handled by R8, but for safety in Services)
-keep class androidx.lifecycle.** { *; }
