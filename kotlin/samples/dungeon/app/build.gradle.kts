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
apply plugin: 'com.android.application'
apply plugin: 'kotlin-android'

apply from: rootProject.file('.buildscript/configure-android-defaults.gradle')
apply from: rootProject.file('.buildscript/android-ui-tests.gradle')

android {
  defaultConfig {
    applicationId "com.squareup.sample.dungeon"
    multiDexEnabled true
  }

  defaultConfig {
    testInstrumentationRunner "com.squareup.sample.dungeon.DungeonTestRunner"
  }

  testOptions {
    animationsDisabled = true
  }

  compileOptions {
    // Required for SnakeYAML.
    coreLibraryDesugaringEnabled true
  }
}

dependencies {
  // Required for SnakeYAML.
  coreLibraryDesugaring Dep.get("desugar_jdk_libs")

  implementation project(':samples:dungeon:common')
  implementation project(':samples:dungeon:timemachine-shakeable')
  implementation project(':workflow-ui:modal-android')
  implementation project(':workflow-core')
  implementation project(':workflow-runtime')
  implementation project(':workflow-tracing')

  implementation Dep.get("androidx.appcompat")
  implementation Dep.get("androidx.constraint_layout")
  implementation Dep.get("androidx.material")
  implementation Dep.get("androidx.gridlayout")
  implementation Dep.get("kotlin.coroutines.rx2")
  implementation Dep.get("okio")
  implementation Dep.get("rxandroid2")
  implementation Dep.get("rxjava2.rxjava2")
  implementation Dep.get("timber")
  implementation Dep.get("cycler")

  testImplementation Dep.get("test.junit")
  testImplementation Dep.get("test.truth")

  androidTestImplementation Dep.get("test.androidx.uiautomator")
}
