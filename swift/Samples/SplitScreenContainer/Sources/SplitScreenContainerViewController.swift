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

import Workflow
import WorkflowUI

internal final class SplitScreenContainerViewController<LeadingScreenType: Screen, TrailingScreenType: Screen>: ScreenViewController<SplitScreenContainerViewController.ContainerScreen> {
    internal typealias ContainerScreen = SplitScreenContainerScreen<LeadingScreenType, TrailingScreenType>

    private var leadingContentViewController: DescribedViewController
    private lazy var leadingContainerView: ContainerView = .init()

    private lazy var separatorView: UIView = .init()

    private var trailingContentViewController: DescribedViewController
    private lazy var trailingContainerView: ContainerView = .init()

    private var needsAnimatedLayout = false

    required init(screen: ContainerScreen, environment: ViewEnvironment) {
        self.leadingContentViewController = DescribedViewController(
            screen: screen.leadingScreen,
            environment: environment
                .setting(keyPath: \.splitScreenPosition, to: .leading)
        )
        self.trailingContentViewController = DescribedViewController(
            screen: screen.trailingScreen,
            environment: environment
                .setting(keyPath: \.splitScreenPosition, to: .trailing)
        )
        super.init(screen: screen, environment: environment)
    }

    override internal func screenDidChange(from previousScreen: ContainerScreen, previousEnvironment: ViewEnvironment) {
        super.screenDidChange(from: previousScreen, previousEnvironment: previousEnvironment)

        if screen.ratio != previousScreen.ratio {
            needsAnimatedLayout = true
        }
        if screen.separatorWidth != previousScreen.separatorWidth {
            needsAnimatedLayout = true
        }
        update(with: screen)
    }

    private func update(with screen: ContainerScreen) {
        separatorView.backgroundColor = screen.separatorColor

        leadingContentViewController.update(
            screen: screen.leadingScreen,
            environment: environment
                .setting(keyPath: \.splitScreenPosition, to: .leading)
        )
        trailingContentViewController.update(
            screen: screen.trailingScreen,
            environment: environment
                .setting(keyPath: \.splitScreenPosition, to: .trailing)
        )

        // Intentional force of layout pass after updating the child view controllers
        view.layoutIfNeeded()

        if needsAnimatedLayout {
            needsAnimatedLayout = false

            UIView.animate(withDuration: 0.25) {
                self.view.setNeedsLayout()
                self.view.layoutIfNeeded()
            }
        }
    }

    override internal func viewDidLoad() {
        super.viewDidLoad()

        view.addSubview(leadingContainerView)
        view.addSubview(separatorView)
        view.addSubview(trailingContainerView)

        addChild(leadingContentViewController)
        leadingContainerView.contentView.addSubview(leadingContentViewController.view)
        leadingContentViewController.didMove(toParent: self)

        addChild(trailingContentViewController)
        trailingContainerView.contentView.addSubview(trailingContentViewController.view)
        trailingContentViewController.didMove(toParent: self)

        update(with: screen)
    }

    override internal func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()

        let distance = view.bounds.width * screen.ratio

        let (firstSlice, trailingRect) = view.bounds.divided(atDistance: distance, from: .minXEdge)

        let (leadingRect, separatorRect) = firstSlice.divided(atDistance: distance - screen.separatorWidth, from: .minXEdge)

        leadingContainerView.frame = isLayoutDirectionRightToLeft ? trailingRect : leadingRect

        separatorView.frame = separatorRect

        trailingContainerView.frame = isLayoutDirectionRightToLeft ? leadingRect : trailingRect
    }
}

private extension UIViewController {
    var isLayoutDirectionRightToLeft: Bool {
        if #available(iOS 10.0, *) {
            return traitCollection.layoutDirection == .rightToLeft
        } else {
            return UIView.userInterfaceLayoutDirection(for: view.semanticContentAttribute) == .rightToLeft
        }
    }
}
