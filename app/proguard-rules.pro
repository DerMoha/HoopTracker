# Add project specific ProGuard rules here.
-keep class com.hooptracker.app.** { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
