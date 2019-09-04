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
@testable import Tutorial5
import WorkflowTesting


class TodoListWorkflowTests: XCTestCase {

    func testActions() {
        TodoListWorkflow
            .Action
            .tester(withState: TodoListWorkflow.State())
            .send(action: .onBack) { output in
                // The `.onBack` action should emit an output of `.back`.
                switch output {
                case .back?:
                    break // Expected
                default:
                    XCTFail("Expected an output of `.back`")
                }
            }
            .send(action: .selectTodo(index: 7)) { output in
                // The `.selectTodo` action should emit a `.selectTodo` output.
                switch output {
                case .selectTodo(let index)?:
                    XCTAssertEqual(7, index)
                default:
                    XCTFail("Expected an output of `.selectTodo`")
                }
            }
            .send(action: .new) { output in
                // The`.new` action should emit a `.newTodo` output.
                switch output {
                case .newTodo?:
                break // Expected
                default:
                    XCTFail("Expected an output of `.newTodo`")
                }
            }
    }

}
