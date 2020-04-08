import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  `kotlin-dsl`
  `kotlin-dsl-precompiled-script-plugins`
}

gradlePlugin {
  plugins {
    register("android-defaults-plugin") {
      id = "android-defaults-plugin"
      implementationClass = "plugins.AndroidDefaultsBinaryPlugin"
    }
  }
}

buildscript {
  repositories {
    mavenCentral()
    jcenter()
    google()
  }

  dependencies {
    // not sure how to reference Dependencies.kt
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.71")
  }
}

repositories {
  mavenCentral()
  jcenter()
  google()
}

kotlinDslPluginOptions {
  experimentalWarning.set(false)
}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
  languageVersion = "1.3.71"
}

dependencies {
  implementation("com.android.tools.build:gradle:4.0.0-beta03")
  implementation("com.android.tools.build:gradle-api:4.0.0-beta03")
  implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.3.71")
  implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.3.71")
}