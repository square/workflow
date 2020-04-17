import WorkflowUI

/// A `ModalContainerScreen` displays a base screen and optionally one or more modals on top of it.
public struct ModalContainerScreen<BaseScreen: Screen>: Screen
{

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
        case sheet
        case popover
    }

    /// The screen to be displayed
    public var screen: AnyScreen
    
    /// A bool used to specify whether presentation should b animated
    public var animated: Bool

    /// The style in which the screen should be presented
    public var style: Style

    /// A key used to differentiate modal screens during updates
    public var key: String

    public init(screen: AnyScreen, style: Style = .sheet, key: String = "", animated: Bool = true) {
        self.screen = screen
        self.style = style
        self.key = key
        self.animated = animated
    }
}
