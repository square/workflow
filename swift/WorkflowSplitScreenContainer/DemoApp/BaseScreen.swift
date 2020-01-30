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
    var title: String
    var backgroundColor: UIColor
}


extension ViewRegistry {
    
    public mutating func registerBaseScreen() {
        self.register(screenViewControllerType: BaseScreenViewController.self)
    }
    
}


fileprivate final class BaseScreenViewController: ScreenViewController<BaseScreen> {
    
    private let titleLabel: UILabel
    
    required init(screen: BaseScreen, viewRegistry: ViewRegistry) {
        titleLabel = UILabel(frame: .zero)
        super.init(screen: screen, viewRegistry: viewRegistry)
        
        update(with: screen)
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        view.addSubview(titleLabel)
    }
    
    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        
        let height: CGFloat = 44.0
        
        var (top, bottom) = view.bounds.divided(atDistance: view.bounds.height / 2, from: CGRectEdge.minYEdge)
        
        top.size.height -= (height / 2.0)
        bottom.origin.y += (height)
        bottom.size.height -= (height / 2.0)
        
        titleLabel.frame = top
    }
    
    override func screenDidChange(from previousScreen: BaseScreen) {
        update(with: screen)
    }
    
    private func update(with screen: BaseScreen) {
        view.backgroundColor = screen.backgroundColor
        titleLabel.text = screen.title
    }
}

