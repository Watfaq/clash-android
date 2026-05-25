-keep class rs.clash.android.** { *; }
-keep class uniffi.clash_android_ffi.** { *; }

# JNA specific rules
-dontwarn java.awt.*
-keep class com.sun.jna.* { *; }
-keepclassmembers class * extends com.sun.jna.* { public *; }

# rustls-platform-verifier
-keep, includedescriptorclasses class org.rustls.platformverifier.** { *; }


# Serializer for classes with named companion objects are retrieved using `getDeclaredClasses`.
# If you have any, replace classes with those containing named companion objects.
-keepattributes InnerClasses # Needed for `getDeclaredClasses`.

-keepnames class <1>$$serializer {
    static <1>$$serializer INSTANCE;
}