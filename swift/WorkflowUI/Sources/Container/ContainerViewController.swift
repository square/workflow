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

#if canImport(UIKit)

import UIKit
import ReactiveSwift
import Workflow


/// Drives view controllers from a root Workflow.
public final class ContainerViewController<Output, ScreenType>: UIViewController where ScreenType: Screen {

    /// Emits output events from the bound workflow.
    public let output: Signal<Output, Never>

    internal let rootViewController: DescribedViewController

    private let workflowHost: Any

    private let rendering: Property<ScreenType>

    private let (lifetime, token) = Lifetime.make()

    private init(workflowHost: Any, rendering: Property<ScreenType>, output: Signal<Output, Never>) {
        self.workflowHost = workflowHost
        self.rootViewController = DescribedViewController(screen: rendering.value)
        self.rendering = rendering
        self.output = output

        super.init(nibName: nil, bundle: nil)

        rendering
            .signal
            .take(during: lifetime)
            .observeValues { [weak self] screen in
                self?.render(screen: screen)
            }
    }

    public convenience init<W: Workflow>(workflow: W) where W.Rendering == ScreenType, W.Output == Output {
        let host = WorkflowHost(workflow: workflow)
        self.init(
            workflowHost: host,
            rendering: host.rendering,
            output: host.output)
    }

    required public init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    private func render(screen: ScreenType) {
        rootViewController.update(screen: screen)
    }

    override public func viewDidLoad() {
        super.viewDidLoad()

        view.backgroundColor = .white

        addChild(rootViewController)
        view.addSubview(rootViewController.view)
        rootViewController.didMove(toParent: self)
    }

    override public func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        rootViewController.view.frame = view.bounds
    }

    public override var childForStatusBarStyle: UIViewController? {
        return rootViewController
    }

    public override var childForStatusBarHidden: UIViewController? {
        return rootViewController
    }

    public override var childForHomeIndicatorAutoHidden: UIViewController? {
        return rootViewController
    }

    public override var childForScreenEdgesDeferringSystemGestures: UIViewController? {
        return rootViewController
    }

    public override var supportedInterfaceOrientations: UIInterfaceOrientationMask {
        return rootViewController.supportedInterfaceOrientations
    }

}

#endif
