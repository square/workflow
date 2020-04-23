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

public struct TabBarContainerScreen {
    public var screens: [TabScreen]
    public var selectedIndex: Int

    public init(screens: [TabScreen], selectedIndex: Int) {
        precondition(
            selectedIndex < screens.count,
            "selectedIndex \(selectedIndex) is invalid for items \(screens)"
        )

        self.screens = screens
        self.selectedIndex = selectedIndex
    }

}

extension TabBarContainerScreen: Screen {
    public func viewControllerDescription(environment: ViewEnvironment) -> ViewControllerDescription {
        return TabBarScreenContainerViewController.description(
            for: self,
            environment: environment
        )
    }
}

public struct BarItem {

    public var title: String
    public var image: UIImage
    public var selectedImage: UIImage?
    public var badge: Badge

    public init(
        title: String,
        image: UIImage,
        selectedImage: UIImage? = nil,
        badge: Badge = .none
    ) {
        self.title = title
        self.image = image
        self.selectedImage = selectedImage
        self.badge = badge
    }

}

extension BarItem {
    public enum Badge {
        case none
        case value(Int)
        case text(String)

        internal var stringValue: String? {
            switch self {
            case .none:
                return nil
            case .value(let value):
                return String(value)
            case .text(let text):
                return text
            }
        }
    }
}
