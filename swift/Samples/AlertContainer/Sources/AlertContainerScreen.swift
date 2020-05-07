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

/// An `AlertContainerScreen` displays a base screen with an optional alert over top of it.
public struct AlertContainerScreen<BaseScreen: Screen>: Screen {
    /// The base screen to show underneath any visible alert.
    public var baseScreen: BaseScreen

    /// The presented alert.
    public var alert: Alert?

    public init(baseScreen: BaseScreen, alert: Alert? = nil) {
        self.baseScreen = baseScreen
        self.alert = alert
    }

    public func viewControllerDescription(environment: ViewEnvironment) -> ViewControllerDescription {
        return AlertContainerViewController.description(for: self, environment: environment)
    }
}

public struct Alert {
    public var title: String
    public var message: String
    public var actions: [AlertAction]

    public init(title: String, message: String, actions: [AlertAction]) {
        self.title = title
        self.message = message
        self.actions = actions
    }
}

public struct AlertAction {
    public var title: String
    public var style: Style
    public var handler: () -> Void

    public init(title: String, style: Style, handler: @escaping () -> Void) {
        self.title = title
        self.style = style
        self.handler = handler
    }
}

extension AlertAction {
    public enum Style {
        case `default`
        case cancel
        case destructive
    }
}
