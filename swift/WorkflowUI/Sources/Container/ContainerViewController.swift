import UIKit
import ReactiveSwift
import Result
import Workflow


/// Drives view controllers from a root Workflow.
public final class ContainerViewController<WorkflowType>: UIViewController where WorkflowType: Workflow, WorkflowType.Rendering: Screen {

    public typealias Output = WorkflowType.Output
    public typealias ScreenType = WorkflowType.Rendering

    /// Emits output events from the bound workflow.
    public let output: Signal<Output, NoError>

    internal let rootViewController: ScreenViewController<ScreenType>

    private let workflowHost: WorkflowHost<WorkflowType>

    private let rendering: Property<ScreenType>

    private let (lifetime, token) = Lifetime.make()

    private init(workflowHost: WorkflowHost<WorkflowType>, rendering: Property<ScreenType>, output: Signal<Output, NoError>, viewRegistry: ViewRegistry) {
        self.workflowHost = workflowHost
        self.rootViewController = viewRegistry.provideView(for: rendering.value)
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

    public convenience init(workflow: WorkflowType, viewRegistry: ViewRegistry) {
        let host = WorkflowHost(workflow: workflow)
        self.init(
            workflowHost: host,
            rendering: host.rendering,
            output: host.output,
            viewRegistry: viewRegistry)
    }

    required public init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    public func update(workflow: WorkflowType) {
        workflowHost.update(workflow: workflow)
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
