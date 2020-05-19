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

import BackStackContainer
import ReactiveSwift
import Workflow
import WorkflowUI

// MARK: Input and Output

struct TodoEditWorkflow: Workflow {
    // The "Todo" passed from our parent.
    var initialTodo: TodoModel

    enum Output {
        case discard
        case save(TodoModel)
    }
}

// MARK: State and Initialization

extension TodoEditWorkflow {
    struct State {
        // The workflow's copy of the Todo item. Changes are local to this workflow.
        var todo: TodoModel
    }

    func makeInitialState() -> TodoEditWorkflow.State {
        return State(todo: initialTodo)
    }

    func workflowDidChange(from previousWorkflow: TodoEditWorkflow, state: inout State) {
        // The `Todo` from our parent changed. Update our internal copy so we are starting from the same item.
        // The "correct" behavior depends on the business logic - would we only want to update if the
        // users hasn't changed the todo from the initial one? Or is it ok to delete whatever edits
        // were in progress if the state from the parent changes?
        if previousWorkflow.initialTodo != initialTodo {
            state.todo = initialTodo
        }
    }
}

// MARK: Actions

extension TodoEditWorkflow {
    enum Action: WorkflowAction {
        typealias WorkflowType = TodoEditWorkflow

        case titleChanged(String)
        case noteChanged(String)
        case discardChanges
        case saveChanges

        func apply(toState state: inout TodoEditWorkflow.State) -> TodoEditWorkflow.Output? {
            switch self {
            case let .titleChanged(title):
                state.todo.title = title

            case let .noteChanged(note):
                state.todo.note = note

            case .discardChanges:
                // Return the .discard output when the discard action is received.
                return .discard

            case .saveChanges:
                // Return the .save output with the current todo state when the save action is received.
                return .save(state.todo)
            }

            return nil
        }
    }
}

// MARK: Workers

extension TodoEditWorkflow {
    struct TodoEditWorker: Worker {
        enum Output {}

        func run() -> SignalProducer<Output, Never> {
            fatalError()
        }

        func isEquivalent(to otherWorker: TodoEditWorker) -> Bool {
            return true
        }
    }
}

// MARK: Rendering

extension TodoEditWorkflow {
    typealias Rendering = BackStackScreen<AnyScreen>.Item

    func render(state: TodoEditWorkflow.State, context: RenderContext<TodoEditWorkflow>) -> Rendering {
        // The sink is used to send actions back to this workflow.
        let sink = context.makeSink(of: Action.self)

        let todoEditScreen = TodoEditScreen(
            title: state.todo.title,
            note: state.todo.note,
            onTitleChanged: { title in
                sink.send(.titleChanged(title))
            },
            onNoteChanged: { note in
                sink.send(.noteChanged(note))
            }
        )

        let backStackItem = BackStackScreen.Item(
            key: "edit",
            screen: todoEditScreen.asAnyScreen(),
            barContent: .init(
                title: "Edit",
                leftItem: .button(.back(handler: {
                    sink.send(.discardChanges)
                })),
                rightItem: .button(.init(
                    content: .text("Save"),
                    handler: {
                        sink.send(.saveChanges)
                    }
                ))
            )
        )
        return backStackItem
    }
}
