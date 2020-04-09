/*
 * Copyright 2020 Square Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#if canImport(UIKit)

import XCTest

import ReactiveSwift
import Workflow
@testable import WorkflowUI

class DescribedViewControllerTests: XCTestCase {

    // MARK: - Tests

    func test_init() {
        // Given
        let screen = TestScreen.counter(0)

        // When
        let describedViewController = DescribedViewController(screen: screen, environment: .empty)

        // Then
        guard
            let currentViewController = describedViewController.currentViewController as? CounterViewController
        else {
            XCTFail("Expected a \(String(reflecting: CounterViewController.self)), but got:  \(describedViewController.currentViewController)")
            return
        }

        XCTAssertEqual(currentViewController.count, 0)
        XCTAssertFalse(describedViewController.isViewLoaded)
        XCTAssertFalse(currentViewController.isViewLoaded)
        XCTAssertNil(currentViewController.parent)
    }

    func test_viewDidLoad() {
        // Given
        let screen = TestScreen.counter(0)
        let describedViewController = DescribedViewController(screen: screen, environment: .empty)

        // When
        _ = describedViewController.view

        // Then
        XCTAssertEqual(describedViewController.currentViewController.parent, describedViewController)
        XCTAssertNotNil(describedViewController.currentViewController.viewIfLoaded?.superview)
    }

    func test_update_toCompatibleDescription_beforeViewLoads() {
        // Given
        let screenA = TestScreen.counter(0)
        let screenB = TestScreen.counter(1)

        let describedViewController = DescribedViewController(screen: screenA, environment: .empty)
        let initialChildViewController = describedViewController.currentViewController

        // When
        describedViewController.update(screen: screenB, environment: .empty)

        // Then
        XCTAssertEqual(initialChildViewController, describedViewController.currentViewController)
        XCTAssertEqual((describedViewController.currentViewController as? CounterViewController)?.count, 1)
        XCTAssertFalse(describedViewController.isViewLoaded)
        XCTAssertFalse(describedViewController.currentViewController.isViewLoaded)
        XCTAssertNil(describedViewController.currentViewController.parent)
    }

    func test_update_toCompatibleDescription_afterViewLoads() {
        // Given
        let screenA = TestScreen.counter(0)
        let screenB = TestScreen.counter(1)

        let describedViewController = DescribedViewController(screen: screenA, environment: .empty)
        let initialChildViewController = describedViewController.currentViewController

        // When
        _ = describedViewController.view
        describedViewController.update(screen: screenB, environment: .empty)

        // Then
        XCTAssertEqual(initialChildViewController, describedViewController.currentViewController)
        XCTAssertEqual((describedViewController.currentViewController as? CounterViewController)?.count, 1)
    }

    func test_update_toIncompatibleDescription_beforeViewLoads() {
        // Given
        let screenA = TestScreen.counter(0)
        let screenB = TestScreen.message("Test")

        let describedViewController = DescribedViewController(screen: screenA, environment: .empty)
        let initialChildViewController = describedViewController.currentViewController

        // When
        describedViewController.update(screen: screenB, environment: .empty)

        // Then
        XCTAssertNotEqual(initialChildViewController, describedViewController.currentViewController)
        XCTAssertEqual((describedViewController.currentViewController as? MessageViewController)?.message, "Test")
        XCTAssertFalse(describedViewController.isViewLoaded)
        XCTAssertFalse(describedViewController.currentViewController.isViewLoaded)
        XCTAssertNil(describedViewController.currentViewController.parent)
    }

    func test_update_toIncompatibleDescription_afterViewLoads() {
        // Given
        let screenA = TestScreen.counter(0)
        let screenB = TestScreen.message("Test")

        let describedViewController = DescribedViewController(screen: screenA, environment: .empty)
        let initialChildViewController = describedViewController.currentViewController

        // When
        _ = describedViewController.view
        describedViewController.update(screen: screenB, environment: .empty)

        // Then
        XCTAssertNotEqual(initialChildViewController, describedViewController.currentViewController)
        XCTAssertEqual((describedViewController.currentViewController as? MessageViewController)?.message, "Test")
        XCTAssertNil(initialChildViewController.parent)
        XCTAssertEqual(describedViewController.currentViewController.parent, describedViewController)
        XCTAssertNil(initialChildViewController.viewIfLoaded?.superview)
        XCTAssertNotNil(describedViewController.currentViewController.viewIfLoaded?.superview)
    }

    func test_childViewControllerFor() {
        // Given
        let screen = TestScreen.counter(0)

        let describedViewController = DescribedViewController(screen: screen, environment: .empty)
        let currentViewController = describedViewController.currentViewController

        // When, Then
        XCTAssertEqual(describedViewController.childForStatusBarStyle, currentViewController)
        XCTAssertEqual(describedViewController.childForStatusBarHidden, currentViewController)
        XCTAssertEqual(describedViewController.childForHomeIndicatorAutoHidden, currentViewController)
        XCTAssertEqual(describedViewController.childForScreenEdgesDeferringSystemGestures, currentViewController)
        XCTAssertEqual(describedViewController.supportedInterfaceOrientations, currentViewController.supportedInterfaceOrientations)
    }

    func test_childViewControllerFor_afterIncompatibleUpdate() {
        // Given
        let screenA = TestScreen.counter(0)
        let screenB = TestScreen.message("Test")

        let describedViewController = DescribedViewController(screen: screenA, environment: .empty)
        let initialChildViewController = describedViewController.currentViewController

        describedViewController.update(screen: screenB, environment: .empty)
        let currentViewController = describedViewController.currentViewController

        // When, Then
        XCTAssertNotEqual(initialChildViewController, currentViewController)
        XCTAssertEqual(describedViewController.childForStatusBarStyle, currentViewController)
        XCTAssertEqual(describedViewController.childForStatusBarHidden, currentViewController)
        XCTAssertEqual(describedViewController.childForHomeIndicatorAutoHidden, currentViewController)
        XCTAssertEqual(describedViewController.childForScreenEdgesDeferringSystemGestures, currentViewController)
        XCTAssertEqual(describedViewController.supportedInterfaceOrientations, currentViewController.supportedInterfaceOrientations)
    }

    func test_preferredContentSizeDidChange() {
        // Given
        let screenA = TestScreen.counter(1)
        let screenB = TestScreen.counter(2)

        let describedViewController = DescribedViewController(screen: screenA, environment: .empty)
        let containerViewController = ContainerViewController(describedViewController: describedViewController)

        // When
        let expectation = self.expectation(description: "did observe size changes")
        expectation.expectedFulfillmentCount = 2

        var observedSizes: [CGSize] = []
        let disposable = containerViewController.preferredContentSizeSignal.observeValues {
            observedSizes.append($0)
            expectation.fulfill()
        }

        defer { disposable?.dispose() }

        _ = containerViewController.view
        describedViewController.update(screen: screenB, environment: .empty)

        // Then
        let expectedSizes = [CGSize(width: 10, height: 0), CGSize(width: 20, height: 0)]
        waitForExpectations(timeout: 1, handler: nil)
        XCTAssertEqual(observedSizes, expectedSizes)
    }

    func test_preferredContentSizeDidChange_afterIncompatibleUpdate() {
        // Given
        let screenA = TestScreen.counter(1)
        let screenB = TestScreen.message("Test")
        let screenC = TestScreen.message("Testing")

        let describedViewController = DescribedViewController(screen: screenA, environment: .empty)
        let containerViewController = ContainerViewController(describedViewController: describedViewController)

        // When
        let expectation = self.expectation(description: "did observe size changes")
        expectation.expectedFulfillmentCount = 3

        var observedSizes: [CGSize] = []
        let disposable = containerViewController.preferredContentSizeSignal.observeValues {
            observedSizes.append($0)
            expectation.fulfill()
        }

        defer { disposable?.dispose() }

        _ = containerViewController.view
        describedViewController.update(screen: screenB, environment: .empty)
        describedViewController.update(screen: screenC, environment: .empty)

        // Then
        let expectedSizes = [
            CGSize(width: 10, height: 0),
            CGSize(width: 40, height: 0),
            CGSize(width: 70, height: 0),
        ]

        waitForExpectations(timeout: 1, handler: nil)
        XCTAssertEqual(observedSizes, expectedSizes)
    }
}

// MARK: - Helper Types

fileprivate enum TestScreen: Screen, Equatable {
    case counter(Int)
    case message(String)

    func viewControllerDescription(environment: ViewEnvironment) -> ViewControllerDescription {
        switch self {
        case let .counter(count):
            return ViewControllerDescription(
                build: { CounterViewController(count: count) },
                update: { $0.count = count }
            )

        case let .message(message):
            return ViewControllerDescription(
                build: { MessageViewController(message: message) },
                update: { $0.message = message }
            )
        }
    }
}

fileprivate class ContainerViewController: UIViewController {
    let describedViewController: DescribedViewController

    var preferredContentSizeSignal: Signal<CGSize, Never> { return signal.skipRepeats() }

    private let (signal, sink) = Signal<CGSize, Never>.pipe()

    init(describedViewController: DescribedViewController) {
        self.describedViewController = describedViewController
        super.init(nibName: nil, bundle: nil)
    }

    @available(*, unavailable) required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        addChild(describedViewController)
        describedViewController.view.frame = view.bounds
        describedViewController.view.autoresizingMask = [.flexibleWidth, .flexibleHeight]
        view.addSubview(describedViewController.view)
        describedViewController.didMove(toParent: self)
    }

    override func preferredContentSizeDidChange(forChildContentContainer container: UIContentContainer) {
        super.preferredContentSizeDidChange(forChildContentContainer: container)

        guard container === describedViewController else { return }

        sink.send(value: container.preferredContentSize)
    }
}

fileprivate class CounterViewController: UIViewController {
    var count: Int {
        didSet {
            preferredContentSize.width = CGFloat(count * 10)
        }
    }

    init(count: Int) {
        self.count = count
        super.init(nibName: nil, bundle: nil)
        preferredContentSize.width = CGFloat(count * 10)
    }

    @available(*, unavailable) required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}

fileprivate class MessageViewController: UIViewController {
    var message: String {
        didSet {
            preferredContentSize.width = CGFloat(message.count * 10)
        }
    }

    init(message: String) {
        self.message = message
        super.init(nibName: nil, bundle: nil)
        preferredContentSize.width = CGFloat(message.count * 10)
    }

    @available(*, unavailable) required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}

#endif
