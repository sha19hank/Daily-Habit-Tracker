# ============================================================
# Mlue — ProGuard / R8 Rules
# Applied for release builds only (isMinifyEnabled = true)
# ============================================================

# ---------- Room ----------
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao interface *
-dontwarn androidx.room.**

# Keep Room-generated _Impl classes
-keep class **_Impl { *; }
-keep class **_Impl$* { *; }

# ---------- DataStore ----------
-keepclassmembers class * extends com.google.protobuf.GeneratedMessageLite {
    <fields>;
}
-keep class androidx.datastore.** { *; }

# ---------- Kotlin Coroutines ----------
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ---------- Kotlin Serialization (if used) ----------
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt

# ---------- Compose ----------
# Compose relies on reflection for previews — keep in debug, strip in release is fine.
-dontwarn androidx.compose.**

# ---------- WorkManager ----------
-keep class * extends androidx.work.Worker
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# ---------- App-specific receivers ----------
-keep class com.mlue.app.reminders.ReminderReceiver { *; }
-keep class com.mlue.app.reminders.BootReceiver { *; }
-keep class com.mlue.app.DailyHabitTrackerApp { *; }
-keep class com.mlue.app.MainActivity { *; }

# ---------- Suppress common library warnings ----------
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
