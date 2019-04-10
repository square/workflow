import XCTest
@testable import WorkflowUI


class AnyScreenTests: XCTestCase {

    func test_asAnyScreen_returns_same_screen_if_anyScreen() {
        let screen = AnyScreen(ScreenA())

        XCTAssertTrue(type(of: screen.wrappedScreen) == ScreenA.self)

        // Wrapping again nests another AnyScreen.
        let wrappedScreen = AnyScreen(screen)
        XCTAssertFalse(type(of: wrappedScreen.wrappedScreen) == ScreenA.self)

        let asAnyScreen = screen.asAnyScreen()
        XCTAssertTrue(type(of: asAnyScreen.wrappedScreen) == ScreenA.self)

    }
}


fileprivate struct ScreenA: Screen {}
