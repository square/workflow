import UIKit

public struct AnyScreen: Screen {
    internal let wrappedScreen: Screen
    private let viewControllerBuilder: (ViewRegistry) -> AnyScreenViewController.WrappedViewController

    public init<T: Screen>(_ screen: T) {
        self.wrappedScreen = screen
        self.viewControllerBuilder = { viewRegistry in
            return viewRegistry.provideView(for: screen)
        }
    }

    // Internal method to inflate the wrapped screen from a view registry
    func makeViewController(from viewRegistry: ViewRegistry) -> AnyScreenViewController.WrappedViewController {
        return viewControllerBuilder(viewRegistry)
    }
}
