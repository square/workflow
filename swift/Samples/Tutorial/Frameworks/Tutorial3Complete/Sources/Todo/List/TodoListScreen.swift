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
import TutorialViews


struct TodoListScreen: Screen {
    // The titles of the todo items
    var todoTitles: [String]

    // Callback when a todo is selected
    var onTodoSelected: (Int) -> Void

    var viewControllerDescription: ViewControllerDescription {
        return TodoListViewController.description(for: self)
    }
}


final class TodoListViewController: ScreenViewController<TodoListScreen> {
    let todoListView: TodoListView

    required init(screen: TodoListScreen) {
        self.todoListView = TodoListView(frame: .zero)
        super.init(screen: screen)
        update(with: screen)
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        view.addSubview(todoListView)
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()

        todoListView.frame = view.bounds.inset(by: view.safeAreaInsets)
    }

    override func screenDidChange(from previousScreen: TodoListScreen) {
        update(with: screen)
    }

    private func update(with screen: TodoListScreen) {
        // Update the todoList on the view with what the screen provided:
        todoListView.todoList = screen.todoTitles
        todoListView.onTodoSelected = screen.onTodoSelected
    }

}
