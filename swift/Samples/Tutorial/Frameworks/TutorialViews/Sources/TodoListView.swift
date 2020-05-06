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
import UIKit

public final class TodoListView: UIView, UITableViewDelegate, UITableViewDataSource {
    public var todoList: [String] {
        didSet {
            tableView.reloadData()
        }
    }

    public var onTodoSelected: (Int) -> Void

    let titleLabel: UILabel
    let tableView: UITableView

    override public init(frame: CGRect) {
        self.todoList = []
        self.onTodoSelected = { _ in }
        self.titleLabel = UILabel(frame: .zero)
        self.tableView = UITableView()

        super.init(frame: frame)

        backgroundColor = .white

        titleLabel.text = "What do you have to do?"
        titleLabel.textColor = .black
        titleLabel.textAlignment = .center

        tableView.delegate = self
        tableView.dataSource = self

        addSubview(titleLabel)
        addSubview(tableView)
    }

    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    // MARK: UIView

    override public func layoutSubviews() {
        super.layoutSubviews()

        titleLabel.frame = CGRect(
            x: bounds.minX,
            y: bounds.minY,
            width: bounds.maxX,
            height: 44.0
        )

        let yOffset = titleLabel.frame.maxY + 8.0

        tableView.frame = CGRect(
            x: bounds.minX,
            y: yOffset,
            width: bounds.maxX,
            height: bounds.maxY - yOffset
        )
    }

    // MARK: UITableViewDelegate, UITableViewDataSource

    public func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return todoList.count
    }

    public func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = UITableViewCell()

        cell.textLabel?.text = todoList[indexPath.row]

        return cell
    }

    public func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        tableView.deselectRow(at: indexPath, animated: true)

        onTodoSelected(indexPath.row)
    }
}
