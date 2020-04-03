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
  kotlin("multiplatform")
  id("org.jetbrains.dokka")
}

apply(from = rootProject.file(".buildscript/configure-maven-publish.gradle"))

kotlin {
  sourceSets {
    all {
      languageSettings.useExperimentalAnnotation("kotlin.RequiresOptIn")
    }

    commonMain {
      dependencies {
        compileOnly(Dependencies.Annotations.intellij)

        api(Dependencies.Kotlin.Stdlib.common)
        api(Dependencies.Kotlin.Coroutines.coreCommon)
        // For Snapshot.
        api(Dependencies.okioMultiplatform)
      }
    }

    commonTest {
      dependencies {
        implementation(Dependencies.Kotlin.Test.common)
        implementation(Dependencies.Kotlin.Test.annotations)
      }
    }
  }

  jvm {
    compilations["main"].defaultSourceSet {
      dependencies {
        compileOnly(Dependencies.Annotations.intellij)

        api(Dependencies.Kotlin.Stdlib.jdk6)
        api(Dependencies.Kotlin.Coroutines.core)
      }
    }
    compilations["test"].defaultSourceSet {
      dependencies {
        // Needed to run the tests on JVM, even if all tests are defined in common.
        implementation(Dependencies.Kotlin.Test.jdk)
      }
    }
  }
}
