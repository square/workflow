# workflow

[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](https://www.apache.org/licenses/LICENSE-2.0)
[![CocoaPods compatible](https://img.shields.io/cocoapods/v/Workflow.svg)](https://cocoapods.org/pods/Workflow)
[![Maven Central](https://img.shields.io/maven-central/v/com.squareup.workflow/workflow-core-jvm.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.squareup.workflow%22)

A unidirectional data flow library for Kotlin and Swift, emphasizing:

* Strong support for state-machine driven UI and navigation.
* Composition and scaling.
* Effortless separation of business and UI concerns.

_**This project is currently experimental and the API subject to breaking changes without notice.**
Follow Square's engineering blog, [The Corner](https://developer.squareup.com/blog/), to see when
this project becomes stable._

While the API is not yet stable, this code is in heavy production use in Android and iOS
apps with millions of users.

<iframe title="vimeo-player" src="https://player.vimeo.com/video/362741019" width="640" height="360"
frameborder="0" allowfullscreen></iframe>

## Using Workflows in your project

### Swift

#### Swift Package Manager

[![SwiftPM compatible](https://img.shields.io/badge/SwiftPM-compatible-orange.svg)](#swift-package-manager)

If you are developing your own package, be sure that Workflow is included in `dependencies` 
in `Package.swift`:

```swift
dependencies: [
    .package(url: "git@github.com:square/workflow.git", from: "0.21.1")
]
```

In Xcode 11+, add Workflow directly as a dependency to your project with 
`File` > `Swift Packages` > `Add Package Dependency...`. Provide the git URL when prompted: `git@github.com:square/workflow.git`.

#### Cocoapods

[![CocoaPods compatible](https://img.shields.io/cocoapods/v/Workflow.svg)](https://cocoapods.org/pods/Workflow)

If you use CocoaPods to manage your dependencies, simply add Workflow and WorkflowUI to your
Podfile:

```ruby
pod 'Workflow'
pod 'WorkflowUI'
```

### Kotlin

[![Maven Central](https://img.shields.io/maven-central/v/com.squareup.workflow/workflow-core-jvm.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.squareup.workflow%22)

#### Quick Start

To get started with a fresh, barebones, Workflow-based Android app, we've created a template
repository – just click "Use this template" to create a new repository with a simple but runnable
app: [github.com/square/workflow-android-template](https://github.com/square/workflow-android-template)

#### Maven Artifacts

Artifacts are hosted on Maven Central. If you're using Gradle, ensure `mavenCentral()` appears in
your `repositories` block, and then add dependencies on the following artifacts:

<table>
  <tr>
    <th>Maven Coordinates</th>
    <th>Depend on this if…</th>
  </tr>
  <tr>
    <td nowrap><code>com.squareup.workflow:workflow-core-jvm:x.y.z</code></td>
    <td>You are writing a library module/project that uses Workflows, but you don't need to interact
    with the runtime from the outside.</td>
  </tr>
  <tr>
    <td nowrap><code>com.squareup.workflow:workflow-rx2:x.y.z</code></td>
    <td>You need to interact with RxJava2 from your Workflows.</td>
  </tr>
  <tr>
    <td nowrap><code>com.squareup.workflow:workflow-testing-jvm:x.y.z</code></td>
    <td>You are writing tests. This should only be included as a test dependency.</td>
  </tr>
  <tr>
    <td nowrap><code>com.squareup.workflow:workflow-ui-android:x.y.z</code></td>
    <td>You're writing an Android app that uses Workflows.</td>
  </tr>
</table>

#### Lower-level Artifacts

Most code shouldn't need to depend on these directly. They should generally only be used to build
higher-level integrations with UI frameworks.

<table>
  <tr>
    <th>Maven Coordinates</th>
    <th>Depend on this if…</th>
  </tr>
  <tr>
    <td nowrap><code>com.squareup.workflow:workflow-runtime-jvm:x.y.z</code></td>
    <td>You need to interact directly with the runtime, i.e. streams of renderings and outputs.</td>
  </tr>
  <tr>
    <td nowrap><code>com.squareup.workflow:workflow-ui-core-jvm:x.y.z</code></td>
    <td>You are writing workflow-ui-android for another UI framework. Defines the core types used by
    that artifact.</td>
  </tr>
</table>

## Resources

* [Square Workflow – Droidcon NYC 2019](https://www.droidcon.com/media-detail?video=362741019) ([slides](https://docs.google.com/presentation/d/19-DkVCn-XawssyHQ_cboIX_s-Lf6rNg-ryAehA9xBVs))
* [SF Android GDG @ Square 2019 - Hello Workflow](https://www.youtube.com/watch?v=8PlYtfsgDKs)
  (live coding)
* [Android Dialogs 5-part Coding Series](https://twitter.com/chiuki/status/1100810374410956800)
  * [1](https://www.youtube.com/watch?v=JJ4-8AR5HhA),
    [2](https://www.youtube.com/watch?v=XB6frWBGvp0),
    [3](https://www.youtube.com/watch?v=NdFJMkT-t3c),
    [4](https://www.youtube.com/watch?v=aRxmyO6fwSs),
    [5](https://www.youtube.com/watch?v=aKaZa-1KN2M)
* [Reactive Workflows a Year Later – Droidcon NYC 2018](https://www.youtube.com/watch?v=cw9ZF9-ilac)
* [The Reactive Workflow Pattern – Fragmented Podcast](https://www.youtube.com/watch?v=mUBXgYnT7w0)
* [The Reactive Workflow Pattern Update – Droidcon SF 2017](https://www.youtube.com/watch?v=mvBVkU2mCF4)
* [The Rx Workflow Pattern – Droidcon NYC 2017](https://www.youtube.com/watch?v=KjoMnsc2lPo)
  ([slides](https://speakerdeck.com/rjrjr/reactive-workflows))

### Support & Contact

Workflow maintainers hang out in the [#squarelibraries](https://kotlinlang.slack.com/messages/C5HT9AL7Q)
channel on the [Kotlin Slack](https://surveys.jetbrains.com/s3/kotlin-slack-sign-up?_ga=2.93235285.916482233.1570572671-654176432.1527183673)
and the [#square-libraries-wtf](https://androidstudygroup.slack.com/messages/C03NYGB45) channel on
the Android Study Group Slack.

## Releasing and Deploying

See [RELEASING.md](RELEASING.md).

## License

<pre>
Copyright 2019 Square Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
</pre>
