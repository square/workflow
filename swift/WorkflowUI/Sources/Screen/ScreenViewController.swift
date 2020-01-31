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

#if canImport(UIKit)

import UIKit


/// Generic base class that can be subclassed in order to to define a UI implementation that is powered by the
/// given screen type.
///
/// Using this base class, a screen can be implemented as:
/// ```
/// struct MyScreen: Screen {
///     var viewControllerDescription: ViewControllerDescription {
///         return MyScreenViewController.description(for: self)
///     }
/// }
///
/// private class MyScreenViewController: ScreenViewController<MyScreen> {
///     override func screenDidChange(from previousScreen: MyScreen) {
///         // … update views as necessary
///     }
/// }
/// ```
open class ScreenViewController<ScreenType: Screen>: UIViewController {

    private(set) public final var screen: ScreenType

    public final var screenType: Screen.Type {
        return ScreenType.self
    }

    private(set) public final var hints: ContainerHints

    public required init(screen: ScreenType, hints: ContainerHints) {
        self.screen = screen
        self.hints = hints
        super.init(nibName: nil, bundle: nil)
    }

    @available(*, unavailable)
    required public init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    public final func update(screen: ScreenType, hints: ContainerHints) {
        let previousScreen = self.screen
        let previousHints = self.hints
        self.screen = screen
        self.hints = hints
        screenDidChange(from: previousScreen, previousHints: previousHints)
    }

    /// Subclasses should override this method in order to update any relevant UI bits when the screen model changes.
    open func screenDidChange(from previousScreen: ScreenType, previousHints: ContainerHints) {

    }

}

extension ScreenViewController {

    public static func description(for screen: ScreenType) -> ViewControllerDescription {
        return ViewControllerDescription(
            build: { Self(screen: screen, hints: $0) },
            update: { $0.update(screen: screen, hints: $1) })
    }

}

#endif
