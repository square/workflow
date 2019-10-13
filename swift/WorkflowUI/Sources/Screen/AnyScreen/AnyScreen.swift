import UIKit

public struct AnyScreen: Screen {

    public typealias ViewController = AnyScreenViewController
    private let _makeViewController: () -> AnyScreenViewController
    private let _updateViewController: (AnyScreenViewController) -> Void

    public func makeViewController() -> AnyScreenViewController {
        return _makeViewController()
    }

    public func update(viewController: AnyScreenViewController) {
        _updateViewController(viewController)
    }

    public init<T: Screen>(_ screen: T) {

        if let screen = screen as? AnyScreen {
            self = screen
            return
        }

        _makeViewController = {
            return AnyScreenViewController(wrappedScreen: screen)
        }

        _updateViewController = { viewController in
            viewController.update(from: screen)
        }

    }

}
