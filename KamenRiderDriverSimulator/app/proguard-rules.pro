# Add project specific ProGuard rules here.

# Sceneform
-keep class com.google.ar.sceneform.** { *; }
-keep class com.gorisse.thomas.sceneform.** { *; }

# ARCore
-keep class com.google.ar.core.** { *; }

# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class **$$serializer { *; }
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    *** serializer(...);
}

# Hilt
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class *

# Keep data models
-keep class com.kamenrider.simulator.data.model.** { *; }
-keep class com.kamenrider.simulator.data.config.** { *; }
