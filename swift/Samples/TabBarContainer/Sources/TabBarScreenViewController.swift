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

internal final class TabBarScreenContainerViewController<Content: Screen>: ScreenViewController<TabBarScreen<Content>>, UITabBarDelegate {
    private var containerView: ContainerView
    private var contentViewController: DescribedViewController
    
    private let tabBar: UITabBar
    
    required init(screen: TabBarScreen<Content>, environment: ViewEnvironment) {
        self.containerView = ContainerView()
        
        self.contentViewController = DescribedViewController(
            screen: screen.currentScreen,
            environment: environment
        )
        
        self.tabBar = UITabBar()
        
        super.init(screen: screen, environment: environment)
        
        self.tabBar.delegate = self
    }

    override internal func screenDidChange(from previousScreen: TabBarScreen<Content>, previousEnvironment: ViewEnvironment) {
        update()
    }

    private func update() {
        contentViewController.update(screen: screen.currentScreen, environment: environment)
        
        var selectedTabBarItem: UITabBarItem?
        
        let tabBarItems = screen.barItems.enumerated().map { (index, barItem) -> UITabBarItem in
            let tabBarItem = UITabBarItem(
                title: barItem.title,
                image: barItem.image,
                selectedImage: barItem.selectedImage
            )
            
            tabBarItem.badgeValue = barItem.badge.stringValue
            
            if index == screen.selectedIndex {
                selectedTabBarItem = tabBarItem
            }
            
            return tabBarItem
        }
        
        tabBar.setItems(tabBarItems, animated: false)
        tabBar.selectedItem = selectedTabBarItem
    }

    override internal func viewDidLoad() {
        super.viewDidLoad()

        view.addSubview(containerView)
        view.addSubview(tabBar)

        addChild(contentViewController)
        containerView.contentView.addSubview(contentViewController.view)
        contentViewController.didMove(toParent: self)
        
        update()
    }
    
    internal override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        
        let tabBarHeight: CGFloat = 83
        
        containerView.frame = view.bounds.inset(by:
            UIEdgeInsets(
                top: 0,
                left: 0,
                bottom: tabBarHeight,
                right: 0
            )
        )
        
        tabBar.frame = CGRect(
            x: 0,
            y: view.bounds.height - tabBarHeight,
            width: view.bounds.width,
            height: tabBarHeight
        )
    }
    
    override var childForStatusBarStyle: UIViewController? {
        return contentViewController
    }

    override var childForStatusBarHidden: UIViewController? {
        return contentViewController
    }

    override var childForHomeIndicatorAutoHidden: UIViewController? {
        return contentViewController
    }

    override var childForScreenEdgesDeferringSystemGestures: UIViewController? {
        return contentViewController
    }

    public override var supportedInterfaceOrientations: UIInterfaceOrientationMask {
        return contentViewController.supportedInterfaceOrientations
    }
    
    func tabBar(_ tabBar: UITabBar, didSelect item: UITabBarItem) {
        guard let items = tabBar.items else {
            fatalError("tabBar cannot have zero items when a selection event occurs")
        }
        
        guard let selectedIndex = items.firstIndex(of: item) else {
            fatalError("tabBar firstIndex of item cannot be nil")
        }
        
        precondition(items.indices.contains(selectedIndex), "selectedIndex \(selectedIndex) is invalid for items \(items)")
        
        screen.barItems[selectedIndex].onSelect()
    }
}
