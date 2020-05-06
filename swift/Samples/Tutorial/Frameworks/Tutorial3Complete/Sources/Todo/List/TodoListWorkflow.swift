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
import ReactiveSwift
import Workflow
import WorkflowUI

// MARK: Input and Output

struct TodoListWorkflow: Workflow {
    // The name is an input.
    var name: String

    enum Output {
        case back
    }
}

// MARK: State and Initialization

extension TodoListWorkflow {
    struct State {
        var todos: [TodoModel]
        var step: Step
        enum Step {
            // Showing the list of todo items.
            case list
            // Editing a single item. The state holds the index so it can be updated when a save action is received.
            case edit(index: Int)
        }
    }

    func makeInitialState() -> TodoListWorkflow.State {
        return State(
            todos: [
                TodoModel(
                    title: "Take the cat for a walk",
                    note: "Cats really need their outside sunshine time. Don't forget to walk Charlie. Hamilton is less excited about the prospect."
                ),
            ],
            step: .list
        )
    }

    func workflowDidChange(from previousWorkflow: TodoListWorkflow, state: inout State) {}
}

// MARK: Actions

extension TodoListWorkflow {
    enum Action: WorkflowAction {
        typealias WorkflowType = TodoListWorkflow

        case onBack
        case selectTodo(index: Int)
        case discardChanges
        case saveChanges(todo: TodoModel, index: Int)

        func apply(toState state: inout TodoListWorkflow.State) -> TodoListWorkflow.Output? {
            switch self {
            case .onBack:
                // When a `.onBack` action is received, emit a `.back` output
                return .back

            case let .selectTodo(index: index):
                // When a todo item is selected, edit it.
                state.step = .edit(index: index)
                return nil

            case .discardChanges:
                // When a discard action is received, return to the list.
                state.step = .list
                return nil

            case let .saveChanges(todo: todo, index: index):
                // When changes are saved, update the state of that `todo` item and return to the list.
                state.todos[index] = todo

                state.step = .list
                return nil
            }
        }
    }
}

// MARK: Workers

extension TodoListWorkflow {
    struct TodoListWorker: Worker {
        enum Output {}

        func run() -> SignalProducer<Output, Never> {
            fatalError()
        }

        func isEquivalent(to otherWorker: TodoListWorker) -> Bool {
            return true
        }
    }
}

// MARK: Rendering

extension TodoListWorkflow {
    typealias Rendering = [BackStackScreen.Item]

    func render(state: TodoListWorkflow.State, context: RenderContext<TodoListWorkflow>) -> Rendering {
        // Define a sink to be able to send actions.
        let sink = context.makeSink(of: Action.self)

        let titles = state.todos.map { (todoModel) -> String in
            todoModel.title
        }
        let todoListScreen = TodoListScreen(
            todoTitles: titles,
            onTodoSelected: { index in
                // Send the `selectTodo` action when a todo is selected in the UI.
                sink.send(.selectTodo(index: index))
            }
        )

        let todoListItem = BackStackScreen.Item(
            key: "list",
            screen: todoListScreen,
            barContent: BackStackScreen.BarContent(
                title: "Welcome \(name)",
                leftItem: .button(.back(handler: {
                    // When the left button is tapped, send the .onBack action.
                    sink.send(.onBack)
                })),
                rightItem: .none
            )
        )

        switch state.step {
        case .list:
            // On the "list" step, return just the list screen.
            return [todoListItem]

        case let .edit(index: index):
            // On the "edit" step, return both the list and edit screens.
            let todoEditItem = TodoEditWorkflow(
                initialTodo: state.todos[index])
                .mapOutput { output -> Action in
                    switch output {
                    case .discard:
                        // Send the discardChanges action when the discard output is received.
                        return .discardChanges

                    case let .save(todo):
                        // Send the saveChanges action when the save output is received.
                        return .saveChanges(todo: todo, index: index)
                    }
                }
                .rendered(with: context)

            return [todoListItem, todoEditItem]
        }
    }
}
