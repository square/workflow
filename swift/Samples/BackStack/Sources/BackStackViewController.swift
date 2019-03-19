import WorkflowUI

final class BackStackViewController: ScreenViewController<BackStackScreen> {

    private let navController = CustomNavController()

    override func viewDidLoad() {
        super.viewDidLoad()
        addChild(navController)
        view.addSubview(navController.view)
        navController.didMove(toParent: self)

        update()
    }

    override func screenDidChange(from previousScreen: BackStackScreen) {
        super.screenDidChange(from: previousScreen)
        update()
    }

    private func update() {

        var existingViewControllers = navController.viewControllers.map { $0 as! ItemViewController }

        var newViewControllers: [ItemViewController] = []

        for item in screen.items {
            if let existingIndex = existingViewControllers.firstIndex(where: { $0.key == item.key }) {
                let viewController = existingViewControllers.remove(at: existingIndex)
                viewController.update(item: item)
                newViewControllers.append(viewController)
            } else {
                newViewControllers.append(ItemViewController(item: item, viewRegistry: viewRegistry))
            }
        }

        if newViewControllers != navController.viewControllers {
            navController.setViewControllers(newViewControllers, animated: true)
        }

    }

}

fileprivate final class CustomNavController: UINavigationController {

}


fileprivate final class ItemViewController: UIViewController {

    var key: String {
        return item.key
    }

    private let viewRegistry: ViewRegistry

    private var item: BackStackItem

    private var viewController: AnyScreenViewController

    init(item: BackStackItem, viewRegistry: ViewRegistry) {
        self.item = item
        self.viewRegistry = viewRegistry
        self.viewController = item.screen.inflate(from: viewRegistry)
        super.init(nibName: nil, bundle: nil)

        addChild(viewController)
        viewController.didMove(toParent: self)

        updateNavigationItem()
    }

    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        view.addSubview(viewController.view)
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        viewController.view.frame = view.bounds
    }

    func update(item: BackStackItem) {
        self.item = item
        updateNavigationItem()
        updateViewController()
    }

    private func updateNavigationItem() {
        self.navigationItem.title = item.title
        self.navigationItem.hidesBackButton = true
        switch item.backAction {
        case .none:
            navigationItem.leftBarButtonItem = nil
        case .back(_):
            navigationItem.leftBarButtonItem = UIBarButtonItem(title: "👈🏻", style: .plain, target: self, action: #selector(goBack))
        }
    }

    @objc private func goBack() {
        switch item.backAction {
        case .none:
            break
        case .back(let handler):
            handler()
        }
    }

    private func updateViewController() {
        if viewController.screenType == type(of: item.screen) {
            viewController.update(screen: item.screen)
        } else {
            viewController.willMove(toParent: nil)
            if isViewLoaded {
                viewController.view.removeFromSuperview()
            }
            viewController.removeFromParent()

            viewController = item.screen.inflate(from: viewRegistry)
            addChild(viewController)
            if isViewLoaded {
                view.addSubview(viewController.view)
            }
            viewController.didMove(toParent: self)

        }
    }

}
