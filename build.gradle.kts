// Top-level build file
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.7.3")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.10")
        classpath("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:2.1.10-1.0.29")
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
