/*
 * Copyright 2017 Square Inc.
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

apply(from = rootProject.file(".buildscript/android-sample-app.gradle"))
apply(from = rootProject.file(".buildscript/android-ui-tests.gradle"))

android {
  defaultConfig {
    applicationId = "com.squareup.sample.tictacworkflow"
    multiDexEnabled = true
  }

  testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
  implementation(project(":samples:containers:android"))
  implementation(project(":samples:tictactoe:common"))
  implementation(project(":workflow-ui:core-android"))
  implementation(project(":workflow-tracing"))

  implementation(Dependencies.AndroidX.constraint_layout)
  implementation(Dependencies.Kotlin.Coroutines.rx2)
  implementation(Dependencies.okio)
  implementation(Dependencies.rxandroid2)
  implementation(Dependencies.RxJava2.rxjava2)
  implementation(Dependencies.Test.AndroidX.Espresso.idlingResource)
  implementation(Dependencies.timber)

  androidTestImplementation(Dependencies.Test.AndroidX.Espresso.intents)
  androidTestImplementation(Dependencies.Test.AndroidX.runner)
  androidTestImplementation(Dependencies.Test.AndroidX.truthExt)
  androidTestImplementation(Dependencies.Test.junit)
  androidTestImplementation(Dependencies.Test.truth)
}
