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

public final class SplitScreenContainerViewController<LeftScreenType: Screen, RightScreenType: Screen>: ScreenViewController<SplitScreenContainerScreen<LeftScreenType, RightScreenType>> {

    private var leftContentViewController: ScreenViewController<LeftScreenType>? = nil
    private lazy var leftContainerView: UIView = .init()
    private var leftContainerViewWidthConstraint: NSLayoutConstraint?

    private lazy var separatorView: UIView = .init()

    private var rightContentViewController: ScreenViewController<RightScreenType>? = nil
    private lazy var rightContainerView: UIView = .init()

    required init(screen: SplitScreenContainerScreen<LeftScreenType, RightScreenType>, viewRegistry: ViewRegistry) {
        super.init(screen: screen, viewRegistry: viewRegistry)
    }

    override public func screenDidChange(from previousScreen: SplitScreenContainerScreen<LeftScreenType, RightScreenType>) {
        update(with: screen)
    }

    private func update(with screen: SplitScreenContainerScreen<LeftScreenType, RightScreenType>) {
        if (separatorView.backgroundColor != screen.separatorColor) {
            separatorView.backgroundColor = screen.separatorColor
        }
        
        leftContentViewController?.update(screen: screen.leftScreen)
        rightContentViewController?.update(screen: screen.rightScreen)
        
        if (leftContainerViewWidthConstraint?.multiplier != screen.ratio) {
            updateWidthConstraints(ratio: screen.ratio, animated: true)
        }
    }

    override public func viewDidLoad() {
        super.viewDidLoad()
        
        leftContainerView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(leftContainerView)
        
        separatorView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(separatorView)
        
        rightContainerView.translatesAutoresizingMaskIntoConstraints = false
        view.addSubview(rightContainerView)

        NSLayoutConstraint.activate([
            leftContainerView.topAnchor.constraint(equalTo: view.topAnchor),
            leftContainerView.leftAnchor.constraint(equalTo: view.leftAnchor),
            leftContainerView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            leftContainerView.rightAnchor.constraint(equalTo: separatorView.leftAnchor),

            separatorView.topAnchor.constraint(equalTo: view.topAnchor),
            separatorView.widthAnchor.constraint(equalToConstant: 1.0),
            separatorView.bottomAnchor.constraint(equalTo: view.bottomAnchor),

            rightContainerView.topAnchor.constraint(equalTo: view.topAnchor),
            rightContainerView.leftAnchor.constraint(equalTo: separatorView.rightAnchor),
            rightContainerView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            rightContainerView.rightAnchor.constraint(equalTo: view.rightAnchor),
        ])
        
        updateWidthConstraints(ratio: screen.ratio, animated: false)
        
        self.leftContentViewController = embed(screen.leftScreen, in: leftContainerView)
        self.rightContentViewController = embed(screen.rightScreen, in: rightContainerView)
        
        update(with: screen)
    }
    
    private func embed<ScreenType: Screen>(_ screen: ScreenType, in containerView: UIView) -> ScreenViewController<ScreenType> {
        let viewController = viewRegistry.provideView(for: screen)
        
        addChild(viewController)
        
        viewController.view.translatesAutoresizingMaskIntoConstraints = false
        containerView.addSubview(viewController.view)
        
        NSLayoutConstraint.activate([
            viewController.view.topAnchor.constraint(equalTo: containerView.topAnchor),
            viewController.view.rightAnchor.constraint(equalTo: containerView.rightAnchor),
            viewController.view.bottomAnchor.constraint(equalTo: containerView.bottomAnchor),
            viewController.view.leftAnchor.constraint(equalTo: containerView.leftAnchor),
            ])
        
        viewController.didMove(toParent: self)
        
        return viewController
    }
    
    private func updateWidthConstraints(ratio: CGFloat, animated: Bool) {
        func updateConstraints() {
            leftContainerViewWidthConstraint?.isActive = false
            
            leftContainerViewWidthConstraint = leftContainerView.widthAnchor.constraint(equalTo: view.widthAnchor, multiplier: ratio)
            leftContainerViewWidthConstraint?.isActive = true
        }
        
        if animated {
            view.layoutIfNeeded()
            UIView.animate(withDuration: 0.2) {
                updateConstraints()
                
                self.view.layoutIfNeeded()
            }
        } else {
            updateConstraints()
        }
    }
}
