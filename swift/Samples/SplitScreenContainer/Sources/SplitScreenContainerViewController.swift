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

public final class SplitScreenContainerViewController<LeadingScreenType: Screen, TrailingScreenType: Screen>: ScreenViewController<SplitScreenContainerViewController.ContainerScreen> {
    public typealias ContainerScreen = SplitScreenContainerScreen<LeadingScreenType, TrailingScreenType>
    
    private var leadingContentViewController: ScreenViewController<LeadingScreenType>? = nil
    private lazy var leadingContainerView: ContainerView = .init()

    private lazy var separatorView: UIView = .init()

    private var trailingContentViewController: ScreenViewController<TrailingScreenType>? = nil
    private lazy var trailingContainerView: ContainerView = .init()
    
    private var currentRatio: CGFloat {
        didSet {
            guard oldValue != currentRatio else {
                return
            }
            
            needsAnimatedLayout = true
        }
    }

    private var currentSeparatorWidth: CGFloat {
        didSet {
            guard oldValue != currentSeparatorWidth else {
                return
            }

            needsAnimatedLayout = true
        }
    }

    private var needsAnimatedLayout = false

    required init(screen: ContainerScreen, viewRegistry: ViewRegistry) {
        currentRatio = screen.ratio
        currentSeparatorWidth = screen.separatorWidth
        
        super.init(screen: screen, viewRegistry: viewRegistry)
    }

    override public func screenDidChange(from previousScreen: ContainerScreen) {
        update(with: screen)
    }

    private func update(with screen: ContainerScreen) {
        separatorView.backgroundColor = screen.separatorColor
        
        leadingContentViewController?.update(screen: screen.leadingScreen)
        trailingContentViewController?.update(screen: screen.trailingScreen)
        
        //Intentional force of layout pass after updating the child view controllers
        view.layoutIfNeeded()

        currentRatio = screen.ratio
        currentSeparatorWidth = screen.separatorWidth

        if needsAnimatedLayout {
            needsAnimatedLayout = false

            UIView.animate(withDuration: 0.25) {
                self.view.setNeedsLayout()
                self.view.layoutIfNeeded()
            }
        }
    }

    override public func viewDidLoad() {
        super.viewDidLoad()
        
        view.addSubview(leadingContainerView)
        view.addSubview(separatorView)
        view.addSubview(trailingContainerView)
        
        self.leadingContentViewController = embed(screen.leadingScreen, in: leadingContainerView)
        self.trailingContentViewController = embed(screen.trailingScreen, in: trailingContainerView)
        
        update(with: screen)
    }
    
    public override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        
        let distance = view.bounds.width * currentRatio
        
        let (firstSlice, trailingRect) = view.bounds.divided(atDistance: distance, from: .minXEdge)
        
        let (leadingRect, separatorRect) = firstSlice.divided(atDistance: distance - currentSeparatorWidth, from: .minXEdge)

        leadingContainerView.frame = isLayoutDirectionRightToLeft ? trailingRect: leadingRect
        
        separatorView.frame = separatorRect
        
        trailingContainerView.frame = isLayoutDirectionRightToLeft ? leadingRect : trailingRect
    }
}

fileprivate extension UIViewController {
    var isLayoutDirectionRightToLeft: Bool {
        if #available(iOS 10.0, *) {
            return traitCollection.layoutDirection == .rightToLeft
        } else {
            return UIView.userInterfaceLayoutDirection(for: view.semanticContentAttribute) == .rightToLeft
        }
    }
}

fileprivate extension ScreenViewController {
    func embed<ScreenType: Screen>(_ screen: ScreenType, in containerView: ContainerView) -> ScreenViewController<ScreenType> {
        let viewController = viewRegistry.provideView(for: screen)

        addChild(viewController)
        containerView.contentView.addSubview(viewController.view)
        viewController.didMove(toParent: self)

        return viewController
    }
}
