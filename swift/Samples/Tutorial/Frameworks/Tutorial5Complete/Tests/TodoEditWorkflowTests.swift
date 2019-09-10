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


class TodoEditWorkflowTests: XCTestCase {

    func testAction() {
        TodoEditWorkflow
            .Action
            // Start with a todo of "Title" "Note"
            .tester(
                withState: TodoEditWorkflow.State(
                    todo: TodoModel(title: "Title", note: "Note")))
            .assertState { state in
                XCTAssertEqual("Title", state.todo.title)
                XCTAssertEqual("Note", state.todo.note)
            }
            // Update the title to "Updated Title"
            .send(action: .titleChanged("Updated Title")) { output in
                XCTAssertNil(output)
            }
            // Validate that only the title changed.
            .assertState { state in
                XCTAssertEqual("Updated Title", state.todo.title)
                XCTAssertEqual("Note", state.todo.note)
            }
            // Update the note.
            .send(action: .noteChanged("Updated Note")) { output in
                XCTAssertNil(output)
            }
            // Validate that the note updated.
            .assertState { state in
                XCTAssertEqual("Updated Title", state.todo.title)
                XCTAssertEqual("Updated Note", state.todo.note)
            }
            // Send a `.discardChanges` action, which will emit a `.discard` output.
            .send(action: .discardChanges) { output in
                switch output {
                case .discard?:
                    break // Expected
                default:
                    XCTFail("Expected an output of `.discard`")
                }
            }
            // Send a `.saveChanges` action, which will emit a `.save` output with the updated todo model.
            .send(action: .saveChanges) { output in
                switch output {
                case .save(let todo)?:
                    XCTAssertEqual("Updated Title", todo.title)
                    XCTAssertEqual("Updated Note", todo.note)
                default:
                    XCTFail("Expected an output of `.save`")
                }
            }
    }

    func testChangedPropertyUpdatesLocalState() {
        let initialWorkflow = TodoEditWorkflow(initialTodo: TodoModel(title: "Title", note: "Note"))
        var state = initialWorkflow.makeInitialState()
        // The initial state is a copy of the provided todo:
        XCTAssertEqual("Title", state.todo.title)
        XCTAssertEqual("Note", state.todo.note)

        // Mutate the internal state, simulating the change from actions:
        state.todo.title = "Updated Title"

        // Update the workflow properties with the same value. The state should not be updated:
        initialWorkflow.workflowDidChange(from: initialWorkflow, state: &state)
        XCTAssertEqual("Updated Title", state.todo.title)
        XCTAssertEqual("Note", state.todo.note)

        // The parent provided different properties. The internal state should be updated with the newly provided properties.
        let updatedWorkflow = TodoEditWorkflow(initialTodo: TodoModel(title: "New Title", note: "New Note"))
        updatedWorkflow.workflowDidChange(from: initialWorkflow, state: &state)

        XCTAssertEqual("New Title", state.todo.title)
        XCTAssertEqual("New Note", state.todo.note)
    }

}
