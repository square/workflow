import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/*
 * Copyright 2019 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

plugins {
  id("com.android.application")
  kotlin("android")
}

apply(from = rootProject.file(".buildscript/configure-android-defaults.gradle"))
apply(from = rootProject.file(".buildscript/android-sample-app.gradle"))

android {
  defaultConfig {
    applicationId = "com.squareup.sample.hellocompose"
    minSdkVersion(25)
  }
}

apply(from = rootProject.file(".buildscript/configure-compose.gradle"))
tasks.withType<KotlinCompile> {
  kotlinOptions.apiVersion = "1.3"
}

dependencies {
  implementation(project(":workflow-core"))
  implementation(project(":workflow-runtime"))
  implementation(project(":workflow-ui:core-compose"))
  implementation(Dependencies.AndroidX.appcompat)
  implementation(Dependencies.Compose.layout)
  implementation(Dependencies.Compose.material)
  implementation(Dependencies.Compose.tooling)
  implementation(Dependencies.Compose.foundation)
  implementation(Dependencies.RxJava2.rxjava2)

  debugImplementation(project(":workflow-ui:compose-tooling"))
}
