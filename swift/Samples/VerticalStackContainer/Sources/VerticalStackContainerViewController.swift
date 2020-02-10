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

public final class VerticalStackContainerViewController<TopScreenType: Screen, MiddleScreenType: Screen, BottomScreenType: Screen>: ScreenViewController<VerticalStackContainerViewController.ContainerScreen> {
    public typealias ContainerScreen = VerticalStackContainerScreen<TopScreenType, MiddleScreenType, BottomScreenType>
    
    private var topContentViewController: ScreenViewController<TopScreenType>? = nil
    private lazy var topContainerView: ContainerView = .init()

    private lazy var topMiddleSeparatorView: UIView = .init()

    private var middleContentViewController: ScreenViewController<MiddleScreenType>? = nil
    private lazy var middleContainerView: ContainerView = .init()

    private lazy var middleBottomSeparatorView: UIView = .init()

    private var bottomContentViewController: ScreenViewController<BottomScreenType>? = nil
    private lazy var bottomContainerView: ContainerView = .init()

    private var currentSeparatorHeight: CGFloat {
        didSet {
            guard oldValue != currentSeparatorHeight else {
                return
            }

            needsAnimatedLayout = true
        }
    }

    private var needsAnimatedLayout = false

    required init(screen: ContainerScreen, viewRegistry: ViewRegistry) {
        print("\(#file) - \(#function)")

        currentSeparatorHeight = screen.separatorHeight
        
        super.init(screen: screen, viewRegistry: viewRegistry)
    }

    override public func screenDidChange(from previousScreen: ContainerScreen) {
        print("\(#file) - \(#function)")

        update(with: screen)
    }

    private func update(with screen: ContainerScreen) {
        print("\(#file) - \(#function)")

        topMiddleSeparatorView.backgroundColor = screen.separatorColor
        middleBottomSeparatorView.backgroundColor = screen.separatorColor
        
        topContentViewController?.update(screen: screen.topScreen)
        middleContentViewController?.update(screen: screen.middleScreen)
        bottomContentViewController?.update(screen: screen.bottomScreen)

        //Intentional force of layout pass after updating the child view controllers
        print("\(#file) - \(#function) - Intentional Layout Pass")
        view.layoutIfNeeded()

        currentSeparatorHeight = screen.separatorHeight

        if needsAnimatedLayout {
            print("\(#file) - \(#function) - Animiate")
            needsAnimatedLayout = false

            UIView.animate(withDuration: 0.25) {
                self.view.setNeedsLayout()
                self.view.layoutIfNeeded()
            }
        }
    }

    override public func viewDidLoad() {
        super.viewDidLoad()

        print("\(#file) - \(#function)")

        view.addSubview(topContainerView)
        view.addSubview(topMiddleSeparatorView)
        view.addSubview(middleContainerView)
        view.addSubview(middleBottomSeparatorView)
        view.addSubview(bottomContainerView)
        
        self.topContentViewController = embed(screen.topScreen, in: topContainerView)
        self.middleContentViewController = embed(screen.middleScreen, in: middleContainerView)
        self.bottomContentViewController = embed(screen.bottomScreen, in: bottomContainerView)
        
        update(with: screen)
    }
    
    public override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        print("\(#file) - \(#function)")

        let viewWidth = view.bounds.width
        let topViewHeight = topContentViewController?.preferredContentSize.height ?? 0
        let middleViewHeight = middleContentViewController?.preferredContentSize.height ?? 0

        let topViewYOffset: CGFloat = 0.0
        let topMiddleSeparatorYOffset = topViewYOffset + topViewHeight
        let middleViewYOffset = topMiddleSeparatorYOffset + currentSeparatorHeight
        let middleBottomSeparatorYOffset = middleViewYOffset + middleViewHeight
        let bottomViewYOffset = middleBottomSeparatorYOffset + currentSeparatorHeight
        let bottomViewHeight = view.bounds.height - bottomViewYOffset

        topContainerView.frame = CGRect(x: 0, y: topViewYOffset, width: viewWidth, height: topViewHeight)
        topMiddleSeparatorView.frame = CGRect(x: 0, y: topMiddleSeparatorYOffset, width: viewWidth, height: currentSeparatorHeight)
        middleContainerView.frame =  CGRect(x: 0, y: middleViewYOffset, width: viewWidth, height: middleViewHeight)
        middleBottomSeparatorView.frame = CGRect(x: 0, y: middleBottomSeparatorYOffset, width: viewWidth, height: currentSeparatorHeight)
        bottomContainerView.frame = CGRect(x: 0, y: bottomViewYOffset, width: viewWidth, height: bottomViewHeight)
    }

    override public func preferredContentSizeDidChange(forChildContentContainer container: UIContentContainer) {
        super.preferredContentSizeDidChange(forChildContentContainer: container)

        print("\(#file) - \(#function)")

        needsAnimatedLayout = true
    }
}

fileprivate extension ScreenViewController {
    func embed<ScreenType: Screen>(_ screen: ScreenType, in containerView: ContainerView) -> ScreenViewController<ScreenType> {
        print("\(#file) - \(#function)")

        let viewController = viewRegistry.provideView(for: screen)

        addChild(viewController)
        containerView.contentView.addSubview(viewController.view)
        viewController.didMove(toParent: self)

        return viewController
    }
}
