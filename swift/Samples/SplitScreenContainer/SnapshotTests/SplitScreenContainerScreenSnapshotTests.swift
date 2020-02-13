import Workflow
import WorkflowUI
import XCTest
import FBSnapshotTestCase
@testable import SplitScreenContainer


class SplitScreenContainerScreenSnapshotTests: FBSnapshotTestCase {
    override func setUp() {
        super.setUp()
        recordMode = false
        folderName = "SplitScreenContainerScreenSnapshotTests"
        fileNameOptions = [.device]
    }
    
    func test_splitRatio() {
        var viewRegistry = ViewRegistry()
        viewRegistry.register(screenViewControllerType: FooScreenViewController.self)

        let ratios: [String : CGFloat] = [
            "third" : .third,
            "quarter" : .quarter,
            "half" : .half,
            "custom" : 0.3125
        ]

        for (name, ratio) in ratios {
            let splitScreenContainerScreen = SplitScreenContainerScreen(
                leadingScreen: FooScreen(title: "Leading screen", backgroundColor: .green, viewTapped: {}),
                trailingScreen: FooScreen(title: "Trailing screen", backgroundColor: .red, viewTapped: {}),
                ratio: ratio
            )

            let viewController = SplitScreenContainerViewController(
                screen: splitScreenContainerScreen,
                viewRegistry: viewRegistry
            )
            viewController.view.layoutIfNeeded()

            FBSnapshotVerifyView(viewController.view, identifier: name, suffixes: ["_64"])
        }
    }
}


fileprivate struct FooScreen: Screen {
    let title: String
    let backgroundColor: UIColor
    let viewTapped: () -> Void
}


fileprivate final class FooScreenViewController: ScreenViewController<FooScreen> {
    
    private lazy var titleLabel: UILabel = .init()
    private lazy var tapGestureRecognizer: UITapGestureRecognizer = .init()
    
    required init(screen: FooScreen, viewRegistry: ViewRegistry) {
        super.init(screen: screen, viewRegistry: viewRegistry)
        
        update(with: screen)
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        tapGestureRecognizer.addTarget(self, action: #selector(viewTapped))
        view.addGestureRecognizer(tapGestureRecognizer)
        
        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        titleLabel.textAlignment = .center
        view.addSubview(titleLabel)
        
        NSLayoutConstraint.activate([
            titleLabel.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            titleLabel.centerYAnchor.constraint(equalTo: view.centerYAnchor),
            ])
    }
    
    override func screenDidChange(from previousScreen: FooScreen) {
        update(with: screen)
    }
    
    private func update(with screen: FooScreen) {
        view.backgroundColor = screen.backgroundColor
        titleLabel.text = screen.title
    }
    
    @objc
    private func viewTapped() {
        screen.viewTapped()
    }
}

