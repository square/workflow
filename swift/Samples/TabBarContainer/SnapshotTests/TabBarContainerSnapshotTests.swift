/*
* Copyright 2019 Square Inc.
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
import Workflow
import WorkflowUI
import XCTest
import FBSnapshotTestCase
@testable import TabBarContainer


class TabBarContainerSnapshotTests: FBSnapshotTestCase {
    override func setUp() {
        super.setUp()
        recordMode = false
        folderName = "TabBarContainerSnapshotTests"
        fileNameOptions = [.device, .OS, .screenSize, .screenScale]
    }
    
    func test_tabBarScreen() {
        let viewController = TabBarScreenContainerViewController(
            screen: makeTabBarScreen(),
            environment: .empty
        )

        viewController.view.layoutIfNeeded()

        FBSnapshotVerifyView(
            viewController.view,
            identifier: name,
            suffixes: ["_64"]
        )
    }
}


fileprivate extension TabBarContainerSnapshotTests {
    func makeTabBarScreen() -> TabBarContainerScreen {
        let fooScreen = TestScreen(
            title: "Foo Screen",
            backgroundColor: .red
        ).tabScreen(barItem: .init(
            title: "Foo",
            image: fooImage,
            badge: .value(10))) {
            //Do nothing on select
        }
        
        let bazScreen = TestScreen(
            title: "Baz Screen",
            backgroundColor: .blue
        ).tabScreen(barItem: .init(
            title: "Baz",
            image: bazImage,
            badge: .value(1))) {
            //Do nothing on select
        }
        
        return TabBarContainerScreen(
            screens: [fooScreen, bazScreen],
            selectedIndex: 0
        )
    }
    
    var bazImage: UIImage {
        if #available(iOS 13.0, *) {
            return UIImage(systemName: "triangle") ?? UIImage()
        } else {
           return UIImage()
        }
    }
    
    var fooImage: UIImage {
        if #available(iOS 13.0, *) {
            return UIImage(systemName: "square") ?? UIImage()
        } else {
           return UIImage()
        }
    }
}


fileprivate struct TestScreen: Screen {
    let title: String
    let backgroundColor: UIColor

    func viewControllerDescription(environment: ViewEnvironment) -> ViewControllerDescription {
        return FooScreenViewController.description(for: self, environment: environment)
    }
}


fileprivate final class FooScreenViewController: ScreenViewController<TestScreen> {
    
    private lazy var titleLabel: UILabel = .init()
    
    required init(screen: TestScreen, environment: ViewEnvironment) {
        super.init(screen: screen, environment: environment)
        
        update(with: screen)
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        titleLabel.textAlignment = .center
        view.addSubview(titleLabel)
        
        NSLayoutConstraint.activate([
            titleLabel.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            titleLabel.centerYAnchor.constraint(equalTo: view.centerYAnchor),
            ])
    }

    override func screenDidChange(from previousScreen: TestScreen, previousEnvironment: ViewEnvironment) {
        update(with: screen)
    }
    
    private func update(with screen: TestScreen) {
        view.backgroundColor = screen.backgroundColor
        titleLabel.text = screen.title
    }
}

