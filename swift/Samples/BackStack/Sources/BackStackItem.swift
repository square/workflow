import WorkflowUI

public struct BackStackItem {

    public var key: String
    public var title: String
    public var screen: Screen
    public var backAction = BackAction.none

    public enum BackAction {
        case none
        case back(handler: () -> Void)
    }

    public init(key: String, title: String, screen: Screen) {
        self.key = key
        self.title = title
        self.screen = screen
    }

}
