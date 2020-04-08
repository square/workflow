package plugins

import Versions
import com.android.build.gradle.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project

class AndroidDefaultsBinaryPlugin : Plugin<Project> {
  val Project.android: LibraryExtension
    // why is there no api for this? why does this feel so hacky?
    // from https://proandroiddev.com/sharing-build-logic-with-kotlin-dsl-203274f73013
    get() = extensions.findByName("android") as? LibraryExtension
        ?: error("Not an android module: $name")

  override fun apply(project: Project) =
    with(project) {
      applyPlugins()
      androidConfig()
      packagingOptions()
    }

  private fun Project.applyPlugins() {
    plugins.run {
      apply("com.android.library")
      apply("kotlin-android")
      apply("kotlin-android-extensions")
    }
  }

  private fun Project.androidConfig() {
    android.run {
      compileSdkVersion(Versions.targetSdk)
      buildToolsVersion("29.0.2")
      compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
      }
      defaultConfig {
        minSdkVersion(Versions.minSdk)
        targetSdkVersion(Versions.targetSdk)
        versionCode = 1
        versionName = "1.0"
      }
      buildTypes {
        getByName("debug") {
          isMinifyEnabled = false
        }
      }
      buildFeatures {
        viewBinding = true
      }
    }
  }

  private fun Project.packagingOptions() {
    android.run {
      packagingOptions {
        exclude("META-INF/atomicfu.kotlin_module")
        exclude("META-INF/common.kotlin_module")
        exclude("META-INF/android_debug.kotlin_module")
        exclude("META-INF/android_release.kotlin_module")
      }
    }
  }
}