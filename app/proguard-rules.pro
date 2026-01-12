-keep class rs.clash.android.** { *; }
-keep class uniffi.clash_android_ffi.** { *; }

# JNA specific rules
-dontwarn java.awt.*
-keep class com.sun.jna.* { *; }
-keepclassmembers class * extends com.sun.jna.* { public *; }

# SnakeYAML
-keep class org.yaml.snakeyaml.** { *; }
-dontwarn org.yaml.snakeyaml.**
