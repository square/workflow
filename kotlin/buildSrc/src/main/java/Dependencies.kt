@file:JvmName("Deps")

import java.util.Locale.US
import kotlin.reflect.full.declaredMembers

object Versions {
  const val coroutines = "1.3.7"
  const val kotlin = "1.3.72"
  const val targetSdk = 29
}

@Suppress("unused")
object Dependencies {
  const val android_gradle_plugin = "com.android.tools.build:gradle:4.0.0"

  object AndroidX {
    const val activity = "androidx.activity:activity:1.1.0"
    const val annotations = "androidx.annotation:annotation:1.1.0"
    const val appcompat = "androidx.appcompat:appcompat:1.1.0"
    const val constraint_layout = "androidx.constraintlayout:constraintlayout:1.1.3"
    const val fragment = "androidx.fragment:fragment:1.2.3"
    const val gridlayout = "androidx.gridlayout:gridlayout:1.0.0"

    object Lifecycle {
      const val ktx = "androidx.lifecycle:lifecycle-runtime-ktx:2.2.0"
      const val reactivestreams = "androidx.lifecycle:lifecycle-reactivestreams-ktx:2.2.0"
    }

    // Note that we're not using the actual androidx material dep yet, it's still alpha.
    const val material = "com.google.android.material:material:1.1.0"
    const val recyclerview = "androidx.recyclerview:recyclerview:1.1.0"

    // Note that we are *not* using lifecycle-viewmodel-savedstate, which at this
    // writing is still in beta and still fixing bad bugs. Probably we'll never bother to,
    // it doesn't really add value for us.
    const val savedstate = "androidx.savedstate:savedstate:1.0.0"
    const val transition = "androidx.transition:transition:1.3.1"
    const val viewbinding = "androidx.databinding:viewbinding:3.6.2"
  }

  const val cycler = "com.squareup.cycler:cycler:0.1.3"

  // Required for Dungeon Crawler sample.
  const val desugar_jdk_libs = "com.android.tools:desugar_jdk_libs:1.0.5"
  const val moshi = "com.squareup.moshi:moshi:1.9.2"
  const val rxandroid2 = "io.reactivex.rxjava2:rxandroid:2.1.1"
  const val timber = "com.jakewharton.timber:timber:4.7.1"

  object Kotlin {
    const val binaryCompatibilityValidatorPlugin =
      "org.jetbrains.kotlinx:binary-compatibility-validator:0.2.3"
    const val gradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"

    object Stdlib {
      const val common = "org.jetbrains.kotlin:kotlin-stdlib-common"
      const val jdk8 = "org.jetbrains.kotlin:kotlin-stdlib-jdk8"
      const val jdk7 = "org.jetbrains.kotlin:kotlin-stdlib-jdk7"
      const val jdk6 = "org.jetbrains.kotlin:kotlin-stdlib"
    }

    object Coroutines {
      const val android = "org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.coroutines}"
      const val core = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}"
      const val rx2 = "org.jetbrains.kotlinx:kotlinx-coroutines-rx2:${Versions.coroutines}"

      const val test = "org.jetbrains.kotlinx:kotlinx-coroutines-test:${Versions.coroutines}"
    }

    const val moshi = "com.squareup.moshi:moshi-kotlin:1.9.2"
    const val reflect = "org.jetbrains.kotlin:kotlin-reflect:${Versions.kotlin}"

    object Serialization {
      const val gradlePlugin = "org.jetbrains.kotlin:kotlin-serialization:${Versions.kotlin}"
      const val runtime = "org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.20.0"
      const val kaml = "com.charleskorn.kaml:kaml:0.16.1"
    }

    object Test {
      const val common = "org.jetbrains.kotlin:kotlin-test-common"
      const val annotations = "org.jetbrains.kotlin:kotlin-test-annotations-common"
      const val jdk = "org.jetbrains.kotlin:kotlin-test-junit"

      const val mockito = "com.nhaarman:mockito-kotlin-kt1.1:1.6.0"
    }
  }

  const val dokka = "org.jetbrains.dokka:dokka-gradle-plugin:0.10.0"

  object Jmh {
    const val gradlePlugin = "me.champeau.gradle:jmh-gradle-plugin:0.5.0"
    const val core = "org.openjdk.jmh:jmh-core:1.23"
    const val generator = "org.openjdk.jmh:jmh-generator-annprocess:1.23"
  }

  const val mavenPublish = "com.vanniktech:gradle-maven-publish-plugin:0.11.1"
  const val ktlint = "org.jlleitschuh.gradle:ktlint-gradle:9.2.0"
  const val lanterna = "com.googlecode.lanterna:lanterna:3.0.2"
  const val detekt = "io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.0.1"
  const val okio = "com.squareup.okio:okio:2.5.0"

  object RxJava2 {
    const val rxjava2 = "io.reactivex.rxjava2:rxjava:2.2.19"

    const val extensions = "com.github.akarnokd:rxjava2-extensions:0.20.10"
  }

  object Annotations {
    const val intellij = "org.jetbrains:annotations:19.0.0"
  }

  object Test {
    object AndroidX {
      object Espresso {
        const val contrib = "androidx.test.espresso:espresso-contrib:3.2.0"
        const val core = "androidx.test.espresso:espresso-core:3.2.0"
        const val idlingResource = "androidx.test.espresso:espresso-idling-resource:3.2.0"
        const val intents = "androidx.test.espresso:espresso-intents:3.2.0"
      }

      const val junitExt = "androidx.test.ext:junit:1.1.1"
      const val runner = "androidx.test:runner:1.2.0"
      const val truthExt = "androidx.test.ext:truth:1.2.0"
      const val uiautomator = "androidx.test.uiautomator:uiautomator:2.2.0"
    }

    const val hamcrestCore = "org.hamcrest:hamcrest-core:2.2"
    const val junit = "junit:junit:4.13"
    const val mockito = "org.mockito:mockito-core:3.3.3"
    const val truth = "com.google.truth:truth:1.0.1"
  }
}

/**
 * Workaround to make [Dependencies] accessible from Groovy scripts. [path] is case-insensitive.
 *
 * ```
 * dependencies {
 *   implementation Deps.get("kotlin.stdlib.common")
 * }
 * ```
 */
@JvmName("get")
fun getDependencyFromGroovy(path: String): String = Dependencies.resolveObject(
    path.toLowerCase(US)
        .split(".")
)

private tailrec fun Any.resolveObject(pathParts: List<String>): String {
  require(pathParts.isNotEmpty())
  val klass = this::class

  if (pathParts.size == 1) {
    @Suppress("UNCHECKED_CAST")
    val member = klass.declaredMembers.single { it.name.toLowerCase(US) == pathParts.single() }
    return member.call() as String
  }

  val nestedKlasses = klass.nestedClasses
  val selectedKlass = nestedKlasses.single { it.simpleName!!.toLowerCase(US) == pathParts.first() }
  return selectedKlass.objectInstance!!.resolveObject(pathParts.subList(1, pathParts.size))
}
