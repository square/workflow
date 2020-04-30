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
import WorkflowTesting
import XCTest

@testable import Development_SampleTicTacToe

class AuthenticationWorkflowTests: XCTestCase {
    // MARK: Action Tests

    func test_action_back() {
        AuthenticationWorkflow
            .Action
            .tester(withState: .twoFactor(intermediateSession: "test", authenticationError: nil))
            .send(action: .back)
            .assertState { state in
                XCTAssertEqual(state, .emailPassword)
            }
    }

    func test_action_login() {
        AuthenticationWorkflow
            .Action
            .tester(withState: .emailPassword)
            .send(action: .login(email: "reza@example.com", password: "password"))
            .assertState { state in
                if case let .authorizingEmailPassword(email, password) = state {
                    XCTAssertEqual(email, "reza@example.com")
                    XCTAssertEqual(password, "password")
                } else {
                    XCTFail("Unexpected emailPassword in state \(state)")
                }
            }
    }

    func test_action_verifySecondFactor() {
        AuthenticationWorkflow
            .Action
            .tester(withState: .emailPassword)
            .send(
                action: .verifySecondFactor(
                    intermediateSession: "intermediateSession",
                    twoFactorCode: "twoFactorCode"
                )
            )
            .assertState { state in
                if case let .authorizingTwoFactor(twoFactorCode, intermediateSession) = state {
                    XCTAssertEqual(intermediateSession, "intermediateSession")
                    XCTAssertEqual(twoFactorCode, "twoFactorCode")
                } else {
                    XCTFail("Unexpected verifySecondFactor in state \(state)")
                }
            }
    }

    func test_action_authenticationSucceeded() {
        AuthenticationWorkflow
            .Action
            .tester(withState: .emailPassword)
            .send(
                action: .authenticationSucceeded(
                    response: AuthenticationService.AuthenticationResponse(
                        token: "token",
                        secondFactorRequired: true
                    )
                )
            )
            .assertState { state in
                if case let .twoFactor(intermediateSession, authenticationError) = state {
                    XCTAssertEqual(intermediateSession, "token")
                    XCTAssertNil(authenticationError)
                } else {
                    XCTFail("Unexpected authenticationSucceeded in state \(state)")
                }
            }

        AuthenticationWorkflow
            .Action
            .tester(withState: .emailPassword)
            .send(
                action: .authenticationSucceeded(
                    response: AuthenticationService.AuthenticationResponse(
                        token: "token",
                        secondFactorRequired: false
                    )
                ), outputAssertions: { output in
                    XCTAssertNotNil(output)
                    switch output! {
                    case let .authorized(session: session):
                        XCTAssertEqual(session, "token")
                    }
                }
            )
            .assertState { state in
                XCTAssertEqual(state, .emailPassword)
            }
    }

    func test_action_dismissAuthenticationAlert() {
        AuthenticationWorkflow
            .Action
            .tester(
                withState: .authorizingEmailPassword(
                    email: "reza@example.com",
                    password: "password"
                )
            )
            .send(
                action: .authenticationError(AuthenticationService.AuthenticationError.invalidUserPassword)
            )
            .assertState { state in
                if case let .authenticationErrorAlert(error) = state {
                    XCTAssertNotNil(error)
                    XCTAssertEqual(error, AuthenticationService.AuthenticationError.invalidUserPassword)
                } else {
                    XCTFail("Unexpected authenticationError in state \(state)")
                }
            }
            .send(action: .dismissAuthenticationAlert)
            .assertState { state in
                XCTAssertEqual(state, .emailPassword)
            }
    }

    func test_action_authenticationError() {
        AuthenticationWorkflow
            .Action
            .tester(
                withState: .authorizingEmailPassword(
                    email: "reza@example.com",
                    password: "password"
                )
            )
            .send(
                action: .authenticationError(AuthenticationService.AuthenticationError.invalidUserPassword)
            )
            .assertState { state in
                if case let .authenticationErrorAlert(error) = state {
                    XCTAssertNotNil(error)
                    XCTAssertEqual(error, AuthenticationService.AuthenticationError.invalidUserPassword)
                } else {
                    XCTFail("Unexpected authenticationError in state \(state)")
                }
            }

        AuthenticationWorkflow
            .Action
            .tester(
                withState: .authorizingTwoFactor(
                    twoFactorCode: "twoFactorCode",
                    intermediateSession: "intermediateSession"
                )
            )
            .send(
                action: .authenticationError(AuthenticationService.AuthenticationError.invalidTwoFactor)
            )
            .assertState { state in
                if case let .twoFactor(intermediateSession, error) = state {
                    XCTAssertNotNil(intermediateSession)
                    XCTAssertNotNil(error)
                    XCTAssertEqual(error, AuthenticationService.AuthenticationError.invalidTwoFactor)
                } else {
                    XCTFail("Unexpected authenticationError in state \(state)")
                }
            }
    }

    // MARK: Render Tests

    func test_render_initial() {
        let authenticationWorkFlow = AuthenticationWorkflow(authenticationService: AuthenticationService())
        let expectedState = ExpectedState<AuthenticationWorkflow>(state: .emailPassword)

        let expectedWorkflow = ExpectedWorkflow(
            type: LoginWorkflow.self,
            rendering: LoginScreen(
                title: "",
                email: "",
                onEmailChanged: { _ in },
                password: "",
                onPasswordChanged: { _ in },
                onLoginTapped: {}
            ),
            output: nil
        )

        let renderExpectations = RenderExpectations<AuthenticationWorkflow>(
            expectedState: expectedState,
            expectedOutput: nil,
            expectedWorkers: [],
            expectedWorkflows: [expectedWorkflow]
        )

        authenticationWorkFlow
            .renderTester(initialState: .emailPassword)
            .render(
                with: renderExpectations,
                assertions: { screen in
                    XCTAssertNil(screen.alert)
                }
            )
    }

    func test_render_AuthorizingEmailPasswordWorker() {
        let authenticationService = AuthenticationService()
        let authenticationWorkFlow = AuthenticationWorkflow(authenticationService: authenticationService)

        let expectedState = ExpectedState<AuthenticationWorkflow>(
            state: .authorizingEmailPassword(
                email: "reza@example.com",
                password: "password"
            )
        )

        let expectedWorkflow = ExpectedWorkflow(
            type: LoginWorkflow.self,
            rendering: LoginScreen(
                title: "",
                email: "",
                onEmailChanged: { _ in },
                password: "",
                onPasswordChanged: { _ in },
                onLoginTapped: {}
            ),
            output: nil
        )

        let expectedWorker = ExpectedWorker(
            worker: AuthenticationWorkflow.AuthorizingEmailPasswordWorker(
                authenticationService: authenticationService,
                email: "reza@example.com",
                password: "password"
            )
        )

        let renderExpectations = RenderExpectations<AuthenticationWorkflow>(
            expectedState: expectedState,
            expectedOutput: nil,
            expectedWorkers: [expectedWorker],
            expectedWorkflows: [expectedWorkflow]
        )

        authenticationWorkFlow
            .renderTester(
                initialState: .authorizingEmailPassword(
                    email: "reza@example.com",
                    password: "password"
                )
            )
            .render(
                with: renderExpectations,
                assertions: { screen in
                    XCTAssertNil(screen.alert)
                }
            )
    }

    func test_render_authorizingTwoFactorWorker() {
        let authenticationService = AuthenticationService()
        let authenticationWorkFlow = AuthenticationWorkflow(authenticationService: authenticationService)

        let expectedState = ExpectedState<AuthenticationWorkflow>(
            state: .authorizingTwoFactor(
                twoFactorCode: "twoFactorCode",
                intermediateSession: "intermediateSession"
            )
        )

        let expectedWorkflow = ExpectedWorkflow(
            type: LoginWorkflow.self,
            rendering: LoginScreen(
                title: "",
                email: "",
                onEmailChanged: { _ in },
                password: "",
                onPasswordChanged: { _ in },
                onLoginTapped: {}
            ),
            output: nil
        )

        let expectedWorker = ExpectedWorker(
            worker: AuthenticationWorkflow.AuthorizingTwoFactorWorker(
                authenticationService: authenticationService,
                intermediateToken: "intermediateSession",
                twoFactorCode: "twoFactorCode"
            )
        )

        let renderExpectations = RenderExpectations<AuthenticationWorkflow>(
            expectedState: expectedState,
            expectedOutput: nil,
            expectedWorkers: [expectedWorker],
            expectedWorkflows: [expectedWorkflow]
        )

        authenticationWorkFlow
            .renderTester(
                initialState: .authorizingTwoFactor(
                    twoFactorCode: "twoFactorCode",
                    intermediateSession: "intermediateSession"
                )
            )
            .render(
                with: renderExpectations,
                assertions: { screen in
                    XCTAssertNil(screen.alert)
                }
            )
    }

    func test_render_authenticationErrorAlert() {
        let authenticationService = AuthenticationService()
        let authenticationWorkFlow = AuthenticationWorkflow(authenticationService: authenticationService)

        let expectedState = ExpectedState<AuthenticationWorkflow>(
            state: .authenticationErrorAlert(error: AuthenticationService.AuthenticationError.invalidUserPassword)
        )

        let expectedWorkflow = ExpectedWorkflow(
            type: LoginWorkflow.self,
            rendering: LoginScreen(
                title: "",
                email: "",
                onEmailChanged: { _ in },
                password: "",
                onPasswordChanged: { _ in },
                onLoginTapped: {}
            ),
            output: nil
        )

        let renderExpectations = RenderExpectations<AuthenticationWorkflow>(
            expectedState: expectedState,
            expectedOutput: nil,
            expectedWorkers: [],
            expectedWorkflows: [expectedWorkflow]
        )

        authenticationWorkFlow
            .renderTester(
                initialState: .authenticationErrorAlert(error: AuthenticationService.AuthenticationError.invalidUserPassword)
            )
            .render(
                with: renderExpectations,
                assertions: { screen in
                    XCTAssertNotNil(screen.alert)
                }
            )
    }
}
