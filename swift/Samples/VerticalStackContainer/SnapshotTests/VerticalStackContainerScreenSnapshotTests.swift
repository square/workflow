import Workflow
import WorkflowUI
import XCTest
import FBSnapshotTestCase
@testable import VerticalStackContainer


class VerticalStackContainerScreenSnapshotTests: FBSnapshotTestCase {
    override func setUp() {
        super.setUp()
        recordMode = false
    }
    
    func test_stacking() {
        var viewRegistry = ViewRegistry()
        viewRegistry.register(screenViewControllerType: FooScreenViewController.self)

        let verticalStackContainerScreen = VerticalStackContainerScreen(
            topScreen: FooScreen(title: "Top screen", backgroundColor: .green, viewTapped: {}),
            middleScreen: FooScreen(title: "Middle screen", backgroundColor: .red, viewTapped: {}),
            bottomScreen: FooScreen(title: "Bottom screen", backgroundColor: .blue, viewTapped: {})
        )

        let viewController = VerticalStackContainerViewController(
            screen: verticalStackContainerScreen,
            viewRegistry: viewRegistry
        )
        viewController.view.layoutIfNeeded()

        FBSnapshotVerifyView(viewController.view, identifier: name, suffixes: ["_64"])
    }
}


fileprivate struct FooScreen: Screen {
    let title: String
    let backgroundColor: UIColor
    let viewTapped: () -> Void
}


fileprivate final class FooScreenViewController: ScreenViewController<FooScreen> {

    private lazy var titleLabel: UILabel = .init()
    private let titleLabelPadding: CGFloat = 50.0
    private lazy var tapGestureRecognizer: UITapGestureRecognizer = .init()

    required init(screen: FooScreen, viewRegistry: ViewRegistry) {
        super.init(screen: screen, viewRegistry: viewRegistry)

        update(with: screen)
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        tapGestureRecognizer.addTarget(self, action: #selector(viewTapped))
        view.addGestureRecognizer(tapGestureRecognizer)

        titleLabel.font = UIFont.preferredFont(forTextStyle: .title1)
        titleLabel.textAlignment = .center
        view.addSubview(titleLabel)
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()

        titleLabel.frame = view.bounds

        titleLabel.preferredMaxLayoutWidth = view.bounds.width - (titleLabelPadding * 2)

        calculatePreferredSize()
    }

    private func calculatePreferredSize() {
        let preferredHeight = titleLabel.intrinsicContentSize.height + (titleLabelPadding * 2)

        preferredContentSize = CGSize(width: view.bounds.width, height: preferredHeight)
    }

    override func screenDidChange(from previousScreen: FooScreen) {
        update(with: screen)
    }

    private func update(with screen: FooScreen) {
        view.backgroundColor = screen.backgroundColor

        titleLabel.text = screen.title

        calculatePreferredSize()
    }

    @objc
    private func viewTapped() {
        screen.viewTapped()
    }
}
