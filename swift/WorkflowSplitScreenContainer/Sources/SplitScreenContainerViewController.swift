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

    var leftContentViewController: ScreenViewController<AnyScreen>? = nil
    var leftContainerView: UIView

    var separatorView: UIView

    var rightContentViewController: ScreenViewController<AnyScreen>? = nil
    var rightContainerView: UIView

    required init(screen: SplitScreenContainerScreen, viewRegistry: ViewRegistry) {
        self.leftContainerView = UIView()
        self.leftContainerView.translatesAutoresizingMaskIntoConstraints = false

        self.separatorView = UIView()
        self.separatorView.translatesAutoresizingMaskIntoConstraints = false
        self.separatorView.backgroundColor = .black

        self.rightContainerView = UIView()
        self.rightContainerView.translatesAutoresizingMaskIntoConstraints = false

        super.init(screen: screen, viewRegistry: viewRegistry)

        update(with: screen)
    }

    override func screenDidChange(from previousScreen: SplitScreenContainerScreen) {
        update(with: screen)
    }

    private func update(with screen: SplitScreenContainerScreen) {

        func embed(_ screen: AnyScreen, in containerView: UIView) -> ScreenViewController<AnyScreen> {
            let viewController = viewRegistry.provideView(for: screen)
            addChild(viewController)
            containerView.addSubview(viewController.view)
            viewController.didMove(toParent: self)

            return viewController
        }

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

    override func viewDidLoad() {
        super.viewDidLoad()

        view.addSubview(leftContainerView)
        view.addSubview(separatorView)
        view.addSubview(rightContainerView)

        if let leftContentViewController = leftContentViewController {
            leftContainerView.addSubview(leftContentViewController.view)
        }

        if let rightContentViewController = rightContentViewController {
            rightContainerView.addSubview(rightContentViewController.view)
        }

        NSLayoutConstraint.activate([
            leftContainerView.topAnchor.constraint(equalTo: view.topAnchor),
            leftContainerView.leftAnchor.constraint(equalTo: view.leftAnchor),
            leftContainerView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            leftContainerView.rightAnchor.constraint(equalTo: separatorView.leftAnchor),
            leftContainerView.widthAnchor.constraint(equalTo: view.widthAnchor, multiplier: screen.ratio.value),

            separatorView.topAnchor.constraint(equalTo: view.topAnchor),
            separatorView.widthAnchor.constraint(equalToConstant: 1.0),
            separatorView.bottomAnchor.constraint(equalTo: view.bottomAnchor),

            rightContainerView.topAnchor.constraint(equalTo: view.topAnchor),
            rightContainerView.leftAnchor.constraint(equalTo: separatorView.rightAnchor),
            rightContainerView.bottomAnchor.constraint(equalTo: view.bottomAnchor),
            rightContainerView.rightAnchor.constraint(equalTo: view.rightAnchor),
        ])
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()

        leftContentViewController?.view.frame = leftContainerView.bounds
        rightContentViewController?.view.frame = rightContainerView.bounds
    }
}
