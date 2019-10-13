/// Screens are the building blocks of an interactive application.
///
/// Conforming types contain any information needed to populate a screen: data,
/// styling, event handlers, etc.
public protocol Screen {
    associatedtype ViewController: UIViewController

    func makeViewController() -> ViewController
    func update(viewController: ViewController)
}
