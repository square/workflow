import ReactiveSwift

/// Screens are the building blocks of an interactive application.
///
/// Conforming types contain any information needed to populate a screen: data,
/// styling, event handlers, etc.
public protocol Screen {}

extension Screen {

    public func inflate(from registry: ViewRegistry) -> AnyScreenViewController {
        return registry.provideView(for: self)
    }

}
