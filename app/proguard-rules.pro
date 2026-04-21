# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep Room entities
-keep class com.fittrack.data.entity.** { *; }

# Keep Room DAOs
-keep class com.fittrack.data.db.** { *; }

# Keep data classes used by Room
-keep class **$$Parcelable { *; }
