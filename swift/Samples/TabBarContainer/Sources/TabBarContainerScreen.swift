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
