/*
 * Copyright 2020 Square Inc.
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

import Workflow
import WorkflowUI

// MARK: Input and Output

/// This is a stateless workflow. It only used the properties sent from its parent to render a result.
struct ReversingWorkflow: Workflow {
    typealias Rendering = String
    typealias Output = Never
    typealias State = Void

    var text: String
}

// MARK: Rendering

extension ReversingWorkflow {
    func render(state: ReversingWorkflow.State, context: RenderContext<ReversingWorkflow>) -> String {
        return String(text.reversed())
    }
}
