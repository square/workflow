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


struct BaseScreen: Screen {
    let title: String
    let backgroundColor: UIColor
    let viewTapped: () -> Void
}


extension ViewRegistry {
    
    public mutating func registerBaseScreen() {
        self.register(screenViewControllerType: BaseScreenViewController.self)
    }
    
}


fileprivate final class BaseScreenViewController: ScreenViewController<BaseScreen> {
    
    private lazy var titleLabel: UILabel = .init()
    private lazy var tapGestureRecognizer: UITapGestureRecognizer = .init()
    
    required init(screen: BaseScreen, viewRegistry: ViewRegistry) {
        super.init(screen: screen, viewRegistry: viewRegistry)
        
        update(with: screen)
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        tapGestureRecognizer.addTarget(self, action: #selector(viewTapped))
        view.addGestureRecognizer(tapGestureRecognizer)
        
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        titleLabel.textAlignment = .center
        view.addSubview(titleLabel)
        
        NSLayoutConstraint.activate([
            titleLabel.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            titleLabel.centerYAnchor.constraint(equalTo: view.centerYAnchor),
        ])
    }
    
    override func screenDidChange(from previousScreen: BaseScreen) {
        update(with: screen)
    }
    
    private func update(with screen: BaseScreen) {
        view.backgroundColor = screen.backgroundColor
        titleLabel.text = screen.title
    }
    
    @objc
    private func viewTapped() {
        screen.viewTapped()
    }
}

