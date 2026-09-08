import org.gradle.external.javadoc.StandardJavadocDocletOptions

// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    // Keep in sync with the kotlin-stdlib version in app/build.gradle.kts.
    val kotlinVersion = "2.1.20"

    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.11.2")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
        // NOTE: Do not place your application dependencies here;
        // they belong in the individual module build.gradle.kts files
    }
}

plugins {
    idea
}

idea {
    module {
        isDownloadJavadoc = true
        isDownloadSources = true
    }
}

allprojects {
    repositories {
        mavenCentral()
        google()

        maven {
            url = uri("https://jitpack.io")
            credentials {
                username = providers.gradleProperty("authToken").orNull
            }
        }

        maven { url = uri("https://bitbucket.org/purevpn/atom-android-releases/raw/master") }
    }

    tasks.withType<Javadoc>().configureEach {
        (options as StandardJavadocDocletOptions).apply {
            addStringOption("Xdoclint:none", "-quiet")
            addStringOption("encoding", "UTF-8")
        }
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
}
