import UIKit
import ReactiveSwift


/// Maps screen models into live views.
///
/// In order for the registry to handle a given screen type, a view factory
/// must first be registered using `register(screenType:factory:)`, where the
/// factory is a simple closure that is responsible for instantiating a view.
public struct ViewRegistry {

    /// Defines a closure that instantiates a live view instance.
    private typealias Factory<T: Screen> = (T, ViewRegistry) -> ScreenViewController<T>

    private var factories: [ObjectIdentifier:Any] = [:]

    /// Initializes an empty registry.
    public init() {
        // `AnyScreen` is a WorkflowUI primitive; all view registries should support them.
        register(screenViewControllerType: AnyScreenViewController.self)
    }

    /// Convenience registration method that wraps a simple `UIViewController` in a `ScreenViewController` to provide convenient
    /// update methods.
    public mutating func register<ViewControllerType, ScreenType>(screenViewControllerType: ViewControllerType.Type) where ViewControllerType: ScreenViewController<ScreenType> {

        let factory: Factory<ScreenType> = { screen, registry -> ScreenViewController<ScreenType> in
            return ViewControllerType(screen: screen, viewRegistry: registry)
        }
        factories[ObjectIdentifier(ScreenType.self)] = factory

    }

    /// Returns `true` is a factory block has previously been registered for the screen type `T`.
    public func canProvideView<T>(for screenType: T.Type) -> Bool where T : Screen {
        return factories[ObjectIdentifier(screenType)] != nil
    }

    /// Instantiates and returns a view instance for the given screen model.
    ///
    /// Note that you must check `canProvideView(for:)` before calling this method. Calling `provideView(for:)`
    /// with a screen type that was not previously registered is a programmer error, and the application will crash.
    internal func provideView<T>(for screen: T) -> ScreenViewController<T> where T : Screen {
        guard let factory = factories[ObjectIdentifier(T.self)] as? Factory<T> else {
            fatalError("The screen type \(T.self) was not registered with the view registry.")
        }
        return factory(screen, self)
    }

    /// Merges from another registry. If a screen type is registered with both,
    /// the definition from the other registry will replace the original in `self`.
    public mutating func merge(with otherRegistry: ViewRegistry) {
        factories.merge(otherRegistry.factories) { (_, new) -> Any in
            return new
        }
    }

    /// The returned value is identical to the result of calling `merge(from:)` on `self`.
    public func merged(with otherRegistry: ViewRegistry) -> ViewRegistry {
        var result = self
        result.merge(with: otherRegistry)
        return result
    }

}
