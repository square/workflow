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
apply plugin: 'com.android.library'
apply plugin: 'kotlin-android'

sourceCompatibility = JavaVersion.VERSION_1_8
targetCompatibility = JavaVersion.VERSION_1_8

apply from: rootProject.file('.buildscript/configure-android-defaults.gradle')

dependencies {
  api project(':workflow-core')
  api project(':workflow-ui:backstack-android')
  api project(':workflow-ui:modal-android')
  api project(':samples:containers:common')

  api Dep.get("androidx.transition")
  api Dep.get("kotlin.stdLib.jdk6")
  api Dep.get("rxjava2.rxjava2")

  implementation project(':workflow-runtime')
  implementation Dep.get("androidx.appcompat")
  implementation Dep.get("androidx.lifecycle.reactivestreams")
  implementation Dep.get("androidx.savedstate")
  implementation Dep.get("kotlin.coroutines.android")
  implementation Dep.get("kotlin.coroutines.core")
  implementation Dep.get("kotlin.coroutines.rx2")

  testImplementation Dep.get("test.junit")
  testImplementation Dep.get("test.truth")
  testImplementation Dep.get("kotlin.coroutines.test")
}
