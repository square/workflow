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

import ReactiveSwift
import Workflow
import WorkflowUI

// MARK: Input and Output

struct WelcomeWorkflow: Workflow {
    enum Output: Equatable {
        case didLogin(name: String)
    }
}

// MARK: State and Initialization

extension WelcomeWorkflow {
    struct State: Equatable {
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

        case nameChanged(name: String)
        case didLogin

        func apply(toState state: inout WelcomeWorkflow.State) -> WelcomeWorkflow.Output? {
            switch self {
            case let .nameChanged(name: name):
                // Update our state with the updated name.
                state.name = name
                // Return `nil` for the output, we want to handle this action only at the level of this workflow.
                return nil

            case .didLogin:
                if state.name.count != 0 {
                    // Return an output of `didLogin` with the name if it's not empty.
                    return .didLogin(name: state.name)
                } else {
                    // Don't log in if the name isn't filled in.
                    return nil
                }
            }
        }
    }
}

// MARK: Workers

extension WelcomeWorkflow {
    struct WelcomeWorker: Worker {
        enum Output {}

        func run() -> SignalProducer<Output, Never> {
            fatalError()
        }

        func isEquivalent(to otherWorker: WelcomeWorker) -> Bool {
            return true
        }
    }
}

// MARK: Rendering

extension WelcomeWorkflow {
    typealias Rendering = WelcomeScreen

    func render(state: WelcomeWorkflow.State, context: RenderContext<WelcomeWorkflow>) -> Rendering {
        // Create a "sink" of type `Action`. A sink is what we use to send actions to the workflow.
        let sink = context.makeSink(of: Action.self)

        return WelcomeScreen(
            name: state.name,
            onNameChanged: { name in
                sink.send(.nameChanged(name: name))
            },
            onLoginTapped: {
                // Whenever the login button is tapped, emit the `.didLogin` action.
                sink.send(.didLogin)
            }
        )
    }
}
