import UIKit
import ReactiveSwift
import Result
import Workflow


/// Binds a root workflow to a renderable view controller.
public final class ContainerViewController<Output>: UIViewController {

    /// Emits output events from the bound workflow.
    public let output: Signal<Output, NoError>

    internal let renderer: UIKitRenderableViewController

    private let workflowHost: Any

    private let rendering: Property<Screen>

    private let (lifetime, token) = Lifetime.make()

    private init(workflowHost: Any, rendering: Property<Screen>, output: Signal<Output, NoError>, viewRegistry: ViewRegistry) {
        self.workflowHost = workflowHost
        self.renderer = UIKitRenderableViewController(viewRegistry: viewRegistry)
        self.rendering = rendering
        self.output = output

        super.init(nibName: nil, bundle: nil)

        rendering
            .signal
            .take(during: lifetime)
            .observeValues { [weak self] screen in
                self?.render(screen: screen)
            }

        render(screen: rendering.value)
    }

    public convenience init<W: Workflow>(workflow: W, viewRegistry: ViewRegistry) where W.Rendering == Screen, W.Output == Output {
        let host = WorkflowHost(workflow: workflow)
        self.init(
            workflowHost: host,
            rendering: host.rendering,
            output: host.output,
            viewRegistry: viewRegistry)
    }

    public convenience init<W: Workflow>(workflow: W, viewRegistry: ViewRegistry) where W.Rendering: Screen, W.Output == Output {
        let host = WorkflowHost(workflow: workflow)
        self.init(
            workflowHost: host,
            rendering: host.rendering.map { $0 as Screen },
            output: host.output,
            viewRegistry: viewRegistry)
    }

    required public init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    private func render(screen: Screen) {
        renderer.render(screen: screen)
    }

    override public func viewDidLoad() {
        super.viewDidLoad()

        view.backgroundColor = .white

        addChild(renderer)
        view.addSubview(renderer.view)
        renderer.didMove(toParent: self)
    }

    override public func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        renderer.view.frame = view.bounds
    }

    public override var childForStatusBarStyle: UIViewController? {
        return renderer
    }

    public override var childForStatusBarHidden: UIViewController? {
        return renderer
    }

    public override var childForHomeIndicatorAutoHidden: UIViewController? {
        return renderer
    }

    public override var childForScreenEdgesDeferringSystemGestures: UIViewController? {
        return renderer
    }

    public override var supportedInterfaceOrientations: UIInterfaceOrientationMask {
        return renderer.supportedInterfaceOrientations
    }

}
