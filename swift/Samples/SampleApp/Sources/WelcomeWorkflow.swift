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

struct WelcomeWorkflow: Workflow {
    enum Output {
        case login(name: String)
    }
}

// MARK: State and Initialization

extension WelcomeWorkflow {
    struct State {
        var name: String
    }

    func makeInitialState() -> WelcomeWorkflow.State {
        return State(name: "")
    }

    func workflowDidChange(from previousWorkflow: WelcomeWorkflow, state: inout State) {}
}

// MARK: Actions

extension WelcomeWorkflow {
    enum Action: WorkflowAction {
        typealias WorkflowType = WelcomeWorkflow

        case nameChanged(String)
        case login

        func apply(toState state: inout WelcomeWorkflow.State) -> WelcomeWorkflow.Output? {
            switch self {
            case let .nameChanged(updatedName):
                state.name = updatedName
                return nil

            case .login:
                return .login(name: state.name)
            }
        }
    }
}

// MARK: Rendering

extension WelcomeWorkflow {
    typealias Rendering = WelcomeScreen

    func render(state: WelcomeWorkflow.State, context: RenderContext<WelcomeWorkflow>) -> Rendering {
        let sink = context.makeSink(of: Action.self)
        return WelcomeScreen(
            name: state.name,
            onNameChanged: { updatedName in
                sink.send(.nameChanged(updatedName))
            },
            onLoginTapped: {
                sink.send(.login)
            }
        )
    }
}
