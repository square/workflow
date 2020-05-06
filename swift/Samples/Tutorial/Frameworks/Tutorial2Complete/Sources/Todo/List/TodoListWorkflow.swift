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

import ReactiveSwift
import Workflow
import WorkflowUI

// MARK: Input and Output

struct TodoListWorkflow: Workflow {
    typealias Output = Never
}

// MARK: State and Initialization

extension TodoListWorkflow {
    struct State {
        var todos: [TodoModel]
    }

    func makeInitialState() -> TodoListWorkflow.State {
        return State(todos: [TodoModel(title: "Take the cat for a walk", note: "Cats really need their outside sunshine time. Don't forget to walk Charlie. Hamilton is less excited about the prospect.")])
    }

    func workflowDidChange(from previousWorkflow: TodoListWorkflow, state: inout State) {}
}

// MARK: Actions

extension TodoListWorkflow {
    enum Action: WorkflowAction {
        typealias WorkflowType = TodoListWorkflow

        func apply(toState state: inout TodoListWorkflow.State) -> TodoListWorkflow.Output? {
            switch self {
                // Update state and produce an optional output based on which action was received.
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
    typealias Rendering = TodoListScreen

    func render(state: TodoListWorkflow.State, context: RenderContext<TodoListWorkflow>) -> Rendering {
        let titles = state.todos.map { (todoModel) -> String in
            todoModel.title
        }
        return TodoListScreen(
            todoTitles: titles,
            onTodoSelected: { _ in }
        )
    }
}
