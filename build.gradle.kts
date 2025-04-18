// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.0"
}
buildscript {
    dependencies {
        classpath("com.android.tools.build:gradle:8.2.0") // Or latest stable
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.10") // Or latest stable

    }
}
