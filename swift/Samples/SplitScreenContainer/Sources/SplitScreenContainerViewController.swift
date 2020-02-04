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

extension ViewRegistry {
    
    public mutating func registerSplitScreenContainer() {
        self.register(screenViewControllerType: SplitScreenContainerViewController.self)
    }
    
}


final class SplitScreenContainerViewController: ScreenViewController<SplitScreenContainerScreen> {

    private var leftContentViewController: ScreenViewController<AnyScreen>? = nil
    private lazy var leftContainerView: UIView = .init()

    private lazy var separatorView: UIView = .init()

    private var rightContentViewController: ScreenViewController<AnyScreen>? = nil
    private lazy var rightContainerView: UIView = .init()

    required init(screen: SplitScreenContainerScreen, viewRegistry: ViewRegistry) {
        super.init(screen: screen, viewRegistry: viewRegistry)

        update(with: screen)
    }

    override func screenDidChange(from previousScreen: SplitScreenContainerScreen) {
        update(with: screen)
    }

    private func update(with screen: SplitScreenContainerScreen) {
        separatorView.backgroundColor = screen.separatorColor

        if let leftContentViewController = leftContentViewController {
            if leftContentViewController.screenType == type(of: screen.leftScreen) {
                leftContentViewController.update(screen: screen.leftScreen)
            } else {
                leftContentViewController.willMove(toParent: nil)
                leftContentViewController.view.removeFromSuperview()
                leftContentViewController.removeFromParent()

                self.leftContentViewController = embed(screen.leftScreen, in: leftContainerView)
            }
        } else {
            self.leftContentViewController = embed(screen.leftScreen, in: leftContainerView)
        }

        if let rightContentViewController = rightContentViewController {
            if rightContentViewController.screenType == type(of: screen.rightScreen) {
                rightContentViewController.update(screen: screen.rightScreen)
            } else {
                rightContentViewController.willMove(toParent: nil)
                rightContentViewController.view.removeFromSuperview()
                rightContentViewController.removeFromParent()

                self.rightContentViewController = embed(screen.rightScreen, in: rightContainerView)
            }
        } else {
            self.rightContentViewController = embed(screen.rightScreen, in: rightContainerView)
        }
    }
    
    private func embed(_ screen: AnyScreen, in containerView: UIView) -> ScreenViewController<AnyScreen> {
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

    override func viewDidLoad() {
        super.viewDidLoad()
        
        leftContainerView.translatesAutoresizingMaskIntoConstraints = false
        separatorView.translatesAutoresizingMaskIntoConstraints = false
        rightContainerView.translatesAutoresizingMaskIntoConstraints = false

        view.addSubview(leftContainerView)
        view.addSubview(separatorView)
        view.addSubview(rightContainerView)

        NSLayoutConstraint.activate([
            leftContainerView.topAnchor.constraint(equalTo: view.topAnchor),
            leftContainerView.leftAnchor.constraint(equalTo: view.leftAnchor),
            leftContainerView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            leftContainerView.rightAnchor.constraint(equalTo: separatorView.leftAnchor),
            leftContainerView.widthAnchor.constraint(equalTo: view.widthAnchor, multiplier: screen.ratio),

            separatorView.topAnchor.constraint(equalTo: view.topAnchor),
            separatorView.widthAnchor.constraint(equalToConstant: 1.0),
            separatorView.bottomAnchor.constraint(equalTo: view.bottomAnchor),

            rightContainerView.topAnchor.constraint(equalTo: view.topAnchor),
            rightContainerView.leftAnchor.constraint(equalTo: separatorView.rightAnchor),
            rightContainerView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            rightContainerView.rightAnchor.constraint(equalTo: view.rightAnchor),
        ])
    }
}
