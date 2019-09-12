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
import BackStackContainer
import ReactiveSwift
import Result


// MARK: Input and Output

struct TodoWorkflow: Workflow {
    var name: String
    // Have the `IssueService` be provided as a dependency to the `TodoWorkflow`:
    var issueService: IssueService

    enum Output {
        case back
    }
}


// MARK: State and Initialization

extension TodoWorkflow {

    struct State: Equatable {
        var todos: [TodoModel]
        var step: Step
        enum Step: Equatable {
            // Show a loading screen while fetching the initial set of TODO items
            case loading
            // Showing the list of todo items.
            case list
            // Editing a single item. The state holds the index so it can be updated when a save action is received.
            case edit(index: Int)
        }
    }

    func makeInitialState() -> TodoWorkflow.State {
        return State(
            todos: [],
            // Start from the `.loading` step, which will show our loading screen and fetch the initial list.
            step: .loading)
    }

    func workflowDidChange(from previousWorkflow: TodoWorkflow, state: inout State) {

    }
}


// MARK: Actions

extension TodoWorkflow {

    enum LoadAction: WorkflowAction {
        typealias WorkflowType = TodoWorkflow

        case loaded(todos: [TodoModel])
        case loadingFailed(Error)

        func apply(toState state: inout State) -> Output? {
            switch self {

            case .loaded(todos: let todos):
                // Populate the `todos` from a successful load.
                state.todos = todos
                state.step = .list
                return nil

            case .loadingFailed:
                // For now, just go back if we fail to load the issues.
                // We could also consider showing a default TODO if this fails, or an error message, etc.
                return .back
            }
        }
    }

    enum ListAction: WorkflowAction {

        typealias WorkflowType = TodoWorkflow

        case back
        case editTodo(index: Int)
        case newTodo

        func apply(toState state: inout TodoWorkflow.State) -> TodoWorkflow.Output? {

            switch self {
            case .back:
                return .back

            case .editTodo(index: let index):
                state.step = .edit(index: index)

            case .newTodo:
                // Append a new todo model to the end of the list.
                state.todos.append(TodoModel(
                    title: "New Todo",
                    note: ""))
            }

            return nil
        }
    }

    enum EditAction: WorkflowAction {

        typealias WorkflowType = TodoWorkflow

        case discardChanges
        case saveChanges(index: Int, todo: TodoModel)

        func apply(toState state: inout TodoWorkflow.State) -> TodoWorkflow.Output? {
            guard case .edit = state.step else {
                fatalError("Received edit action when state was not `.edit`.")
            }

            switch self {

            case .discardChanges:
                state.step = .list

            case .saveChanges(index: let index, todo: let updatedTodo):
                state.todos[index] = updatedTodo

            }
            // Return to the list view for either a discard or save action.
            state.step = .list

            return nil
        }
    }
}


// MARK: Workers

extension TodoWorkflow {

    struct TodoWorker: Worker {

        enum Output {

        }

        func run() -> SignalProducer<Output, NoError> {
            fatalError()
        }

        func isEquivalent(to otherWorker: TodoWorker) -> Bool {
            return true
        }

    }

}

// MARK: Rendering

extension TodoWorkflow {

    typealias Rendering = [BackStackScreen.Item]

    func render(state: TodoWorkflow.State, context: RenderContext<TodoWorkflow>) -> Rendering {

        let todoListItem = TodoListWorkflow(
            name: name,
            todos: state.todos)
            .mapOutput({ output -> ListAction in
                switch output {

                case .back:
                    return .back

                case .selectTodo(index: let index):
                    return .editTodo(index: index)

                case .newTodo:
                    return .newTodo
                }
            })
            .rendered(with: context)

        switch state.step {
        // Add a case for the loading step:
        case .loading:
            let loadingScreen = LoadingWorkflow(issueService: issueService)
                .mapOutput({ output -> LoadAction in
                    // Map the output of the LoadingWorkflow to our LoadAction.
                    switch output {
                    case .loadCompleted(let todos):
                        return .loaded(todos: todos)
                    case .loadFailed(let error):
                        return .loadingFailed(error)
                    }
                })
                .rendered(with: context)
            return [
                BackStackScreen.Item(
                    screen: loadingScreen,
                    barVisibility: .hidden)
            ]

        case .list:
            // Return only the list item.
            return [todoListItem]

        case .edit(index: let index):

            let todoEditItem = TodoEditWorkflow(
                initialTodo: state.todos[index])
                .mapOutput({ output -> EditAction in
                    switch output {
                    case .discard:
                        return .discardChanges

                    case .save(let updatedTodo):
                        return .saveChanges(index: index, todo: updatedTodo)
                    }
                })
                .rendered(with: context)

            // Return both the list item and edit.
            return [todoListItem, todoEditItem]
        }

    }
}
