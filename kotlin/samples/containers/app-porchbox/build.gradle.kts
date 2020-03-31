/*
 * Copyright 2020 Square Inc.
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
import java.io.FileInputStream
import java.util.*

plugins {
  id("com.android.application")
  kotlin("android")
  id("com.google.gms.google-services")
}

apply(from = rootProject.file(".buildscript/configure-android-defaults.gradle"))
apply(from = rootProject.file(".buildscript/android-ui-tests.gradle"))

android {
  defaultConfig {
    applicationId = "com.squareup.sample.porchbox"

    val apikeyPropertiesFile = rootProject.file("samples/containers/app-porchbox/keys.properties")
    val apikeyProperties = Properties()
    apikeyProperties.load(FileInputStream(apikeyPropertiesFile))
    val stringField = "String"
    buildConfigField(stringField, "CLIENT_PASS", apikeyProperties.getProperty("CLIENT_PASS"))
    buildConfigField(stringField, "CLIENT_EMAIL", apikeyProperties.getProperty("CLIENT_EMAIL"))
  }

  testOptions {
    animationsDisabled = true
  }
}

dependencies {
  implementation(project(":samples:containers:android"))
  implementation(project(":samples:containers:poetry"))
  implementation(project(":workflow-ui:core-android"))
  implementation(project(":workflow-core"))
  implementation(project(":workflow-runtime"))

  implementation(Dependencies.AndroidX.appcompat)
  implementation(Dependencies.AndroidX.constraint_layout)
  implementation(Dependencies.AndroidX.material)
  implementation(Dependencies.AndroidX.recyclerview)
  implementation(Dependencies.cycler)
  implementation(Dependencies.Google.Firebase.analytics)
  implementation(Dependencies.Google.Firebase.auth)
  implementation(Dependencies.Google.Firebase.firestore)
  implementation(Dependencies.picasso)
  implementation(Dependencies.timber)

  testImplementation(project(":workflow-testing"))
  testImplementation(Dependencies.Kotlin.Test.mockito)
  testImplementation(Dependencies.Test.junit)
  testImplementation(Dependencies.Test.truth)
}