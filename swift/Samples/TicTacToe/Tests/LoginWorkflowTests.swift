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

class LoginWorkflowTests: XCTestCase {
    // MARK: Action Tests

    func test_action_emailUpdate() {
        LoginWorkflow
            .Action
            .tester(
                withState: LoginWorkflow.State(
                    email: "reza@example.com",
                    password: "password"
                )
            )
            .send(
                action: .emailUpdated("square@example.com"),
                outputAssertions: { output in
                    XCTAssertNil(output)
                }
            )
            .assertState { state in
                XCTAssertEqual(state.email, "square@example.com")
                XCTAssertEqual(state.password, "password")
            }
    }

    func test_action_passwordUpdate() {
        LoginWorkflow
            .Action
            .tester(
                withState: LoginWorkflow.State(
                    email: "reza@example.com",
                    password: "password"
                )
            )
            .send(
                action: .passwordUpdated("drowssap"),
                outputAssertions: { output in
                    XCTAssertNil(output)
                }
            )
            .assertState { state in
                XCTAssertEqual(state.email, "reza@example.com")
                XCTAssertEqual(state.password, "drowssap")
            }
    }

    func test_action_login() {
        LoginWorkflow
            .Action
            .tester(
                withState: LoginWorkflow.State(
                    email: "reza@example.com",
                    password: "password"
                )
            )
            .send(
                action: .login,
                outputAssertions: { output in
                    XCTAssertNotNil(output)
                    switch output! {
                    case let .login(email, password):
                        XCTAssertEqual(email, "reza@example.com")
                        XCTAssertEqual(password, "password")
                    }
                }
            )
    }

    // MARK: Render Tests

    func test_render_initial() {
        let loginWorkflow = LoginWorkflow()
        loginWorkflow
            .renderTester(initialState: LoginWorkflow.State(email: "reza@example.com", password: "password"))
            .render(assertions: { screen in
                XCTAssertEqual(screen.title, "Welcome! Please log in to play TicTacToe!")
                XCTAssertEqual(screen.email, "reza@example.com")
                XCTAssertEqual(screen.password, "password")
            }
            )
    }
}
