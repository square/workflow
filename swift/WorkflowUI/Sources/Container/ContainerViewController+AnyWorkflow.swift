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

#if canImport(UIKit)

import Foundation
import Workflow

extension ContainerViewController {
    public convenience init<W: AnyWorkflowConvertible>(
        workflow: W,
        rootViewEnvironment: ViewEnvironment = .empty
    ) where W.Rendering == ScreenType, W.Output == Output {
        self.init(workflow: WrapperWorkflow(workflow), rootViewEnvironment: rootViewEnvironment)
    }
}

fileprivate struct WrapperWorkflow<Rendering, Output>: Workflow {
    typealias State = Void
    typealias Output = Output
    typealias Rendering = Rendering

    var wrapped: AnyWorkflow<Rendering, Output>

    init<W: AnyWorkflowConvertible>(_ wrapped: W) where W.Output == Output, W.Rendering == Rendering {
        self.wrapped = wrapped.asAnyWorkflow()
    }

    func render(state: State, context: RenderContext<WrapperWorkflow>) -> Rendering {
        return wrapped
            .mapOutput { AnyWorkflowAction(sendingOutput: $0) }
            .rendered(with: context)
    }
}

#endif
