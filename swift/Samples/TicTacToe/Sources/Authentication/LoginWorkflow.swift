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
import ReactiveSwift

// MARK: Input and Output

struct LoginWorkflow: Workflow {
    var error: AuthenticationService.AuthenticationError?

    enum Output {
        case login(email: String, password: String)
    }
}


// MARK: State and Initialization

extension LoginWorkflow {

    struct State {
        var email: String
        var password: String
    }

    func makeInitialState() -> LoginWorkflow.State {
        return State(email: "", password: "")
    }

    func workflowDidChange(from previousWorkflow: LoginWorkflow, state: inout State) {

    }
}


// MARK: Actions

extension LoginWorkflow {

    enum Action: WorkflowAction {

        typealias WorkflowType = LoginWorkflow

        case emailUpdated(String)
        case passwordUpdated(String)
        case login

        func apply(toState state: inout LoginWorkflow.State) -> LoginWorkflow.Output? {

            switch self {
            case .emailUpdated(let email):
                state.email = email

            case .passwordUpdated(let password):
                state.password = password

            case .login:
                return .login(email: state.email, password: state.password)
            }

            return nil
        }
    }
}


// MARK: Rendering

extension LoginWorkflow {

    typealias Rendering = LoginScreen

    func render(state: LoginWorkflow.State, context: RenderContext<LoginWorkflow>) -> Rendering {
        let sink = context.makeSink(of: Action.self)

        let title: String
        if let authenticationError = error {
            title = authenticationError.localizedDescription
        } else {
            title = "Welcome! Please log in to play TicTacToe!"
        }

        return LoginScreen(
            title: title,
            email: state.email,
            onEmailChanged: { email in
                sink.send(.emailUpdated(email))
            },
            password: state.password,
            onPasswordChanged: { password in
                sink.send(.passwordUpdated(password))
            },
            onLoginTapped: {
                sink.send(.login)
            })

    }
}
