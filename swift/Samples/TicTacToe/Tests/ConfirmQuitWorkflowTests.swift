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

class ConfirmQuitWorkflowTests: XCTestCase {
    // MARK: Action Tests

    func test_action_cancel() {
        ConfirmQuitWorkflow
            .Action
            .tester(withState: ConfirmQuitWorkflow.State(step: .confirmOnce))
            .send(
                action: .cancel,
                outputAssertions: { output in
                    XCTAssertEqual(output, ConfirmQuitWorkflow.Output.cancel)
                }
            )
    }

    func test_action_quit() {
        ConfirmQuitWorkflow
            .Action
            .tester(withState: ConfirmQuitWorkflow.State(step: .confirmOnce))
            .send(
                action: .quit,
                outputAssertions: { output in
                    XCTAssertEqual(output, ConfirmQuitWorkflow.Output.quit)
                }
            )
    }

    func test_action_confirm() {
        ConfirmQuitWorkflow
            .Action
            .tester(withState: ConfirmQuitWorkflow.State(step: .confirmOnce))
            .send(action: .confirm)
            .assertState { state in
                XCTAssertEqual(state.step, .confirmTwice)
            }
    }

    // MARK: Render Tests

    func test_render_confirmOnce() {
        let confirmQuitWorkflow = ConfirmQuitWorkflow()
        confirmQuitWorkflow
            .renderTester(initialState: ConfirmQuitWorkflow.State(step: .confirmOnce))
            .render(
                assertions: { screen in
                    XCTAssertNotNil(screen)
                    XCTAssertNotNil(screen.0)
                    XCTAssertNil(screen.1)
                    XCTAssertEqual(screen.0.question, "Are you sure you want to quit?")
                }
            )
    }

    func test_render_confirmTwice() {
        let confirmQuitWorkflow = ConfirmQuitWorkflow()
        confirmQuitWorkflow
            .renderTester(initialState: ConfirmQuitWorkflow.State(step: .confirmTwice))
            .render(
                assertions: { screen in
                    XCTAssertNotNil(screen)
                    XCTAssertNotNil(screen.0)
                    XCTAssertNotNil(screen.1)
                    XCTAssertEqual(screen.1!.title, "Confirm Again")
                    XCTAssertEqual(screen.1!.message, "Do you really want to quit?")
                    XCTAssertEqual(screen.0.question, "Are you sure you want to quit?")
                    XCTAssertEqual(screen.1!.actions[0].title, "Not really")
                    XCTAssertEqual(screen.1!.actions[1].title, "Yes, please!")
                }
            )
    }
}
