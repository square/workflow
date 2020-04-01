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
  id("com.android.library")
  kotlin("android")
  id("org.jetbrains.dokka")
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

apply(from = rootProject.file(".buildscript/configure-maven-publish.gradle"))

apply(from = rootProject.file(".buildscript/configure-android-defaults.gradle"))

dependencies {
  compileOnly(get("androidx.viewbinding"))

  api(project(":workflow-core"))
  api(project(":workflow-ui:core-common"))

  api(get("androidx.transition"))
  api(get("kotlin.stdLib.jdk6"))
  api(get("rxjava2.rxjava2"))

  implementation(project(":workflow-runtime"))
  implementation(get("androidx.activity"))
  implementation(get("androidx.fragment"))
  implementation(get("androidx.lifecycle.reactivestreams"))
  implementation(get("androidx.savedstate"))
  implementation(get("kotlin.coroutines.android"))
  implementation(get("kotlin.coroutines.core"))
  implementation(get("kotlin.coroutines.rx2"))

  testImplementation(get("test.junit"))
  testImplementation(get("test.truth"))
  testImplementation(get("kotlin.coroutines.test"))
  testImplementation(get("kotlin.test.jdk"))
  testImplementation(get("kotlin.test.mockito"))
}
