import Workflow
import WorkflowUI
import XCTest
import FBSnapshotTestCase
@testable import WorkflowSplitScreenContainer


class SplitScreenContainerScreenSnapshotTests: FBSnapshotTestCase {
    override func setUp() {
        super.setUp()
        recordMode = false
    }
    
    func test_splitRatio() {
        var viewRegistry = ViewRegistry()
        viewRegistry.register(screenViewControllerType: BaseScreenViewController.self)

        let ratios: [String : SplitScreenContainerScreen.Ratio] = [
            "third" : .third,
            "quarter" : .quarter,
            "half" : .half,
            "custom" : .init(0.3125)
        ]

        for (name, ratio) in ratios {
            let splitScreenContainerScreen = SplitScreenContainerScreen(
                leftScreen: BaseScreen(title: "Left screen", backgroundColor: .green),
                rightScreen: BaseScreen(title: "Right screen", backgroundColor: .red),
                ratio: ratio
            )

            let viewController = SplitScreenContainerViewController(
                screen: splitScreenContainerScreen,
                viewRegistry: viewRegistry
            )
            viewController.view.layoutIfNeeded()

            FBSnapshotVerifyView(viewController.view, identifier: name)
        }
    }
}


struct BaseScreen: Screen {
    var title: String
    var backgroundColor: UIColor
}


fileprivate final class BaseScreenViewController: ScreenViewController<BaseScreen> {
    
    private let titleLabel: UILabel
    
    required init(screen: BaseScreen, viewRegistry: ViewRegistry) {
        titleLabel = UILabel(frame: CGRect(x: 0, y: 0, width: 100, height: 20))
        titleLabel.textAlignment = .center
        
        super.init(screen: screen, viewRegistry: viewRegistry)
        
        update(with: screen)
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        view.addSubview(titleLabel)
    }
    
    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        
        titleLabel.center = view.center
    }
    
    override func screenDidChange(from previousScreen: BaseScreen) {
        update(with: screen)
    }
    
    private func update(with screen: BaseScreen) {
        view.backgroundColor = screen.backgroundColor
        titleLabel.text = screen.title
    }
}

