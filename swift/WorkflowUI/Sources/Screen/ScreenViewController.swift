import UIKit


/// Generic base class that should be subclassed in order to to define a UI implementation that is powered by the
/// given screen type.
open class ScreenViewController<ScreenType: Screen>: UIViewController {

    public final let viewRegistry: ViewRegistry

    private var currentScreen: ScreenType

    public final var screen: ScreenType {
        return currentScreen
    }

    public final var screenType: Screen.Type {
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

    public final func update(screen: ScreenType) {
        let previousScreen = currentScreen
        currentScreen = screen
        screenDidChange(from: previousScreen)
    }

    /// Subclasses should override this method in order to update any relevant UI bits when the screen model changes.
    open func screenDidChange(from previousScreen: ScreenType) {

    }

}
