import me.champeau.gradle.JMHPluginExtension
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

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
  `java-library`
  kotlin("jvm")
  id("org.jetbrains.dokka")
  // Benchmark plugins.
  id("me.champeau.gradle.jmh")
  // If this plugin is not applied, IntelliJ won't see the JMH definitions for some reason.
  idea
}

java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

apply(from = rootProject.file(".buildscript/configure-maven-publish.gradle"))

// Benchmark configuration.
configure<JMHPluginExtension> {
  include = listOf(".*")
  duplicateClassesStrategy = DuplicatesStrategy.WARN
}
configurations.named("jmh") {
  attributes.attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
}
tasks.named<KotlinCompile>("compileJmhKotlin") {
  kotlinOptions {
    // Give the benchmark code access to internal definitions.
    val compileKotlin: KotlinCompile by tasks
    freeCompilerArgs += "-Xfriend-paths=${compileKotlin.destinationDir}"
  }
}

dependencies {
  compileOnly(get("annotations.intellij"))

  api(project(":workflow-core"))
  api(get("kotlin.stdLib.jdk6"))
  api(get("kotlin.coroutines.core"))

  testImplementation(get("kotlin.test.jdk"))
  testImplementation(get("kotlin.reflect"))

  // These dependencies will be available on the classpath for source inside src/jmh.
  "jmh"(get("kotlin.stdLib.jdk6"))
  "jmh"(get("jmh.core"))
  "jmh"(get("jmh.generator"))
}
