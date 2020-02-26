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

#if canImport(UIKit)

import UIKit


public final class DescribedViewController: UIViewController {

    var currentViewController: UIViewController

    public init(description: ViewControllerDescription) {
        currentViewController = description.buildViewController()
        super.init(nibName: nil, bundle: nil)
    }

    public convenience init<S: Screen>(screen: S) {
        self.init(description: screen.viewControllerDescription)
    }

    @available(*, unavailable)
    required init?(coder: NSCoder) {
        fatalError("init(coder:) is unavailable")
    }

    public func update(description: ViewControllerDescription) {
        if description.canUpdate(viewController: currentViewController) {
            description.update(viewController: currentViewController)
        } else {
            if isViewLoaded {
                currentViewController.willMove(toParent: nil)
                currentViewController.view.removeFromSuperview()
                currentViewController.removeFromParent()
            }
            currentViewController = description.buildViewController()
            if isViewLoaded {
                addChild(currentViewController)
                view.addSubview(currentViewController.view)
                currentViewController.view.frame = view.bounds
                currentViewController.didMove(toParent: self)
            }
        }
    }

    public func update<S: Screen>(screen: S) {
        update(description: screen.viewControllerDescription)
    }

    public override func viewDidLoad() {
        super.viewDidLoad()

        addChild(currentViewController)
        view.addSubview(currentViewController.view)
        currentViewController.didMove(toParent: self)
    }

    public override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        currentViewController.view.frame = view.bounds
    }

    public override var childForStatusBarStyle: UIViewController? {
        return currentViewController
    }

    public override var childForStatusBarHidden: UIViewController? {
        return currentViewController
    }

    public override var childForHomeIndicatorAutoHidden: UIViewController? {
        return currentViewController
    }

    public override var childForScreenEdgesDeferringSystemGestures: UIViewController? {
        return currentViewController
    }

    public override var supportedInterfaceOrientations: UIInterfaceOrientationMask {
        return currentViewController.supportedInterfaceOrientations
    }

}

#endif
