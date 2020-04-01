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
buildscript {
  dependencies {
    classpath Dep.get("android_gradle_plugin")
    classpath Dep.get("detekt")
    classpath Dep.get("dokka")
    classpath Dep.get("jmh.gradlePlugin")
    classpath Dep.get("kotlin.binaryCompatibilityValidatorPlugin")
    classpath Dep.get("kotlin.gradlePlugin")
    classpath Dep.get("kotlin.serialization.gradlePlugin")
    classpath Dep.get("ktlint")
    classpath Dep.get("mavenPublish")
  }

  repositories {
    mavenCentral()
    gradlePluginPortal()
    google()
    // For binary compatibility validator.
    maven { url "https://kotlin.bintray.com/kotlinx" }
  }
}

subprojects {
  repositories {
    google()
    mavenCentral()
    jcenter()
  }

  apply plugin: "org.jlleitschuh.gradle.ktlint"
  apply plugin: "io.gitlab.arturbosch.detekt"
  afterEvaluate { project ->
    project.tasks.findByName('check')?.dependsOn 'detekt'

    project.configurations.configureEach {
      // There could be transitive dependencies in tests with a lower version. This could cause
      // problems with a newer Kotlin version that we use.
      resolutionStrategy.force Dep.get("kotlin.reflect")
    }
  }

  tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile).all {
    kotlinOptions {
      kotlinOptions.allWarningsAsErrors = true

      jvmTarget = "1.8"

      // Don't panic, all this does is allow us to use the @OptIn meta-annotation.
      // to define our own experiments.
      freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
    }
  }

  // Configuration documentation: https://github.com/JLLeitschuh/ktlint-gradle#configuration
  ktlint {
    // Prints the name of failed rules.
    verbose = true
    reporters {
      // Default "plain" reporter is actually harder to read.
      reporter "json"
    }

    disabledRules = [
        // IntelliJ refuses to sort imports correctly.
        // This is a known issue: https://github.com/pinterest/ktlint/issues/527
        "import-ordering",
    ]
  }
}

apply from: rootProject.file('.buildscript/binary-validation.gradle')

// This is intentionally *not* applied to subprojects. When building subprojects' kdoc for maven
// javadocs artifacts, we want to use the default config. This config is for the
// statically-generated documentation site.
apply plugin: 'org.jetbrains.dokka'
repositories {
  // Dokka is not in Maven Central.
  jcenter()
}
tasks.register('siteDokka', org.jetbrains.dokka.gradle.DokkaTask) {
  description = 'Generate dokka Github-flavored Markdown for documentation site.'
  // TODO(#1065) Make this task depend on assembling all subprojects.

  outputDirectory = "$rootDir/../docs/kotlin/api"
  outputFormat = 'gfm'

  // TODO(#1064) Generate this list automatically.
  // These can't be absolute paths, they can only be the leaf project name.
  subProjects = [
      'backstack-android',
      'backstack-common',
      'core-android',
      'core-common',
      'core-compose',
      'legacy-workflow-core',
      'legacy-workflow-rx2',
      'legacy-workflow-test',
      'modal-android',
      'modal-common',
      'trace-encoder',
      'workflow-core',
      'workflow-runtime',
      'workflow-rx2',
      'workflow-testing',
      'workflow-tracing',
  ]

  configuration {
    reportUndocumented = false
    skipDeprecated = true
    jdkVersion = 8

    // TODO(#1063) Add source links using same automated process as subProjects.

    perPackageOption {
      prefix = "com.squareup.workflow.internal"
      suppress = true
    }
  }
}
