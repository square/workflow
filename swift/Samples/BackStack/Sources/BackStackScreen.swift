import WorkflowUI

public struct BackStackScreen: Screen {
    public var items: [BackStackItem]

    public init(items: [BackStackItem]) {
        self.items = items
    }
}


extension ViewRegistry {

    public mutating func registerBackStackScreen() {
        register(screenViewControllerType: BackStackViewController.self)
    }

}
