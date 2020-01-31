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
import Workflow
import WorkflowUI
import BackStackContainer


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

    func workflowDidChange(from previousWorkflow: MainWorkflow, state: inout State) {

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
            case .authenticated(sessionToken: let sessionToken):
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

    typealias Rendering = PaddingScreen<BackStackScreen>

    func render(state: MainWorkflow.State, context: RenderContext<MainWorkflow>) -> Rendering {

        switch state {
        case .authenticating:
            let authenticationBackStackItems = AuthenticationWorkflow(
                authenticationService: AuthenticationService())
                .mapOutput({ output -> Action in
                    switch output {
                    case .authorized(session: let sessionToken):
                        return .authenticated(sessionToken: sessionToken)
                    }
                })
                .rendered(with: context)

            return PaddingScreen(BackStackScreen(items: authenticationBackStackItems))

        case .runningGame:
            return PaddingScreen(RunGameWorkflow().rendered(with: context))
        }

    }
}
