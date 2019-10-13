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
import BackStackContainer
import Workflow
import WorkflowUI
import ReactiveSwift


// MARK: Input and Output

struct AuthenticationWorkflow: Workflow {
    var authenticationService: AuthenticationService

    enum Output {
        case authorized(session: String)
    }
}


// MARK: State and Initialization

extension AuthenticationWorkflow {

    enum State {
        case emailPassword(AuthenticationService.AuthenticationError?)
        case authorizingEmailPassword(email: String, password: String)
        case twoFactor(intermediateSession: String, authenticationError: AuthenticationService.AuthenticationError?)
        case authorizingTwoFactor(twoFactorCode: String, intermediateSession: String)
    }

    func makeInitialState() -> AuthenticationWorkflow.State {
        return .emailPassword(nil)
    }

    func workflowDidChange(from previousWorkflow: AuthenticationWorkflow, state: inout State) {

    }
}


// MARK: Actions

extension AuthenticationWorkflow {

    enum Action: WorkflowAction {

        typealias WorkflowType = AuthenticationWorkflow

        case back
        case login(email: String, password: String)
        case verifySecondFactor(intermediateSession: String, twoFactorCode: String)
        case authenticationSucceeded(response: AuthenticationService.AuthenticationResponse)
        case authenticationError(AuthenticationService.AuthenticationError)

        func apply(toState state: inout AuthenticationWorkflow.State) -> AuthenticationWorkflow.Output? {

            switch self {

            case .back:
                switch state {
                case .twoFactor:
                    state = .emailPassword(nil)

                default:
                    fatalError("Unexpected back in state \(state)")
                }

            case .login(email: let email, password: let password):
                state = .authorizingEmailPassword(email: email, password: password)

            case .verifySecondFactor(intermediateSession: let intermediateSession, twoFactorCode: let twoFactorCode):
                state = .authorizingTwoFactor(twoFactorCode: twoFactorCode, intermediateSession: intermediateSession)

            case .authenticationSucceeded(response: let response):
                if response.secondFactorRequired {
                    state = .twoFactor(intermediateSession: response.token, authenticationError: nil)
                } else {
                    return .authorized(session: response.token)
                }

            case .authenticationError(let error):
                switch state {
                case .authorizingEmailPassword:
                    state = .emailPassword(error)
                case .authorizingTwoFactor(twoFactorCode: _, intermediateSession: let intermediateSession):
                    state = .twoFactor(intermediateSession: intermediateSession, authenticationError: error)

                default:
                    fatalError("Unexpected authentication error in state \(state)")
                }

            }
            return nil
        }
    }
}


// MARK: Workers

extension AuthenticationWorkflow {

    struct AuthorizingEmailPasswordWorker: Worker {

        typealias Output = Action

        var authenticationService: AuthenticationService
        var email: String
        var password: String

        func run() -> SignalProducer<Output, Never> {
            return authenticationService
                .login(email: email, password: password)
                .map({ response -> Action in
                    return .authenticationSucceeded(response: response)
                })
                .flatMapError {
                    SignalProducer(value: .authenticationError($0))
                }
        }

        func isEquivalent(to otherWorker: AuthorizingEmailPasswordWorker) -> Bool {
            return email == otherWorker.email
            && password == otherWorker.password
        }

    }

    struct AuthorizingTwoFactorWorker: Worker {

        typealias Output = Action

        var authenticationService: AuthenticationService
        var intermediateToken: String
        var twoFactorCode: String

        func run() -> SignalProducer<Output, Never> {
            return authenticationService
                .secondFactor(
                    token: intermediateToken,
                    secondFactor: twoFactorCode)
                .map {
                    .authenticationSucceeded(response: $0)
                }
                .flatMapError {
                    SignalProducer(value: .authenticationError($0))
            }
        }

        func isEquivalent(to otherWorker: AuthenticationWorkflow.AuthorizingTwoFactorWorker) -> Bool {
            return intermediateToken == otherWorker.intermediateToken
            && twoFactorCode == otherWorker.twoFactorCode
        }
    }

}

// MARK: Rendering

extension AuthenticationWorkflow {

    typealias Rendering = [BackStackScreen.Item]

    func render(state: AuthenticationWorkflow.State, context: RenderContext<AuthenticationWorkflow>) -> Rendering {
        let sink = context.makeSink(of: Action.self)

        var backStackItems: [BackStackScreen.Item] = []

        let authenticationError: AuthenticationService.AuthenticationError?

        if case let .emailPassword(error) = state {
            authenticationError = error
        } else {
            authenticationError = nil
        }

        let loginScreen = LoginWorkflow(error: authenticationError).mapOutput({ output -> Action in
            switch output {
            case .login(email: let email, password: let password):
                return .login(email: email, password: password)
            }
        }).rendered(with: context)
        backStackItems.append(BackStackScreen.Item(screen: loginScreen, barVisibility: .hidden))

        switch state {
        case .emailPassword:
            break

        case .authorizingEmailPassword(email: let email, password: let password):
            context.awaitResult(for: AuthorizingEmailPasswordWorker(
                authenticationService: authenticationService,
                email: email,
                password: password))

            backStackItems.append(BackStackScreen.Item(screen: LoadingScreen(), barVisibility: .hidden))

        case .twoFactor(intermediateSession: let intermediateSession, authenticationError: let authenticationError):
            backStackItems.append(twoFactorScreen(
                error: authenticationError,
                intermediateSession: intermediateSession,
                sink: sink))

        case .authorizingTwoFactor(twoFactorCode: let twoFactorCode, intermediateSession: let intermediateSession):

            context.awaitResult(
                for: AuthorizingTwoFactorWorker(
                    authenticationService: authenticationService,
                    intermediateToken: intermediateSession,
                    twoFactorCode: twoFactorCode))

            backStackItems.append(twoFactorScreen(error: nil, intermediateSession: intermediateSession, sink: sink))
            backStackItems.append(BackStackScreen.Item(screen: LoadingScreen(), barVisibility: .hidden))
        }

        return backStackItems
    }

    private func twoFactorScreen(error: AuthenticationService.AuthenticationError?, intermediateSession: String, sink: Sink<Action>) -> BackStackScreen.Item {
        let title: String
        if let authenticationError = error {
            title = authenticationError.localizedDescription
        } else {
            title = "Enter the one time code to continue"
        }

        let twoFactorScreen = TwoFactorScreen(
            title: title,
            onLoginTapped: { twoFactorCode in
                sink.send(.verifySecondFactor(
                    intermediateSession: intermediateSession,
                    twoFactorCode: twoFactorCode))
        })

        return BackStackScreen.Item(
            screen: twoFactorScreen,
            barVisibility: .visible(BackStackScreen.BarContent(
                leftItem: BackStackScreen.BarContent.BarButtonItem.button(BackStackScreen.BarContent.Button(
                    content: .text("Cancel"),
                    handler: {
                        sink.send(.back)
                })))))
    }
}
