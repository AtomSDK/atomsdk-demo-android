import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-parcelize")
}

android {

    namespace = "com.atom.vpn.demo"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.atom.vpn.demo"
        minSdk = 24
        targetSdk = 36
        versionCode = 55
        versionName = "7.1.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
//        release {
//            multiDexEnabled = true
//            isMinifyEnabled = true
//            proguardFile("proguard-rules.pro")
//            proguardFile(getDefaultProguardFile("proguard-android.txt"))
//            signingConfig = signingConfigs.getByName("config")
//        }

        debug {
            multiDexEnabled = true
            isMinifyEnabled = false
            isDebuggable = true
        }
    }

    compileOptions {
        targetCompatibility = JavaVersion.VERSION_17
        sourceCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
        resources {
            excludes += "META-INF/*.kotlin_module"
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

// Lifted out of the `android { }` block. It only worked in there because Groovy fell through
// dynamically to project.configurations; the Kotlin DSL is statically typed and the Android
// extension has no `configurations` member. Load-bearing - it keeps xpp3_min off the classpath.
configurations.configureEach {
    exclude(group = "xpp3", module = "xpp3_min")
}

// Kotlin compiler config. Replaces the older `android.kotlinOptions` block, which the
// Kotlin Gradle Plugin deprecates in favour of `compilerOptions` from Kotlin 2.x on.
kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.annotation:annotation:1.9.1")
    implementation("com.github.vihtarb:tooltip:0.2.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    // Keep in sync with kotlinVersion in the root build.gradle.kts.
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.1.20")

    // AndroidX equivalent of the old com.android.support:multidex:1.0.3.
    // The app's Application class already extends androidx.multidex.MultiDexApplication.
    implementation("androidx.multidex:multidex:2.0.1")

    implementation("org.bitbucket.purevpn:purevpn-sdk-android:7.1.1")
}
