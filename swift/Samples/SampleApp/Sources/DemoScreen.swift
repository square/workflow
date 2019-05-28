import Workflow
import WorkflowUI


struct DemoScreen: Screen {
    let title: String
    let color: UIColor
    let onTitleTap: () -> Void

    let subscribeTitle: String
    let onSubscribeTapped: () -> Void

    let refreshText: String
    let isRefreshEnabled: Bool
    let onRefreshTap: () -> Void
}


extension ViewRegistry {

    public mutating func registerDemoScreen() {
        self.register(screenViewControllerType: DemoViewController.self)
    }

}


fileprivate final class DemoViewController: ScreenViewController<DemoScreen> {

    private let titleButton: UIButton
    private let subscribeButton: UIButton
    private let statusLabel: UILabel
    private let refreshButton: UIButton

    required init(screen: DemoScreen, viewRegistry: ViewRegistry) {
        titleButton = UIButton(frame: .zero)
        subscribeButton = UIButton(frame: .zero)
        statusLabel = UILabel(frame: .zero)
        refreshButton = UIButton(frame: .zero)
        super.init(screen: screen, viewRegistry: viewRegistry)

        update(with: screen)
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        titleButton.addTarget(self, action: #selector(titleButtonPressed(sender:)), for: .touchUpInside)

        subscribeButton.addTarget(self, action: #selector(subscribePressed(sender:)), for: .touchUpInside)

        statusLabel.textAlignment = .center

        refreshButton.addTarget(self, action: #selector(refreshButtonPressed(sender:)), for: .touchUpInside)
        refreshButton.setTitle("Reverse!", for: .normal)

        view.addSubview(titleButton)
        view.addSubview(subscribeButton)
        view.addSubview(statusLabel)
        view.addSubview(refreshButton)
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()

        let height: CGFloat = 44.0
        let inset: CGFloat = 12.0

        var (top, bottom) = view.bounds.divided(atDistance: view.bounds.height / 2, from: CGRectEdge.minYEdge)

        top.size.height -= (height / 2.0)
        bottom.origin.y += (height)
        bottom.size.height -= (height / 2.0)

        titleButton.frame = top

        subscribeButton.frame = CGRect(
            x: 0.0,
            y: top.maxY,
            width: top.size.width,
            height: height)

        let yOffset = bottom.midY - (height / 2.0)

        refreshButton.frame = CGRect(
            x: bottom.origin.x,
            y: yOffset,
            width: bottom.size.width,
            height: height)
        .insetBy(dx: inset, dy: 0.0)

        statusLabel.frame = CGRect(
            x: refreshButton.frame.origin.x,
            y: yOffset - height,
            width: refreshButton.frame.size.width,
            height: height)
    }

    override func screenDidChange(from previousScreen: DemoScreen) {
        update(with: screen)
    }

    private func update(with screen: DemoScreen) {
        titleButton.setTitle(screen.title, for: .normal)
        titleButton.backgroundColor = screen.color

        subscribeButton.setTitle(screen.subscribeTitle, for: .normal)
        subscribeButton.backgroundColor = .black

        statusLabel.text = screen.refreshText

        refreshButton.isEnabled = screen.isRefreshEnabled
        refreshButton.backgroundColor = UIColor(
            red: 41/255,
            green: 150/255,
            blue: 204/255,
            alpha: screen.isRefreshEnabled ? 1.0 : 0.5)
    }

    @objc private func titleButtonPressed(sender: UIButton) {
        screen.onTitleTap()
    }

    @objc private func subscribePressed(sender: UIButton) {
        screen.onSubscribeTapped()
    }

    @objc private func refreshButtonPressed(sender: UIButton) {
        screen.onRefreshTap()
    }
}
