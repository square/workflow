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
  id("com.android.library")
  kotlin("android")
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

apply(from = rootProject.file(".buildscript/configure-android-defaults.gradle"))

dependencies {
  api(project(":samples:dungeon:timemachine"))

  implementation(project(":workflow-ui:core-android"))
  implementation(Dependencies.AndroidX.constraint_layout)
  implementation(Dependencies.AndroidX.material)
  implementation(Dependencies.Kotlin.Stdlib.jdk8)
  implementation("com.squareup:seismic:1.0.2")
}
