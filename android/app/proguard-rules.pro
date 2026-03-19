# Retrofit
-keepattributes Signature
-keepattributes *Annotation*
-keep class kotlin.Metadata { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}

# kotlinx.serialization
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keep,includedescriptorclasses class com.yuerchu.remoteask.**$$serializer { *; }
-keepclassmembers class com.yuerchu.remoteask.** {
    *** Companion;
}
