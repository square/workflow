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

public final class SplitScreenContainerViewController<LeftScreenType: Screen, RightScreenType: Screen>: ScreenViewController<SplitScreenContainerViewController.ContainerScreen> {
    public typealias ContainerScreen = SplitScreenContainerScreen<LeftScreenType, RightScreenType>
    
    private var leftContentViewController: ScreenViewController<LeftScreenType>? = nil
    private lazy var leftContainerView: UIView = .init()

    private lazy var separatorView: UIView = .init()

    private var rightContentViewController: ScreenViewController<RightScreenType>? = nil
    private lazy var rightContainerView: UIView = .init()
    
    private var currentRatio: CGFloat {
        didSet {
            guard oldValue != currentRatio else {
                return
            }
            
            UIView.animate(withDuration: 0.25) {
                self.view.setNeedsLayout()
                self.view.layoutIfNeeded()
            }
        }
    }

    required init(screen: ContainerScreen, viewRegistry: ViewRegistry) {
        currentRatio = screen.ratio
        
        super.init(screen: screen, viewRegistry: viewRegistry)
    }

    override public func screenDidChange(from previousScreen: ContainerScreen) {
        update(with: screen)
    }

    private func update(with screen: ContainerScreen) {
        separatorView.backgroundColor = screen.separatorColor
        
        leftContentViewController?.update(screen: screen.leftScreen)
        rightContentViewController?.update(screen: screen.rightScreen)
        
        //Intentional force of layout pass after updating the child view controllers
        view.layoutIfNeeded()
        
        //Update the current ratio in an animated fashion if needed
        currentRatio = screen.ratio
    }

    override public func viewDidLoad() {
        super.viewDidLoad()
        
        view.addSubview(leftContainerView)
        view.addSubview(separatorView)
        view.addSubview(rightContainerView)
        
        self.leftContentViewController = embed(screen.leftScreen, in: leftContainerView)
        self.rightContentViewController = embed(screen.rightScreen, in: rightContainerView)
        update(with: screen)
    }
    
    public override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        
        let distance = view.bounds.width * currentRatio
        
        let (firstSlice, rightRect) = view.bounds.divided(atDistance: distance, from: .minXEdge)
        
        let (leftRect, separatorRect) = firstSlice.divided(atDistance: distance - 1, from: .minXEdge)
        
        leftContainerView.frame = leftRect
        separatorView.frame = separatorRect
        rightContainerView.frame = rightRect
        leftContentViewController?.view.frame = leftRect
        rightContentViewController?.view.frame = CGRect(x: 0, y: 0, width: rightRect.width, height: rightRect.height)
    }
    
    private func embed<ScreenType: Screen>(_ screen: ScreenType, in containerView: UIView) -> ScreenViewController<ScreenType> {
        let viewController = viewRegistry.provideView(for: screen)
        
        addChild(viewController)
        containerView.addSubview(viewController.view)
        viewController.didMove(toParent: self)
        
        return viewController
    }
}
