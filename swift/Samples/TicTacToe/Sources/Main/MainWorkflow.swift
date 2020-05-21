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

import AlertContainer
import BackStackContainer
import ModalContainer
import Workflow
import WorkflowUI

// MARK: Input and Output

struct MainWorkflow: Workflow {
    typealias Output = Never
}

// MARK: State and Initialization

extension MainWorkflow {
    enum State {
        case authenticating
        case runningGame(sessionToken: String)
    }

    func makeInitialState() -> MainWorkflow.State {
        return .authenticating
    }
}

// MARK: Actions

extension MainWorkflow {
    enum Action: WorkflowAction {
        typealias WorkflowType = MainWorkflow

        case authenticated(sessionToken: String)
        case logout

        func apply(toState state: inout MainWorkflow.State) -> MainWorkflow.Output? {
            switch self {
            case let .authenticated(sessionToken: sessionToken):
                state = .runningGame(sessionToken: sessionToken)

            case .logout:
                state = .authenticating
            }

            return nil
        }
    }
}

// MARK: Rendering

extension MainWorkflow {
    typealias Rendering = AlertContainerScreen<ModalContainerScreen<BackStackScreen<AnyScreen>>>

    func render(state: MainWorkflow.State, context: RenderContext<MainWorkflow>) -> Rendering {
        switch state {
        case .authenticating:
            return AuthenticationWorkflow(authenticationService: AuthenticationService())
                .mapOutput { output -> Action in
                    switch output {
                    case let .authorized(session: sessionToken):
                        return .authenticated(sessionToken: sessionToken)
                    }
                }
                .rendered(with: context)

        case .runningGame:
            return RunGameWorkflow().rendered(with: context)
        }
    }
}
