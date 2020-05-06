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

import WorkflowUI

/// A `ModalContainerScreen` displays a base screen and optionally one or more modals on top of it.
public struct ModalContainerScreen<BaseScreen: Screen>: Screen {
    /// The base screen to show underneath any modally presented screens.
    public let baseScreen: BaseScreen

    /// Modally presented screens
    public let modals: [ModalContainerScreenModal]

    public init(baseScreen: BaseScreen, modals: [ModalContainerScreenModal]) {
        self.baseScreen = baseScreen
        self.modals = modals
    }

    public func viewControllerDescription(environment: ViewEnvironment) -> ViewControllerDescription {
        return ModalContainerViewController.description(for: self, environment: environment)
    }
}

/// Represents a single screen to be displayed modally
public struct ModalContainerScreenModal {
    public enum Style: Equatable {
        // full screen modal presentation
        case fullScreen
        // formsheet or pagesheet like modal presentation
        case sheet
    }

    /// The screen to be displayed
    public var screen: AnyScreen

    /// A bool used to specify whether presentation should be animated
    public var animated: Bool

    /// The style in which the screen should be presented
    public var style: Style

    /// A key used to differentiate modal screens during updates
    public var key: AnyHashable

    public init<Key: Hashable>(screen: AnyScreen, style: Style = .fullScreen, key: Key, animated: Bool = true) {
        self.screen = screen
        self.style = style
        self.key = AnyHashable(key)
        self.animated = animated
    }
}
