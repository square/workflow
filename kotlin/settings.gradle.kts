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
rootProject.name = "workflow"

include(
    ":internal-testing-utils",
    ":legacy:legacy-workflow-core",
    ":legacy:legacy-workflow-rx2",
    ":legacy:legacy-workflow-test",
    ":samples:containers:app-poetry",
    ":samples:containers:app-raven",
    ":samples:containers:android",
    ":samples:containers:common",
    ":samples:containers:hello-back-button",
    ":samples:containers:poetry",
    ":samples:dungeon:app",
    ":samples:dungeon:common",
    ":samples:dungeon:timemachine",
    ":samples:dungeon:timemachine-shakeable",
    ":samples:hello-terminal:hello-terminal-app",
    ":samples:hello-terminal:terminal-workflow",
    ":samples:hello-terminal:todo-terminal-app",
    ":samples:hello-workflow",
    ":samples:hello-workflow-fragment",
    ":samples:recyclerview",
    ":samples:tictactoe:app",
    ":samples:tictactoe:common",
    ":samples:todo-android:app",
    ":samples:todo-android:common",
    ":trace-encoder",
    ":workflow-core",
    ":workflow-runtime",
    ":workflow-rx2",
    ":workflow-testing",
    ":workflow-tracing",
    ":workflow-ui:backstack-common",
    ":workflow-ui:backstack-android",
    ":workflow-ui:core-common",
    ":workflow-ui:core-android",
    ":workflow-ui:modal-common",
    ":workflow-ui:modal-android"
)
