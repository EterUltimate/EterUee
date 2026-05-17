# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.

# Keep data models
-keep class com.eterultimate.eteruee.roleplay.data.model.** { *; }

# Keep Room entities
-keep class com.eterultimate.eteruee.roleplay.data.local.entity.** { *; }
