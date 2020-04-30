# workflow-ui-core-compose

This module provides experimental support for [Jetpack Compose UI][1] with workflows.

The only integration that is currently supported is the ability to define [ViewFactories][2] that
are implemented as `@Composable` functions. See the `hello-compose-binding` sample in `samples` for
an example of how to use.

----

## Pre-Alpha

**DO NOT USE this module in your production apps!**

Jetpack Compose is in pre-alpha, developer preview stage. The API is incomplete and changes
_very frequently_. This integration module exists as a proof-of-concept, to show what's possible,
and to experiment with various ways to integrate Compose with Workflow.

----

## Usage

To get started, you must be using the latest Android Gradle Plugin 4.x version. Then, you need to
enable Compose support in your `build.gradle`:

```groovy
android {
  buildFeatures {
    compose true
  }
  composeOptions {
    kotlinCompilerVersion "1.3.70-dev-withExperimentalGoogleExtensions-20200424"
    kotlinCompilerExtensionVersion "${compose_version}"
  }
}
```

You may also need to set the Kotlin API version to 1.3:

```groovy
tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile).configureEach {
  kotlinOptions.apiVersion = "1.3"
}
```

To create a `ViewFactory`, call `bindCompose`. The lambda passed to `bindCompose` is a `@Composable`
function.

```kotlin
val HelloBinding = bindCompose<MyRendering> { rendering ->
  MaterialTheme {
    Clickable(onClick = { rendering.onClick() }) {
      Text(rendering.message)
    }
  }
}
```

The `bindCompose` function returns a regular [`ViewFactory`][2] which can be added to a
[`ViewRegistry`][3] like any other:

```kotlin
val viewRegistry = ViewRegistry(HelloBinding)
```

[1]: https://developer.android.com/jetpack/compose
[2]: https://square.github.io/workflow/kotlin/api/workflow/com.squareup.workflow.ui/-view-factory/
[3]: https://square.github.io/workflow/kotlin/api/workflow/com.squareup.workflow.ui/-view-registry/
