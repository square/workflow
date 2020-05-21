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

import UIKit
import Workflow
import WorkflowUI

/// Container for showing workflow screens modally over a base screen.
internal final class ModalContainerViewController<ModalScreen: Screen>: ScreenViewController<ModalContainerScreen<ModalScreen>> {
    var baseScreenViewController: DescribedViewController

    private var presentedScreens: [ModallyPresentedScreen] = []

    private var topmostScreenViewController: DescribedViewController? {
        if let topModal = presentedScreens.last {
            return topModal.viewController
        } else {
            return baseScreenViewController
        }
    }

    required init(screen: ModalContainerScreen<ModalScreen>, environment: ViewEnvironment) {
        self.baseScreenViewController = DescribedViewController(screen: screen.baseScreen, environment: environment)
        super.init(screen: screen, environment: environment)
    }

    override func screenDidChange(from previousScreen: ModalContainerScreen<ModalScreen>, previousEnvironment: ViewEnvironment) {
        super.screenDidChange(from: previousScreen, previousEnvironment: previousEnvironment)

        baseScreenViewController.update(screen: screen.baseScreen, environment: environment)

        // Sort our existing modals into keyed buckets. This will typically contain a single view controller
        // per value, but duplicate keys/styles/screen types will result in more. In that case, we simply dequeue them in order during the update cycle (first to last)
        var previousScreens: [ModalIdentifier: [ModallyPresentedScreen]] = Dictionary(presentedScreens.map { ($0.identifier, [$0]) }, uniquingKeysWith: +)

        // Will contain the new set of presented screens by the end of this method
        var newScreens: [ModallyPresentedScreen] = []

        var screensNeedingAppearanceTransition: [ModallyPresentedScreen] = []

        for modal in screen.modals {
            if let existing = previousScreens[modal.identifier]?.removeFirst() {
                // Update existing screen view controller
                existing.viewController.update(screen: modal.screen, environment: environment)
                newScreens.append(
                    ModallyPresentedScreen(
                        viewController: existing.viewController,
                        style: modal.style,
                        key: modal.key,
                        dimmingView: existing.dimmingView,
                        animated: modal.animated
                    ))
            } else {
                // Make a new screen view controller
                let newViewController = DescribedViewController(screen: modal.screen, environment: environment)
                addChild(newViewController)
                view.addSubview(newViewController.view)
                newViewController.didMove(toParent: self)

                // Create and set a dimming view if the modal is in popover/formsheet or pagesheet style
                var newDimmingView: UIView?
                if modal.style == .sheet {
                    let dimmingView = UIView()
                    dimmingView.backgroundColor = UIColor(white: 0, alpha: 0.5)
                    dimmingView.frame = view.bounds
                    dimmingView.alpha = 0
                    view.addSubview(dimmingView)
                    newDimmingView = dimmingView
                }

                let modal = ModallyPresentedScreen(
                    viewController: newViewController,
                    style: modal.style,
                    key: modal.key,
                    dimmingView: newDimmingView,
                    animated: modal.animated
                )
                newScreens.append(modal)
                screensNeedingAppearanceTransition.append(modal)
            }
        }

        for modal in previousScreens.values.flatMap({ $0 }) {
            // Anything left behind in `previousScreens` should be removed

            let displayInfo = ModalDisplayInfo(containerSize: view.bounds.size, style: modal.style, animated: modal.animated)

            modal.viewController.willMove(toParent: nil)

            UIView.animate(
                withDuration: displayInfo.duration,
                delay: 0.0,
                options: displayInfo.animationOptions,
                animations: {
                    modal.viewController.view.frame = displayInfo.outgoingFinalFrame
                    modal.viewController.view.transform = displayInfo.outgoingFinalTransform
                    modal.viewController.view.alpha = displayInfo.outgoingFinalAlpha
                    modal.dimmingView?.alpha = 0
                },
                completion: { _ in
                    modal.viewController.view.removeFromSuperview()
                    modal.viewController.removeFromParent()
                    modal.dimmingView?.removeFromSuperview()
                }
            )
        }

        for modal in screensNeedingAppearanceTransition {
            let displayInfo = ModalDisplayInfo(containerSize: view.bounds.size, style: modal.style, animated: modal.animated)
            modal.viewController.view.frame = displayInfo.incomingInitialFrame
            modal.viewController.view.transform = displayInfo.incomingInitialTransform
            modal.viewController.view.alpha = displayInfo.incomingInitialAlpha

            UIView.animate(
                withDuration: displayInfo.duration,
                delay: 0.0,
                options: displayInfo.animationOptions,
                animations: {
                    modal.viewController.view.bounds = CGRect(
                        origin: .zero,
                        size: displayInfo.frame.size
                    )
                    modal.viewController.view.center = CGPoint(
                        x: displayInfo.frame.midX,
                        y: displayInfo.frame.midY
                    )
                    modal.viewController.view.transform = displayInfo.transform
                    modal.viewController.view.alpha = displayInfo.alpha
                    modal.dimmingView?.alpha = 1
                },
                completion: { _ in
                    UIAccessibility.post(notification: .screenChanged, argument: nil)
                }
            )
        }

        // Update our state to reflect the new screens post-update
        presentedScreens = newScreens

        // Sort our views. We go front to back to allow dismissed views to appear above currently presented views (this will matter after transition support is added).
        for modal in presentedScreens.reversed() {
            view.sendSubviewToBack(modal.viewController.view)
            if let dimmingView = modal.dimmingView {
                view.sendSubviewToBack(dimmingView)
            }
        }

        view.sendSubviewToBack(baseScreenViewController.view)

        setNeedsStatusBarAppearanceUpdate()

        if #available(iOS 11.0, *) {
            setNeedsUpdateOfHomeIndicatorAutoHidden()
            setNeedsUpdateOfScreenEdgesDeferringSystemGestures()
        }

        // Set the topmost screen to be the accessibility modal
        presentedScreens.last?.viewController.view.accessibilityViewIsModal = true
        for modal in presentedScreens.dropLast() {
            modal.viewController.view.accessibilityViewIsModal = false
        }
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        addChild(baseScreenViewController)
        view.addSubview(baseScreenViewController.view)
        baseScreenViewController.didMove(toParent: self)
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        baseScreenViewController.view.frame = view.bounds

        presentedScreens.forEach {
            let displayInfo = ModalDisplayInfo(containerSize: view.bounds.size, style: $0.style, animated: $0.animated)
            $0.viewController.view.frame = displayInfo.frame
            $0.dimmingView?.frame = view.bounds
        }
    }

    override var childForStatusBarStyle: UIViewController? {
        return topmostScreenViewController
    }

    override var childForStatusBarHidden: UIViewController? {
        return topmostScreenViewController
    }

    override var childForHomeIndicatorAutoHidden: UIViewController? {
        return topmostScreenViewController
    }

    override var childForScreenEdgesDeferringSystemGestures: UIViewController? {
        return topmostScreenViewController
    }

    override public var supportedInterfaceOrientations: UIInterfaceOrientationMask {
        return topmostScreenViewController?.supportedInterfaceOrientations ?? super.supportedInterfaceOrientations
    }
}

private struct ModallyPresentedScreen {
    var viewController: DescribedViewController
    var style: ModalContainerScreenModal.Style
    var key: AnyHashable
    var dimmingView: UIView?
    var animated: Bool

    var identifier: ModalIdentifier {
        return ModalIdentifier(
            style: style,
            key: key,
            animated: animated
        )
    }
}

extension ModalContainerScreenModal {
    fileprivate var identifier: ModalIdentifier {
        return ModalIdentifier(
            style: style,
            key: key,
            animated: animated
        )
    }
}

private struct ModalIdentifier: Hashable {
    var style: ModalContainerScreenModal.Style
    var key: AnyHashable
    var animated: Bool
}

private struct ModalDisplayInfo {
    var frame: CGRect
    var alpha: CGFloat
    var transform: CGAffineTransform
    var incomingInitialFrame: CGRect
    var outgoingFinalFrame: CGRect
    var incomingInitialTransform: CGAffineTransform
    var outgoingFinalTransform: CGAffineTransform
    var incomingInitialAlpha: CGFloat
    var outgoingFinalAlpha: CGFloat
    var duration: TimeInterval
    var animationOptions: UIView.AnimationOptions

    init(containerSize: CGSize, style: ModalContainerScreenModal.Style, animated: Bool) {
        // Configure all properties so that they default to fullScreen/sheet animation.
        frame = CGRect(origin: .zero, size: containerSize)
        alpha = 1.0
        transform = .identity
        incomingInitialFrame = CGRect(
            x: frame.origin.x,
            y: containerSize.height,
            width: frame.size.width,
            height: frame.size.height
        )
        outgoingFinalFrame = CGRect(
            x: frame.origin.x,
            y: containerSize.height,
            width: frame.size.width,
            height: frame.size.height
        )
        incomingInitialTransform = .identity
        outgoingFinalTransform = .identity
        incomingInitialAlpha = 1.0
        outgoingFinalAlpha = 1.0
        duration = 0.5
        animationOptions = UIView.AnimationOptions(rawValue: 7 << 16)

        switch style {
        case .fullScreen:
            // Clear the default fullscreen animation configuration.
            if !animated {
                duration = 0
                animationOptions = UIView.AnimationOptions(rawValue: 0)
                incomingInitialFrame = frame
                outgoingFinalFrame = frame
            }
        case .sheet:
            if UIDevice.current.userInterfaceIdiom == .phone {
                // On iPhone always show modal in fullscreen.
                break
            }

            let popoverSideLength = min(containerSize.width, containerSize.height)
            let popoverSize = CGSize(width: popoverSideLength, height: popoverSideLength)
            let popOverOrigin = CGPoint(
                x: (containerSize.width - popoverSideLength) / 2,
                y: (containerSize.height - popoverSideLength) / 2
            )

            frame = CGRect(origin: popOverOrigin, size: popoverSize)

            duration = 0.1
            animationOptions = UIView.AnimationOptions(rawValue: 0)

            // Do not animate frame.
            incomingInitialFrame = frame
            outgoingFinalFrame = frame

            // Animate transform and alpha.
            incomingInitialTransform = CGAffineTransform(scaleX: 0.85, y: 0.85)
            outgoingFinalTransform = CGAffineTransform(scaleX: 0.85, y: 0.85)
            incomingInitialAlpha = 0.0
            outgoingFinalAlpha = 0.0
        }
    }
}
