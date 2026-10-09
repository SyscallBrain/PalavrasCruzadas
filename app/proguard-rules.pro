# Compose, Navigation and DataStore ship their own rules; these cover the rest.

# kotlinx.serialization: generated serializers and companions must survive R8.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class app.palavrascruzadas.**$$serializer { *; }
-keepclassmembers class app.palavrascruzadas.** {
    *** Companion;
}
-keepclasseswithmembers class app.palavrascruzadas.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# WorkManager creates workers by class name (reminder and widget).
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}

# Sounds and level files are looked up by resource name, not by code.
-keep class app.palavrascruzadas.R$* { *; }
