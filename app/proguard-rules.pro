# Preserve metadata used by kotlinx.serialization and coroutines.
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions, EnclosingMethod

# Generated serializers for @Serializable models.
-keepclassmembers class **$$serializer { *; }
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}

# Optional TLS providers referenced by OkHttp.
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
