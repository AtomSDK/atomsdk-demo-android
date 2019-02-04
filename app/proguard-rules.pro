# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep options here:

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
-keepattributes *Annotation*
-keep class org.icmp4j**
-dontwarn org.icmp4j**
-dontwarn okio.**
-dontwarn retrofit2.**
-dontwarn rx.internal**


-keepattributes SourceFile,LineNumberTable
-keep public class * extends java.lang.Exception


#-dontwarn com.**
#-keep class com.**


#-injars      in.jar
#-outjars     out.jar
#-libraryjars <java.home>/lib/rt.jar
#-printmapping out.map

-keepparameternames
-renamesourcefileattribute SourceFile
-keepattributes Exceptions,InnerClasses,Signature,Deprecated

-keep public class * {
      public protected *;
}


-keepclassmembernames class * {
    java.lang.Class class$(java.lang.String);
    java.lang.Class class$(java.lang.String, boolean);
}

-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

-keepclassmembers,allowoptimization enum * {
    public static **[] values(); public static ** valueOf(java.lang.String);
}

-assumenosideeffects class android.util.Log {
    public static *** e(...);
    public static *** w(...);
    public static *** wtf(...);
    public static *** d(...);
    public static *** v(...);
}

-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}


-keep class de.blinkt.openvpn.** { *; }
-keep class org.spongycastle.util.** { *; }
-keep class org.strongswan.android.** { *; }

-dontwarn org.jetbrains.annotations.**

-keep class com.jakewharton.timber.** { *; }
-keep class com.jakewharton.timber.** { *; }
-keep class com.jakewharton.timber.** { *; }

-dontwarn com.atom.sdk.**
-keep class com.atom.sdk.** { *; }
-keep interface com.atom.sdk.** { *; }

