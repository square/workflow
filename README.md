# Square Workflow

[![GitHub license](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg?style=flat)](https://www.apache.org/licenses/LICENSE-2.0)

Workflow is an application framework that provides architectural primitives.

Workflow is:

* Written in and used for Kotlin and Swift
* A unidirectional data flow library that uses immutable data within each Workflow.
  Data flows in a single direction from source to UI, and events in a single direction
  from the UI to the business logic.
* A library that supports writing business logic and complex UI navigation logic as
  state machines, thereby enabling confident reasoning about state and validation of
  correctness.
* Optimized for composability and scalability of features and screens.
* Corresponding UI frameworks that bind Rendering data classes for “views”
  (including event callbacks) to Mobile UI frameworks for Android and iOS.
* A corresponding testing framework that facilitates simple-to-write unit
  tests for all application business logic and helps ensure correctness.

_**This project is currently in development and the API subject to breaking changes without notice.**
Follow Square's engineering blog, [The Corner](https://developer.squareup.com/blog/), to see when
this project becomes stable._

While the API is not yet stable, this code is in heavy production use in Android and iOS
apps with millions of users.

## Using Workflows in your project

### Swift

See the [square/workflow-swift](https://github.com/square/workflow-swift) repository.

### Kotlin

See the [square/workflow-kotlin](https://github.com/square/workflow-kotlin) repository.

## Resources

* The website contains a [User Guide](https://square.github.io/workflow/userguide/concepts/)
  describing core concepts.
* For Kotlin (and Android), there is a codelab style
  [tutorial](https://github.com/square/workflow-kotlin/tree/main/samples/tutorial) in the repo.
* For Swift (and iOS), there is also a Getting Started
  [tutorial](https://github.com/square/workflow-swift/tree/main/Samples/Tutorial) in the repo.
* There are also a number of
  [Kotlin samples](https://github.com/square/workflow-kotlin/tree/main/samples)
  and [Swift samples](https://github.com/square/workflow-swift/tree/main/Samples).

### Legacy Resources and Presentations

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

Workflow discussion happens in the Workflow Community slack. Use this [open invitation](https://join.slack.com/t/workflow-community/shared_invite/zt-a2wc0ddx-4bvc1royeZ7yjGqEkW1CsQ).

Workflow maintainers also hang out in the [#squarelibraries](https://kotlinlang.slack.com/messages/C5HT9AL7Q)
channel on the [Kotlin Slack](https://surveys.jetbrains.com/s3/kotlin-slack-sign-up?_ga=2.93235285.916482233.1570572671-654176432.1527183673).

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
