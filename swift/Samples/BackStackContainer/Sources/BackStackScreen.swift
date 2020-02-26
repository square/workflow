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
import WorkflowUI


public struct BackStackScreen: Screen {
    var items: [Item]

    public init(items: [BackStackScreen.Item]) {
        self.items = items
    }

    public var viewControllerDescription: ViewControllerDescription {
        return BackStackContainer.description(for: self)
    }
}

extension BackStackScreen {
    /// A specific item in the back stack. The key and screen type is used to differentiate reused vs replaced screens.
    public struct Item {
        public var key: AnyHashable
        public var screen: AnyScreen
        var screenType: Any.Type
        public var barVisibility: BarVisibility

        public init<ScreenType: Screen, Key: Hashable>(key: Key?, screen: ScreenType, barVisibility: BarVisibility) {
            self.screen = AnyScreen(screen)
            self.screenType = ScreenType.self

            if let key = key {
                self.key = AnyHashable(key)
            } else {
                self.key = AnyHashable(ObjectIdentifier(ScreenType.self))
            }
            self.barVisibility = barVisibility
        }

        public init<ScreenType: Screen>(screen: ScreenType, barVisibility: BarVisibility) {
            let key = Optional<AnyHashable>.none
            self.init(key: key, screen: screen, barVisibility: barVisibility)
        }

        public init<ScreenType: Screen, Key: Hashable>(key: Key?, screen: ScreenType, barContent: BackStackScreen.BarContent) {
            self.init(key: key, screen: screen, barVisibility: .visible(barContent))
        }

        public init<ScreenType: Screen>(screen: ScreenType, barContent: BackStackScreen.BarContent) {
            let key = Optional<AnyHashable>.none
            self.init(key: key, screen: screen, barContent: barContent)
        }

        public init<ScreenType: Screen, Key: Hashable>(key: Key?, screen: ScreenType) {
            let barVisibility: BarVisibility = .visible(BarContent())
            self.init(key: key, screen: screen, barVisibility: barVisibility)
        }

        public init<ScreenType: Screen>(screen: ScreenType) {
            let key = Optional<AnyHashable>.none
            self.init(key: key, screen: screen)
        }
    }
}


extension BackStackScreen {
    public enum BarVisibility {
        case hidden
        case visible(BarContent)
    }
}

extension BackStackScreen {
    public struct BarContent {
        var title: Title
        var leftItem: BarButtonItem
        var rightItem: BarButtonItem

        public enum BarButtonItem {
            case none
            case button(Button)
        }

        public init(title: Title = .none, leftItem: BarButtonItem = .none, rightItem: BarButtonItem = .none) {
            self.title = title
            self.leftItem = leftItem
            self.rightItem = rightItem
        }

        public init(title: String, leftItem: BarButtonItem = .none, rightItem: BarButtonItem = .none) {
            self.init(title: .text(title), leftItem: leftItem, rightItem: rightItem)
        }

    }
}


extension BackStackScreen.BarContent {
    public enum Title {
        case none
        case text(String)
    }

    public enum ButtonContent {
        case text(String)
        case icon(UIImage)
    }

    public struct Button {
        var content: ButtonContent
        var handler: () -> Void

        public init(content: ButtonContent, handler: @escaping () -> Void) {
            self.content = content
            self.handler = handler
        }

        /// Convenience factory for a default back button.
        public static func back(handler: @escaping () -> Void) -> Button {
            return Button(content: .text("Back"), handler: handler)
        }
    }
}
