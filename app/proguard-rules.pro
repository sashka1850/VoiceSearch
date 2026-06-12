# =====================================================================
# VoiceSearch — release R8 / ProGuard rules.
#
# Each block here exists for one of three reasons:
#   1. Library uses reflection (Retrofit, kotlinx-serialization)
#   2. Annotation-driven codegen needs annotations preserved at runtime
#   3. Crash stack traces should remain readable for AppMetrica reports
# =====================================================================

# ---------- Crash stack readability ----------
# Keep file + line info so AppMetrica crash reports map to real lines.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ---------- kotlinx.serialization ----------
# Official rule set from https://github.com/Kotlin/kotlinx.serialization
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# Keep Companion of @Serializable classes.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

# Keep serializer() on companions of @Serializable classes.
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep INSTANCE.serializer() on @Serializable objects.
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# Polymorphic serialization metadata
-keep,includedescriptorclasses class **$$serializer { *; }

# ---------- Retrofit + OkHttp ----------
# Service method parameters and annotations are read reflectively.
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleParameterAnnotations

-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}

-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement

# OkHttp transitively pulls in optional security providers; we don't use them.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# ---------- Hilt / Dagger ----------
# Hilt's KSP generates final code; reflection-touched bits (annotated
# Application / Activity / ViewModel) need explicit retention.
-keep,allowobfuscation,allowshrinking @dagger.hilt.android.HiltAndroidApp class * { *; }
-keep,allowobfuscation,allowshrinking @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keep,allowobfuscation,allowshrinking @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep,allowobfuscation,allowshrinking @dagger.hilt.android.WorkerInject class * { *; }
-keep,allowobfuscation,allowshrinking @androidx.hilt.work.HiltWorker class * { *; }

# javax.inject annotations may be queried by Dagger's reflection probes.
-keepattributes *Annotation*

# ---------- Room ----------
# Room's processor outputs final code; rarely needs rules, but classes
# named *_Impl are referenced by class.forName from Room.
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keepclassmembers @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# ---------- WorkManager + HiltWorker ----------
-keep class * extends androidx.work.ListenableWorker { <init>(android.content.Context, androidx.work.WorkerParameters); }

# ---------- Compose / Material 3 ----------
# Compose runtime doesn't need extra rules — its consumer-proguard files
# already cover what's needed. This just suppresses optional-class warnings
# from compose-material-icons-extended (lots of unused icons).
-dontwarn androidx.compose.material.icons.**

# ---------- Lottie ----------
-keep class com.airbnb.lottie.** { *; }
-dontwarn com.airbnb.lottie.**

# ---------- androidx.security / Tink ----------
# EncryptedSharedPreferences pulls in com.google.crypto.tink, which
# references errorprone annotations only at compile time. They're not on
# the runtime classpath, so silence the R8 "missing class" warnings.
-dontwarn com.google.errorprone.annotations.**
-dontwarn com.google.crypto.tink.**
-keep class com.google.crypto.tink.** { *; }
# Tink also uses jsr305 @Nullable / @CheckReturnValue which aren't bundled.
-dontwarn javax.annotation.**
-dontwarn javax.annotation.concurrent.**

# ---------- App-specific ----------
# Yandex OAuth response models are read via Retrofit + serialization
# — covered by the kotlinx-serialization block above, listed here for clarity.
-keep class com.voicesearch.core.network.yandex.** { *; }
-keep class com.voicesearch.core.domain.model.** { *; }
