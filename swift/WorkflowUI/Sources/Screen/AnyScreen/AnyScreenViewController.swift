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

#if os(iOS)

import UIKit


internal final class AnyScreenViewController: ScreenViewController<AnyScreen> {

    typealias WrappedViewController = UIViewController & UntypedScreenViewController

    private (set) internal var currentViewController: WrappedViewController

    required init(screen: AnyScreen, viewRegistry: ViewRegistry) {
        currentViewController = screen.makeViewController(from: viewRegistry)
        super.init(screen: screen, viewRegistry: viewRegistry)

        addChild(currentViewController)
        currentViewController.didMove(toParent: self)
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        view.addSubview(currentViewController.view)
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        currentViewController.view.frame = view.bounds
    }

    override func screenDidChange(from previousScreen: AnyScreen) {
        super.screenDidChange(from: previousScreen)

        if type(of: screen.wrappedScreen) == currentViewController.screenType {
            currentViewController.update(untypedScreen: screen.wrappedScreen)
        } else {
            currentViewController.willMove(toParent: nil)
            if isViewLoaded {
                currentViewController.view.removeFromSuperview()
            }
            currentViewController.removeFromParent()

            currentViewController = screen.makeViewController(from: viewRegistry)
            addChild(currentViewController)
            if isViewLoaded {
                view.addSubview(currentViewController.view)
            }
            currentViewController.didMove(toParent: self)
        }
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
