import WorkflowUI


/// A `ModalContainerScreen` displays a base screen and optionally one or more modals on top of it.
public struct ModalContainerScreen: Screen {

    /// The base screen to show underneath any modally presented screens.
    public var baseScreen: AnyScreen

    /// Modally presented screens
    public var modals: [Modal]

    public init<ScreenType: Screen>(baseScreen: ScreenType, modals: [Modal]) {
        self.baseScreen = AnyScreen(baseScreen)
        self.modals = modals
    }

    public func viewControllerDescription(environment: ViewEnvironment) -> ViewControllerDescription {
        return ModalContainerViewController.description(for: self, environment: environment)
    }
}


extension ModalContainerScreen {

    /// Represents a single screen to be displayed modally
    public struct Modal {

        public enum Style: Hashable {
            case fullScreen(animated: Bool)
            case card
        }

        /// The screen to be displayed
        public var screen: AnyScreen

        /// The style in which the screen should be presented
        public var style: Style

        /// A key used to differentiate modal screens during updates
        public var key: String

        public init<ScreenType: Screen>(screen: ScreenType, style: Style = .fullScreen(animated: true), key: String = "") {
            self.screen = AnyScreen(screen)
            self.style = style
            self.key = key
        }
    }

}
