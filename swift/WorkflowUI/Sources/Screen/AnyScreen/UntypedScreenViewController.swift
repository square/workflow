// Internal API for working with a screen view controller when the specific screen type is unknown.
protocol UntypedScreenViewController {
    var screenType: Screen.Type { get }
    func update(untypedScreen: Screen)
}

extension ScreenViewController: UntypedScreenViewController {

    // `var screenType: Screen.Type` is already present in ScreenViewController

    func update(untypedScreen: Screen) {
        guard let typedScreen = untypedScreen as? ScreenType else {
            fatalError("Screen type mismatch: \(self) expected to receive a screen of type \(ScreenType.self), but instead received a screen of type \(type(of: screen))")
        }
        update(screen: typedScreen)
    }

}
