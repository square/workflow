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


struct CrossFadeScreen: Screen {
    var baseScreen: AnyScreen
    var key: AnyHashable

    init<ScreenType: Screen, Key: Hashable>(base screen: ScreenType, key: Key?) {
        self.baseScreen = AnyScreen(screen)
        if let key = key {
            self.key = AnyHashable(key)
        } else {
            self.key = AnyHashable(ObjectIdentifier(ScreenType.self))
        }
    }

    init<ScreenType: Screen>(base screen: ScreenType) {
        let key = Optional<AnyHashable>.none
        self.init(base: screen, key: key)
    }

    fileprivate func isEquivalent(to otherScreen: CrossFadeScreen) -> Bool {
        return self.key == otherScreen.key
    }

    var viewControllerDescription: ViewControllerDescription {
        return CrossFadeContainerViewController.description(for: self)
    }
}


fileprivate final class CrossFadeContainerViewController: ScreenViewController<CrossFadeScreen> {
    var childViewController: DescribedViewController

    required init(screen: CrossFadeScreen) {
        childViewController = DescribedViewController(screen: screen.baseScreen)
        super.init(screen: screen)
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        addChild(childViewController)
        view.addSubview(childViewController.view)
        childViewController.didMove(toParent: self)
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()

        childViewController.view.frame = view.bounds
    }

    override func screenDidChange(from previousScreen: CrossFadeScreen) {
        if screen.isEquivalent(to: previousScreen) {
            childViewController.update(screen: screen.baseScreen)
        } else {
            // The new screen is different than the previous. Animate the transition.
            let oldChild = childViewController
            childViewController = DescribedViewController(screen: screen.baseScreen)
            addChild(childViewController)
            view.addSubview(childViewController.view)
            UIView.transition(
                from: oldChild.view,
                to: childViewController.view,
                duration: 0.72,
                options: .transitionCrossDissolve,
                completion: { [childViewController] completed in
                    childViewController.didMove(toParent: self)

                    oldChild.willMove(toParent: nil)
                    oldChild.view.removeFromSuperview()
                    oldChild.removeFromParent()
                })
        }
    }

}
