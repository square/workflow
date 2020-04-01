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
apply(from = rootProject.file(".buildscript/android-ui-tests.gradle"))

android {
  defaultConfig {
    applicationId = "com.squareup.sample.dungeon"
    multiDexEnabled = true

    testInstrumentationRunner = "com.squareup.sample.dungeon.DungeonTestRunner"
  }

  testOptions {
    animationsDisabled = true
  }

  compileOptions {
    // Required for SnakeYAML.
    coreLibraryDesugaringEnabled = true
  }
}

dependencies {
  // Required for SnakeYAML.
  "coreLibraryDesugaring"(get("desugar_jdk_libs"))

  implementation(project(":samples:dungeon:common"))
  implementation(project(":samples:dungeon:timemachine-shakeable"))
  implementation(project(":workflow-ui:modal-android"))
  implementation(project(":workflow-core"))
  implementation(project(":workflow-runtime"))
  implementation(project(":workflow-tracing"))

  implementation(get("androidx.appcompat"))
  implementation(get("androidx.constraint_layout"))
  implementation(get("androidx.material"))
  implementation(get("androidx.gridlayout"))
  implementation(get("kotlin.coroutines.rx2"))
  implementation(get("okio"))
  implementation(get("rxandroid2"))
  implementation(get("rxjava2.rxjava2"))
  implementation(get("timber"))
  implementation(get("cycler"))

  testImplementation(get("test.junit"))
  testImplementation(get("test.truth"))

  androidTestImplementation(get("test.androidx.uiautomator"))
}
