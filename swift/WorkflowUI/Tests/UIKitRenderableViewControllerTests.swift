import XCTest
@testable import WorkflowUI


fileprivate struct ScreenA: Screen {}
fileprivate class ScreenAViewController: ScreenViewController<ScreenA> {}

fileprivate struct ScreenB: Screen {}
fileprivate class ScreenBViewController: ScreenViewController<ScreenB> {}


class UIKitRenderableViewControllerTests: XCTestCase {

    func test_render_adds_view_controller_if_none() {
        var viewRegistry = ViewRegistry()

        viewRegistry.register(screenViewControllerType: ScreenAViewController.self)

        let uikitRenderableViewController = UIKitRenderableViewController(viewRegistry: viewRegistry)

        XCTAssertNil(uikitRenderableViewController.currentScreenViewController)

        uikitRenderableViewController.render(screen: ScreenA())

        let currentScreen = uikitRenderableViewController.currentScreenViewController
        XCTAssertNotNil(currentScreen)
    }

    func test_render_same_screen_type_creates_one_view_controller() {
        var viewRegistry = ViewRegistry()

        viewRegistry.register(screenViewControllerType: ScreenAViewController.self)

        let uikitRenderableViewController = UIKitRenderableViewController(viewRegistry: viewRegistry)

        XCTAssertNil(uikitRenderableViewController.currentScreenViewController)

        uikitRenderableViewController.render(screen: ScreenA())

        let expected = uikitRenderableViewController.currentScreenViewController
        XCTAssertNotNil(expected)

        // Render the same screen type again. The view controller should not be replaced.
        uikitRenderableViewController.render(screen: ScreenA())
        XCTAssertNotNil(uikitRenderableViewController.currentScreenViewController)

        let actual = uikitRenderableViewController.currentScreenViewController
        XCTAssertEqual(expected, actual)
    }

    func test_render_generates_new_view_controller_on_different_screen_type() {
        var viewRegistry = ViewRegistry()

        viewRegistry.register(screenViewControllerType: ScreenAViewController.self)
        viewRegistry.register(screenViewControllerType: ScreenBViewController.self)

        let uikitRenderableViewController = UIKitRenderableViewController(viewRegistry: viewRegistry)

        XCTAssertNil(uikitRenderableViewController.currentScreenViewController)

        uikitRenderableViewController.render(screen: ScreenA())

        let currentScreen = uikitRenderableViewController.currentScreenViewController
        XCTAssertNotNil(currentScreen)
        let expected = currentScreen

        // Render a different screen type. The view controller should be replaced.
        uikitRenderableViewController.render(screen: ScreenB())
        XCTAssertNotNil(uikitRenderableViewController.currentScreenViewController)

        let actual = uikitRenderableViewController.currentScreenViewController
        XCTAssertNotEqual(expected, actual)
    }
}
