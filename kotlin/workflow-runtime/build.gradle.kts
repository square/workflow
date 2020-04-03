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
import me.champeau.gradle.JMHPluginExtension

plugins {
  kotlin("multiplatform")
  id("org.jetbrains.dokka")
  // Benchmark plugins.
  id("me.champeau.gradle.jmh")
  // If this plugin is not applied, IntelliJ won't see the JMH definitions for some reason.
  idea
}

apply(from = rootProject.file(".buildscript/configure-maven-publish.gradle"))

// Benchmark configuration.
configure<JMHPluginExtension> {
  include = listOf(".*")
  duplicateClassesStrategy = DuplicatesStrategy.WARN
}
configurations.named("jmhRuntime") {
  attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
}

kotlin {
  sourceSets {
    all {
      languageSettings.useExperimentalAnnotation("kotlin.RequiresOptIn")
    }

    commonMain {
      dependencies {
        compileOnly(Dependencies.Annotations.intellij)

        api(project(":workflow-core"))
        api(Dependencies.Kotlin.Stdlib.common)
        api(Dependencies.Kotlin.Coroutines.coreCommon)
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
    val main by compilations.getting {
      defaultSourceSet {
        dependencies {
          api(Dependencies.Kotlin.Stdlib.jdk6)
          api(Dependencies.Kotlin.Coroutines.core)
        }
      }
    }

    compilations["test"].defaultSourceSet {
      withJava()
      dependencies {
        implementation(Dependencies.Kotlin.Test.jdk)
        implementation(Dependencies.Kotlin.reflect)
      }
    }

    compilations["jmh"].defaultSourceSet {
      // Ideally we would just pass the friend-paths argument to the compiler, like we did
      // pre-multiplatform, but even if we configure the default task it doesn't seem to get
      // picked up. This causes warnings about duplicate class files, but the benchmarks do work.
      dependsOn(main.defaultSourceSet)

      dependencies {
        // These dependencies will be available on the classpath for source inside src/jmh.
        dependencies.add("jmh", Dependencies.Kotlin.Stdlib.jdk6)
        dependencies.add("jmh", Dependencies.Jmh.core)
        dependencies.add("jmh", Dependencies.Jmh.generator)
      }
    }
  }
}
