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


struct BarScreen: Screen {
    let title: String
    let backgroundColor: UIColor
    let viewTapped: () -> Void
}


extension ViewRegistry {
    
    public mutating func registerBarScreen() {
        self.register(screenViewControllerType: BarScreenViewController.self)
    }
    
}


fileprivate final class BarScreenViewController: ScreenViewController<BarScreen> {

    private lazy var titleLabel: UILabel = .init()
    private let titleLabelPadding: CGFloat = 20.0
    private lazy var tapGestureRecognizer: UITapGestureRecognizer = .init()
    
    required init(screen: BarScreen, viewRegistry: ViewRegistry) {
        super.init(screen: screen, viewRegistry: viewRegistry)
        
        update(with: screen)
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()

        tapGestureRecognizer.addTarget(self, action: #selector(viewTapped))
        view.addGestureRecognizer(tapGestureRecognizer)

        titleLabel.font = UIFont.preferredFont(forTextStyle: .body)
        titleLabel.textAlignment = .left
        titleLabel.numberOfLines = 0
        view.addSubview(titleLabel)
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()

        titleLabel.frame = view.bounds.insetBy(dx: titleLabelPadding, dy: titleLabelPadding)

        titleLabel.preferredMaxLayoutWidth = view.bounds.width - (titleLabelPadding * 2)

        calculatePreferredSize()
    }

    private func calculatePreferredSize() {
        let preferredHeight = titleLabel.intrinsicContentSize.height + (titleLabelPadding * 2)

        preferredContentSize = CGSize(width: view.bounds.width, height: preferredHeight)
    }
    
    override func screenDidChange(from previousScreen: BarScreen) {
        update(with: screen)
    }
    
    private func update(with screen: BarScreen) {
        view.backgroundColor = screen.backgroundColor

        titleLabel.text = screen.title

        calculatePreferredSize()
    }

    @objc
    private func viewTapped() {
        screen.viewTapped()
    }
}

