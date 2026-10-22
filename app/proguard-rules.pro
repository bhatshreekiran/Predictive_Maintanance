# Cogxar Predictive Maintenance Proguard Rules
-keepattributes *Annotation*
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.cogxar.pdm.data.model.** { *; }
