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
  `java-library`
  kotlin("jvm")
}

apply(from = rootProject.file(".buildscript/configure-maven-publish.gradle"))

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
  compileOnly(get("androidx.annotations"))
  compileOnly(get("annotations.intellij"))

  api(project(":legacy:legacy-workflow-core"))
  // For Snapshot.
  api(project(":workflow-core"))
  api(get("kotlin.stdLib.jdk6"))
  api(get("kotlin.coroutines.core"))
  api(get("okio"))
  api(get("rxjava2.rxjava2"))

  implementation(get("kotlin.coroutines.rx2"))

  testImplementation(project(":internal-testing-utils"))
  testImplementation(get("kotlin.test.jdk"))
  testImplementation(get("kotlin.test.mockito"))
  testImplementation(get("test.hamcrestCore"))
  testImplementation(get("test.junit"))
  testImplementation(get("test.truth"))
  testImplementation(get("test.mockito"))
  testImplementation(get("rxjava2.extensions"))
}
