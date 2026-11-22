# Add project specific ProGuard rules here.
# Preserve line numbers for production stack traces; hide original file names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---------------------------------------------------------------------------
# Kotlin / Kotlin reflection
# ---------------------------------------------------------------------------
-keep class kotlin.Metadata { *; }
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# ---------------------------------------------------------------------------
# Room (androidx.room)
# ---------------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Database class * { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keep @androidx.room.TypeConverter class * { *; }
-keep class **_Impl { *; }
-dontwarn androidx.room.paging.**

-keep class com.happypuppy.toastmasterstimer.data.database.** { *; }
-keep class com.happypuppy.toastmasterstimer.data.repository.DateConverters { *; }

# ---------------------------------------------------------------------------
# Koin 3.x
# ---------------------------------------------------------------------------
-keep class com.happypuppy.toastmasterstimer.di.** { *; }
-keep class com.happypuppy.toastmasterstimer.ui.screens.SpeechTimerViewModel { *; }
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-dontwarn org.koin.**

# ---------------------------------------------------------------------------
# Kotlin Coroutines
# ---------------------------------------------------------------------------
-dontwarn kotlinx.coroutines.debug.**
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ---------------------------------------------------------------------------
# AndroidX DataStore (Preferences)
# ---------------------------------------------------------------------------
-dontwarn androidx.datastore.**

# ---------------------------------------------------------------------------
# AndroidX Navigation Compose & Compose
# ---------------------------------------------------------------------------
-keepnames class androidx.navigation.** { *; }
-dontwarn androidx.navigation.**
-dontwarn androidx.compose.**
