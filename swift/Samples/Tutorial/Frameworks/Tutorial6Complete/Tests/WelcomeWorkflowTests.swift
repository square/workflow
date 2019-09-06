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
import XCTest
@testable import Tutorial6
import WorkflowTesting


class WelcomeWorkflowTests: XCTestCase {

    func testNameUpdates() {
        WelcomeWorkflow.Action
            .tester(withState: WelcomeWorkflow.State(name: ""))
            .assertState { state in
                // The initial state provided was an empty name.
                XCTAssertEqual("", state.name)
            }
            .send(action: .nameChanged(name: "myName")) { output in
                // No output is expected when the name changes.
                XCTAssertNil(output)
            }
            .assertState { state in
                // The `name` has been updated from the action.
                XCTAssertEqual("myName", state.name)
            }
    }

    func testLogin() {
        WelcomeWorkflow.Action
            .tester(withState: WelcomeWorkflow.State(name: ""))
            .send(action: .didLogin) { output in
                // Since the name is empty, `.didLogin` will not emit an output.
                XCTAssertNil(output)
            }
            .assertState { state in
                // The name is empty, as was specified in the initial state.
                XCTAssertEqual("", state.name)
            }
            .send(action: .nameChanged(name: "MyName")) { output in
                // Update the name.
                XCTAssertNil(output)
            }
            .assertState { state in
                // Validate the name was updated.
                XCTAssertEqual("MyName", state.name)
            }
            .send(action: .didLogin) { output in
                // Now a `.didLogin` output should be emitted when the `.didLogin` action was received.
                switch output {
                case .didLogin(let name)?:
                    XCTAssertEqual("MyName", name)
                case nil:
                    XCTFail("Did not receive an output for .didLogin")
                }
        }
    }

    func testRendering() {
        WelcomeWorkflow()
            // Use the initial state provided by the welcome workflow
            .renderTester()
        .render(assertions: { screen in
            XCTAssertEqual("", screen.name)
            // Simulate tapping the login button. No output will be emitted, as the name is empty:
            screen.onLoginTapped()
        })
        // Next, simulate the name updating, expecting the state to be changed to reflect the updated name:
        .render(
            expectedState: ExpectedState(state: WelcomeWorkflow.State(name: "myName")),
            assertions: { screen in
                screen.onNameChanged("myName")
            })
        // Finally, validate that `.didLogin` is sent when login is tapped with a non-empty name:
        .render(
            expectedOutput: ExpectedOutput(output: .didLogin(name: "myName")),
            assertions: { screen in
                screen.onLoginTapped()
            })
    }
}
