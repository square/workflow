import WorkflowUI

public struct TabBarScreen<Content> {

    public var currentScreen: Content
    public var barItems: [BarItem]
    public var selectedIndex: Int

    public init(currentScreen: Content, barItems: [BarItem], selectedIndex: Int) {
        precondition(barItems.indices.contains(selectedIndex), "selectedIndex \(selectedIndex) is invalid for items \(barItems)")

        self.currentScreen = currentScreen
        self.barItems = barItems
        self.selectedIndex = selectedIndex
    }

}

extension TabBarScreen: Screen where Content: Screen {
    public func viewControllerDescription(environment: ViewEnvironment) -> ViewControllerDescription {
        return TabBarScreenContainerViewController.description(for: self, environment: environment)
    }
}

public struct BarItem {

    public var title: String
    public var image: UIImage
    public var selectedImage: UIImage?
    public var badge: Badge
    public var onSelect: () -> Void

    public init(title: String, image: UIImage, selectedImage: UIImage? = nil, badge: Badge = .none, onSelect: @escaping () -> Void) {
        self.title = title
        self.image = image
        self.selectedImage = selectedImage
        self.badge = badge
        self.onSelect = onSelect
    }

}

extension BarItem {
    public enum Badge {
        case none
        case value(Int)
        case text(String)
        
        internal var stringValue: String? {
            switch self  {
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
