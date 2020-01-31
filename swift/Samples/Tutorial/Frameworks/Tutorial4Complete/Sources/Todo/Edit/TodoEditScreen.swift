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


struct TodoEditScreen: Screen {
    // The title of this todo item.
    var title: String
    // The contents, or "note" of the todo.
    var note: String

    // Callback for when the title or note changes
    var onTitleChanged: (String) -> Void
    var onNoteChanged: (String) -> Void

    var viewControllerDescription: ViewControllerDescription {
        return TodoEditViewController.description(for: self)
    }
}


final class TodoEditViewController: ScreenViewController<TodoEditScreen> {
    // The `todoEditView` has all the logic for displaying the todo and editing.
    let todoEditView: TodoEditView

    required init(screen: TodoEditScreen) {
        self.todoEditView = TodoEditView(frame: .zero)

        super.init(screen: screen)
        update(with: screen)
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        view.addSubview(todoEditView)
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()

        todoEditView.frame = view.bounds.inset(by: view.safeAreaInsets)
    }

    override func screenDidChange(from previousScreen: TodoEditScreen) {
        update(with: screen)
    }

    private func update(with screen: TodoEditScreen) {
        // Update the view with the data from the screen.
        todoEditView.title = screen.title
        todoEditView.note = screen.note
        todoEditView.onTitleChanged = screen.onTitleChanged
        todoEditView.onNoteChanged = screen.onNoteChanged
    }

}
