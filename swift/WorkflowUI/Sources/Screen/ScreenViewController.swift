import UIKit


/// Abstract superclass for view controllers that participate in the Container / ViewRegistry architecture.
open class AnyScreenViewController: UIViewController {

    /// The screen (view model) type that is used by this view controller.
    public var screenType: Screen.Type {
        fatalError("screenType has not been implemented")
    }

    /// Updates the view controller with the given screen.
    ///
    /// The screen's type **must** be the same type returned by the `screenType` property.
    public func update(screen: Screen) {
        fatalError("update(screen:) has not been implemented")
    }

}

/// Generic base class that should be subclassed in order to to define a UI implementation that is powered by the
/// given screen type.
open class ScreenViewController<ScreenType: Screen>: AnyScreenViewController {

    public final let viewRegistry: ViewRegistry

    private var currentScreen: ScreenType

    public final var screen: ScreenType {
        return currentScreen
    }

    public final override var screenType: Screen.Type {
        return ScreenType.self
    }

    public required init(screen: ScreenType, viewRegistry: ViewRegistry) {
        self.currentScreen = screen
        self.viewRegistry = viewRegistry
        super.init(nibName: nil, bundle: nil)
    }

    @available(*, unavailable)
    required public init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    public final override func update(screen: Screen) {
        guard let typedScreen = screen as? ScreenType else {
            fatalError("Screen type mismatch: \(self) expected to receive a screen of type \(ScreenType.self), but instead received a screen of type \(type(of: screen))")
        }

        let previousScreen = currentScreen
        currentScreen = typedScreen
        screenDidChange(from: previousScreen)
    }

    /// Subclasses should override this method in order to update any relevant UI bits when the screen model changes.
    open func screenDidChange(from previousScreen: ScreenType) {

    }

}
