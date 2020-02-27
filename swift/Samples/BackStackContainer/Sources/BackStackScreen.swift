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
import WorkflowUI


public struct BackStackScreen: Screen {
    public var items: [Item]

    public init(items: [BackStackScreen.Item]) {
        self.items = items
    }

    public func viewControllerDescription(environment: ViewEnvironment) -> ViewControllerDescription {
        return BackStackContainer.description(for: self, environment: environment)
    }
}

extension BackStackScreen {
    /// A specific item in the back stack. The key and screen type is used to differentiate reused vs replaced screens.
    public struct Item {
        public var key: AnyHashable
        public var screen: AnyScreen
        var screenType: Any.Type
        public var barVisibility: BarVisibility

        public init<ScreenType: Screen, Key: Hashable>(key: Key?, screen: ScreenType, barVisibility: BarVisibility) {
            self.screen = AnyScreen(screen)
            self.screenType = ScreenType.self

            if let key = key {
                self.key = AnyHashable(key)
            } else {
                self.key = AnyHashable(ObjectIdentifier(ScreenType.self))
            }
            self.barVisibility = barVisibility
        }

        public init<ScreenType: Screen>(screen: ScreenType, barVisibility: BarVisibility) {
            let key = Optional<AnyHashable>.none
            self.init(key: key, screen: screen, barVisibility: barVisibility)
        }

        public init<ScreenType: Screen, Key: Hashable>(key: Key?, screen: ScreenType, barContent: BackStackScreen.BarContent) {
            self.init(key: key, screen: screen, barVisibility: .visible(barContent))
        }

        public init<ScreenType: Screen>(screen: ScreenType, barContent: BackStackScreen.BarContent) {
            let key = Optional<AnyHashable>.none
            self.init(key: key, screen: screen, barContent: barContent)
        }

        public init<ScreenType: Screen, Key: Hashable>(key: Key?, screen: ScreenType) {
            let barVisibility: BarVisibility = .visible(BarContent())
            self.init(key: key, screen: screen, barVisibility: barVisibility)
        }

        public init<ScreenType: Screen>(screen: ScreenType) {
            let key = Optional<AnyHashable>.none
            self.init(key: key, screen: screen)
        }
    }
}


extension BackStackScreen {
    public enum BarVisibility {
        case hidden
        case visible(BarContent)
    }
}

extension BackStackScreen {
    public struct BarContent {
        var title: Title
        var leftItem: BarButtonItem
        var rightItem: BarButtonItem

        public enum BarButtonItem {
            case none
            case button(Button)
        }

        public init(title: Title = .none, leftItem: BarButtonItem = .none, rightItem: BarButtonItem = .none) {
            self.title = title
            self.leftItem = leftItem
            self.rightItem = rightItem
        }

        public init(title: String, leftItem: BarButtonItem = .none, rightItem: BarButtonItem = .none) {
            self.init(title: .text(title), leftItem: leftItem, rightItem: rightItem)
        }

    }
}


extension BackStackScreen.BarContent {
    public enum Title {
        case none
        case text(String)
    }

    public enum ButtonContent {
        case text(String)
        case icon(UIImage)
    }

    public struct Button {
        var content: ButtonContent
        var handler: () -> Void

        public init(content: ButtonContent, handler: @escaping () -> Void) {
            self.content = content
            self.handler = handler
        }

        /// Convenience factory for a default back button.
        public static func back(handler: @escaping () -> Void) -> Button {
            return Button(content: .text("Back"), handler: handler)
        }
    }
}



public enum SplitScreenPosition {
    /// View is not in a split screen container
    case none

    /// View is in the primary screen in split screen container
    case primary

    /// View is in the secondary screen in a split screen container
    case secondary

    /// View is in a collapsed split screen container
    case single
}

private enum SplitScreenPositionKey: ViewEnvironmentKey {
    static let defaultValue: SplitScreenPosition = .none
}

extension ViewEnvironment {

    var splitScreenPosition: SplitScreenPosition {
        get { self[SplitScreenPositionKey.self] }
        set { self[SplitScreenPositionKey.self] = newValue }
    }

}

public enum ReductionMode {
    case automatic
    case overlay
    case split
}

public enum Ratio {
    case automatic
    case specific(CGFloat)
}

public struct SplitScreenContainerScreen<Primary, Secondary, Combined> {


//    public enum BackStackSplitMode {
//        case preferSplit
//        case preferCollapse
//        case preferOverlay(showOverlayButtonContent: BackStackScreen.BarContent.ButtonContent)
//    }

    public let primary: Primary

    public let secondary: Secondary?

    public let combine: () -> Combined

    public let addShowOverlayButton: (_ action: @escaping () -> Void) -> Secondary?

    public let preferredReductionMode: ReductionMode

    public let onNeedSecondary: () -> Void

    public let ratio: Ratio

    public init(
        primary: Primary,
        secondary: Secondary?,
        combine: @escaping () -> Combined,
        addShowOverlayButton: @escaping (_ action: @escaping () -> Void) -> Secondary?,
        preferredReductionMode: ReductionMode,
        onNeedSecondary: @escaping () -> Void,
        ratio: Ratio
    ) {
        self.primary = primary
        self.secondary = secondary
        self.combine = combine
        self.addShowOverlayButton = addShowOverlayButton
        self.preferredReductionMode = preferredReductionMode
        self.onNeedSecondary = onNeedSecondary
        self.ratio = ratio
    }

}

extension SplitScreenContainerScreen where Primary == BackStackScreen, Secondary == BackStackScreen, Combined == BackStackScreen {

    public init(
        primary: Primary,
        secondary: Secondary?,
        backFromSecondaryBarButtonItem: BackStackScreen.BarContent.BarButtonItem,
        showOverlayButtonContent: BackStackScreen.BarContent.ButtonContent?,
        preferredReductionMode: ReductionMode,
        onNeedSecondary: @escaping () -> Void,
        ratio: Ratio
    ) {
        self.primary = primary
        self.secondary = secondary
        self.combine = {
            SplitScreenContainerScreen.combine(
                primary: primary,
                secondary: secondary,
                backFromSecondaryBarButtonItem: backFromSecondaryBarButtonItem
            )
        }
        self.addShowOverlayButton = { action in
            SplitScreenContainerScreen.addShowOverlayButton(
                secondary: secondary,
                showOverlayButtonContent: showOverlayButtonContent,
                showOverlayAction: action
            )
        }
        self.preferredReductionMode = preferredReductionMode
        self.onNeedSecondary = onNeedSecondary
        self.ratio = ratio
    }

    private static func combine(primary: Primary, secondary: Secondary?, backFromSecondaryBarButtonItem: BackStackScreen.BarContent.BarButtonItem) -> Combined {

        var mutableSecondaryItems = secondary?.items ?? []
        if mutableSecondaryItems.count > 0 {
            switch mutableSecondaryItems[0].barVisibility {
            case .hidden:
                break
            case .visible(var content):
                content.leftItem = backFromSecondaryBarButtonItem
                mutableSecondaryItems[0].barVisibility = .visible(content)
            }
        }
        return BackStackScreen(items: primary.items + mutableSecondaryItems)
    }

    private static func addShowOverlayButton(secondary: Secondary?, showOverlayButtonContent: BackStackScreen.BarContent.ButtonContent?, showOverlayAction: @escaping () -> Void) -> Secondary? {
        guard let showOverlayButtonContent = showOverlayButtonContent else {
            return secondary
        }
        guard var mutableItems = secondary?.items, mutableItems.count > 0 else {
            return nil
        }
        switch mutableItems[0].barVisibility {
        case .hidden:
            break
        case .visible(var content):
            content.leftItem = .button(.init(
                content: showOverlayButtonContent,
                handler: showOverlayAction
            ))
            mutableItems[0].barVisibility = .visible(content)
        }
        return BackStackScreen(items: mutableItems)
    }

}

extension SplitScreenContainerScreen: Screen where Primary: Screen, Secondary: Screen, Combined: Screen {

    public func viewControllerDescription(environment: ViewEnvironment) -> ViewControllerDescription {
        return SplitScreenContainerViewController.description(for: self, environment: environment)
    }

}


internal final class SplitScreenContainerViewController<Primary: Screen, Secondary: Screen, Combined: Screen>: ScreenViewController<SplitScreenContainerScreen<Primary, Secondary, Combined>>, UISplitViewControllerDelegate {

    let contentViewController: UISplitViewController
    let primaryViewController: DescribedViewController
    let secondaryViewController: DescribedViewController

    var requestedSecondary: Bool = false

    required init(screen: SplitScreenContainerScreen<Primary, Secondary, Combined>, environment: ViewEnvironment) {
        contentViewController = UISplitViewController()
        primaryViewController = DescribedViewController(
            screen: screen.primary,
            environment: environment
        )
        secondaryViewController = DescribedViewController(
            description: screen
                .secondary?
                .viewControllerDescription(environment: environment)
                ?? emptyViewControllerDescription
        )
        super.init(screen: screen, environment: environment)
    }

    override func screenDidChange(from previousScreen: SplitScreenContainerScreen<Primary, Secondary, Combined>, previousEnvironment: ViewEnvironment) {
        if screen.secondary != nil && previousScreen.secondary == nil {
            requestedSecondary = false
        }
        update()
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        contentViewController.viewControllers = [
            primaryViewController,
            secondaryViewController,
        ]
        addChild(contentViewController)
        view.addSubview(contentViewController.view)
        contentViewController.didMove(toParent: self)

        update()

        contentViewController.delegate = self
    }

    private func update() {
        guard isViewLoaded else { return }

        switch screen.preferredReductionMode {
        case .automatic:
            contentViewController.preferredDisplayMode = .automatic
        case .overlay:
            if contentViewController.displayMode != .primaryOverlay {
                contentViewController.preferredDisplayMode = .primaryHidden
            }
        case .split:
            contentViewController.preferredDisplayMode = .allVisible
        }

        switch screen.ratio {
        case .automatic:
            contentViewController.preferredPrimaryColumnWidthFraction = UISplitViewController.automaticDimension
            contentViewController.maximumPrimaryColumnWidth = UISplitViewController.automaticDimension
        case .specific(let ratio):
            contentViewController.preferredPrimaryColumnWidthFraction = ratio
            contentViewController.maximumPrimaryColumnWidth = 10000
        }

        update(
            isCollapsed: contentViewController.isCollapsed,
            displayMode: contentViewController.displayMode
        )
    }

    private func update(isCollapsed: Bool, displayMode: UISplitViewController.DisplayMode) {
        if isCollapsed {
            updateForCollapsed()
        } else {
            switch displayMode {
            case .automatic, .allVisible:
                updateForSplit()
            case .primaryHidden, .primaryOverlay:
                updateForOverlay()
            @unknown default:
                updateForSplit()
            }
        }
    }

    private func updateForSplit() {
        print(#function)
        requestSecondaryIfNecessary()
        primaryViewController.update(
            screen: screen.primary,
            environment: environment
                .setting(\.splitScreenPosition, to: .primary)
        )
        secondaryViewController.update(
            description: screen
                .secondary?
                .viewControllerDescription(
                    environment: environment
                        .setting(\.splitScreenPosition, to: .secondary)
                )
                ?? emptyViewControllerDescription
        )
    }

    private func updateForCollapsed() {
        print(#function)
        // let secondaryViewController stay as-is
        primaryViewController.update(
            screen: screen.combine(),
            environment: environment
                .setting(\.splitScreenPosition, to: .single))
    }

    private func updateForOverlay() {
        print(#function)
        requestSecondaryIfNecessary()
        primaryViewController.update(
            screen: screen.primary,
            environment: environment
                .setting(\.splitScreenPosition, to: .primary)
        )
        secondaryViewController.update(
            description: screen
                .addShowOverlayButton({ [weak self] in self?.showOverlay() })?
                .viewControllerDescription(
                    environment: environment
                        .setting(\.splitScreenPosition, to: .secondary)
                )
                ?? emptyViewControllerDescription
        )
    }

    private func requestSecondaryIfNecessary() {
        if screen.secondary == nil && !requestedSecondary {
            print(#function)
            requestedSecondary = true
            screen.onNeedSecondary()
        }
    }

    func showOverlay() {
//        UIView.animate(
//            withDuration: 0.5,
//            delay: 0,
//            options: UIView.AnimationOptions(rawValue: 7 << 16),
//            animations: { [contentViewController] in
//                contentViewController.preferredDisplayMode = .primaryOverlay
//            },
//            completion: { _ in
//                switch self.screen.preferredReductionMode {
//                case .automatic:
//                    self.contentViewController.preferredDisplayMode = .automatic
//                case .overlay:
//                    if self.contentViewController.displayMode != .primaryOverlay {
//                        self.contentViewController.preferredDisplayMode = .primaryHidden
//                    }
//                case .split:
//                    self.contentViewController.preferredDisplayMode = .allVisible
//                }
//            }
//        )
//        contentViewController.preferredDisplayMode = .primaryOverlay
        guard let target = contentViewController.displayModeButtonItem.target as? NSObject else {
            return
        }
        guard let action = contentViewController.displayModeButtonItem.action else {
            return
        }
        guard target.responds(to: action) else {
            return
        }
        target.perform(action)
    }

    func splitViewController(_ svc: UISplitViewController, willChangeTo displayMode: UISplitViewController.DisplayMode) {
        print("\(#function) \(displayMode)")
        printCurrentInfo()
        if displayMode == .primaryHidden || displayMode == .primaryOverlay {
            update(
                isCollapsed: contentViewController.isCollapsed,
                displayMode: displayMode
            )
        }
    }

//    func targetDisplayModeForAction(in svc: UISplitViewController) -> UISplitViewController.DisplayMode {
//        print("\(#function)")
//        return .automatic
//    }

//    func splitViewController(_ splitViewController: UISplitViewController, show vc: UIViewController, sender: Any?) -> Bool {
////        print("\(#function) \(vc == primary ? "primary" : vc == secondary ? "secondary" : "neither"), sender: \(sender as Any?)")
//        return false
//    }

//    func splitViewController(_ splitViewController: UISplitViewController, showDetail vc: UIViewController, sender: Any?) -> Bool {
////        print("\(#function) \(vc == primary ? "primary" : vc == secondary ? "secondary" : "neither"), sender: \(sender as Any?)")
//        return false
//    }
//
//    func primaryViewController(forCollapsing splitViewController: UISplitViewController) -> UIViewController? {
//        updateForCollapsed()
//        return secondaryViewController
////        return secondaryViewController
//    }
//
//    func primaryViewController(forExpanding splitViewController: UISplitViewController) -> UIViewController? {
//        updateForSplit()
//        return primaryViewController
////        print("\(#function)")
////        updateForSplit()
////        return Secondary
//    }

    func splitViewController(_ splitViewController: UISplitViewController, collapseSecondary secondaryViewController: UIViewController, onto primaryViewController: UIViewController) -> Bool {
//        print("\(#function)  \(secondaryViewController == primary ? "primary" : secondaryViewController == secondary ? "secondary" : "neither") onto \(primaryViewController == primary ? "primary" : primaryViewController == secondary ? "secondary" : "neither")")
//        return false
        print("\(#function)")
        printCurrentInfo()
        updateForCollapsed()
        return true
    }

    func splitViewController(_ splitViewController: UISplitViewController, separateSecondaryFrom primaryViewController: UIViewController) -> UIViewController? {
        print("\(#function)")
        printCurrentInfo()

        update(
            isCollapsed: false,
            displayMode: contentViewController.displayMode
        )
        return secondaryViewController
//        print("\(#function) \(primaryViewController == primary ? "primary" : primaryViewController == secondary ? "secondary" : "neither")")
//        return nil
    }

    func printCurrentInfo(spaces: Int = 2) {
        let prefix = String(Array(repeating: " ", count: spaces))
        print("\(prefix)displayMode: \(contentViewController.displayMode)")
        print("\(prefix)isCollapsed: \(contentViewController.isCollapsed)")
    }

}


/// A `SplitScreenContainerScreen` displays two screens side by side with a separator in between.
//public struct SplitScreenContainerScreen: Screen {
//
//    public enum StackingBehavior {
//        case never
//        case canStack(trailingBackItem: BackStackScreen.BarContent.BarButtonItem)
//        case leadingSlideOver(showLeadingButtonContent: BackStackScreen.BarContent.ButtonContent)
//    }
//
//    /// The screen displayed leading the separator.
//    public let leadingScreen: BackStackScreen
//
//    /// The screen displayed trailing the separator.
//    public let trailingScreen: BackStackScreen?
//
//    public let stackingBehavior: StackingBehavior
//
//    public let selectDefaultFromLeading: () -> Void
//
//    /// The ratio of `leadingScreen`'s width relative to that of `trailingScreen`. Defaults to `.third`.
//    public let ratio: CGFloat
//
//    /// The color of the `separatorView` displayed between `leadingScreen`'s and `trailingScreen`'s views.
//    public let separatorColor: UIColor
//
//    /// The width of the `separatorView` displayed between `leadingScreen`'s and `trailingScreen`'s views.
//    public let separatorWidth: CGFloat
//
//    public init(
//        leadingScreen: BackStackScreen,
//        trailingScreen: BackStackScreen?,
//        stackingBehavior: StackingBehavior,
//        selectDefaultFromLeading: @escaping () -> Void = { },
//        ratio: CGFloat = .third,
//        separatorColor: UIColor = .black,
//        separatorWidth: CGFloat = 1.0
//    ) {
//        self.leadingScreen = leadingScreen
//        self.trailingScreen = trailingScreen
//        self.stackingBehavior = stackingBehavior
//        self.selectDefaultFromLeading = selectDefaultFromLeading
//        self.ratio = ratio
//        self.separatorColor = separatorColor
//        self.separatorWidth = separatorWidth
//    }
//
//    public func viewControllerDescription(environment: ViewEnvironment) -> ViewControllerDescription {
//        fatalError()
//    }
//
//}
//
//public extension CGFloat {
//    static let quarter: CGFloat = 1.0 / 4.0
//    static let third: CGFloat = 1.0 / 3.0
//    static let half: CGFloat = 1.0 / 2.0
//}

//private struct EmptyScreen: Screen {
//    func viewControllerDescription(environment: ViewEnvironment) -> ViewControllerDescription {
//        return ViewControllerDescription(
//            build: { UIViewController() },
//            update: { _ in }
//        )
//    }
//}

private let emptyViewControllerDescription = ViewControllerDescription(
    build: { UIViewController() },
    update: { _ in }
)

//private struct SplitBackStackScreen: ProxyScreen {
//
//    var leadingItems: [BackStackScreen.Item]
//    var trailingItems: [BackStackScreen.Item]
//    var backFromTrailingBarItem: BackStackScreen.BarContent.BarButtonItem
//
//    var screenRepresentation: BackStackScreen {
//        var mutableTrailingItems = trailingItems
//        if mutableTrailingItems.count > 0 {
//            switch mutableTrailingItems[0].barVisibility {
//            case .hidden:
//                break
//            case .visible(var content):
//                content.leftItem = backFromTrailingBarItem
//                mutableTrailingItems[0].barVisibility = .visible(content)
//            }
//        }
//        return BackStackScreen(items: leadingItems + mutableTrailingItems)
//    }
//
//}

//private struct SlideOverBackStackScreen: ProxyScreen {
//
//    var items: [BackStackScreen.Item]
//    var showLeadingBarItem: BackStackScreen.BarContent.BarButtonItem
//
//    var screenRepresentation: BackStackScreen {
//        var mutableItems = items
//        if mutableItems.count > 0 {
//            switch mutableItems[0].barVisibility {
//            case .hidden:
//                break
//            case .visible(var content):
//                content.leftItem = showLeadingBarItem
//                mutableItems[0].barVisibility = .visible(content)
//            }
//        }
//        return BackStackScreen(items: mutableItems)
//    }
//
//}

//extension Screen {
//
//    public func environment<Value>(_ keyPath: WritableKeyPath<ViewEnvironment, Value>, value: Value) -> EnvironmentScreen<Self> {
//        return EnvironmentScreen(keyPath: keyPath, value: value, content: self)
//    }
//
//}
//
//public struct EnvironmentScreen<Content> {
//
//    fileprivate var modifyEnvironment: (ViewEnvironment) -> ViewEnvironment
//    fileprivate let content: Content
//
//    fileprivate init<Value>(keyPath: WritableKeyPath<ViewEnvironment, Value>, value: Value, content: Content) {
//        self.modifyEnvironment = { $0.setting(keyPath, to: value) }
//        self.content = content
//    }
//
//    public mutating func setting<Value>(_ keyPath: WritableKeyPath<ViewEnvironment, Value>, to value: Value) {
//        let previousModify = modifyEnvironment
//        modifyEnvironment = { environment in
//            previousModify(environment)
//                .setting(keyPath, to: value)
//        }
//    }
//
//}
//
//extension EnvironmentScreen: Screen where Content: Screen {
//
//    public func viewControllerDescription(environment: ViewEnvironment) -> ViewControllerDescription {
//        return content.viewControllerDescription(environment: modifyEnvironment(environment))
//    }
//
//}

//internal final class SplitScreenContainerViewController: ScreenViewController<SplitScreenContainerScreen> {
//
//    fileprivate enum Layout {
//        case split
//        case stacked(trailingBackItem: BackStackScreen.BarContent.BarButtonItem)
//        case slideOver(showLeadingButtonContent: BackStackScreen.BarContent.ButtonContent)
//    }
//
//    private var leadingContentViewController: DescribedViewController
//
//    private lazy var separatorView: UIView = .init()
//
//    private var trailingContentViewController: DescribedViewController
//
//    private var needsAnimatedLayout = false
//
//    required init(screen: SplitScreenContainerScreen, environment: ViewEnvironment) {
//        leadingContentViewController = DescribedViewController(
//            screen: screen.leadingScreen,
//            environment: environment
//                .setting(\.splitScreenPosition, to: .leading)
//        )
//        if let trailingScreen = screen.trailingScreen {
//            trailingContentViewController = DescribedViewController(
//                screen: trailingScreen,
//                environment: environment
//                    .setting(\.splitScreenPosition, to: .trailing)
//            )
//        } else {
//            trailingContentViewController = DescribedViewController(
//                description: emptyViewControllerDescription
//            )
//        }
//        super.init(screen: screen, environment: environment)
//    }
//
//    override internal func screenDidChange(from previousScreen: SplitScreenContainerScreen, previousEnvironment: ViewEnvironment) {
//        if screen.ratio != previousScreen.ratio {
//            needsAnimatedLayout = true
//        }
//        if screen.separatorWidth != previousScreen.separatorWidth {
//            needsAnimatedLayout = true
//        }
//        update(with: screen)
//    }
//
//    private var currentLayout: Layout {
//        switch screen.stackingBehavior {
//        case .never:
//            return .split
//        case .canStack(let trailingBackItem):
//            // TODO: Only stack when small enough
//            return .stacked(trailingBackItem: trailingBackItem)
//        case .leadingSlideOver(let showLeadingButtonContent):
//            // TODO: Only slide when small enough
//            return .slideOver(showLeadingButtonContent: showLeadingButtonContent)
//        }
//    }
//
////    private var isStacked: BackStackScreen.BarContent.BarButtonItem? {
////        switch screen.stackingBehavior {
////        case .never:
////            return nil
////        case .canStack(let trailingBackItem):
////            return trailingBackItem
////        }
////    }
//
//    private func update(with screen: SplitScreenContainerScreen) {
//        guard isViewLoaded else { return }
//        separatorView.backgroundColor = screen.separatorColor
//
//        switch currentLayout {
//        case .split:
//            leadingContentViewController
//                .update(
//                    screen: screen.leadingScreen,
//                    environment: environment
//                        .setting(\.splitScreenPosition, to: .leading)
//                )
//            trailingContentViewController
//                .update(
//                    description: screen
//                        .trailingScreen?
//                        .viewControllerDescription(
//                            environment: environment
//                                .setting(\.splitScreenPosition, to: .trailing)
//                        )
//                        ?? emptyViewControllerDescription
//                )
//            if screen.trailingScreen == nil {
//                screen.selectDefaultFromLeading()
//            }
//
//        case .stacked(let trailingBackItem):
//            let newTrailingScreen = SplitBackStackScreen(
//                leadingItems: screen.leadingScreen.items,
//                trailingItems: screen.trailingScreen?.items ?? [],
//                backFromTrailingBarItem: trailingBackItem
//            )
//
//            // let the leadingContentViewController stay as-is, it will be off-screen
//
//            trailingContentViewController
//                .update(
//                    screen: newTrailingScreen,
//                    environment: environment
//                        .setting(\.splitScreenPosition, to: .single)
//                )
//
//        case .slideOver(let showLeadingButtonContent):
//            if screen.trailingScreen == nil {
//                screen.selectDefaultFromLeading()
//            }
//            let newTrailingScreen = SlideOverBackStackScreen(
//                items: screen.trailingScreen?.items ?? [],
//                showLeadingBarItem: .button(.init(
//                    content: showLeadingButtonContent,
//                    handler: { self.showSlideOver() }
//                ))
//            )
//            leadingContentViewController
//                .update(
//                    screen: screen.leadingScreen,
//                    environment: environment
//                        .setting(\.splitScreenPosition, to: .leading)
//                )
//            trailingContentViewController
//                .update(
//                    screen: newTrailingScreen,
//                    environment: environment
//                        .setting(\.splitScreenPosition, to: .trailing)
//                )
//
////            if var trailingScreen = screen.trailingScreen {
////                switch trailingScreen.items[0].barVisibility {
////                case .hidden:
////                    break
////                case .visible(var content):
////                    content.leftItem = .button(.init(
////                        content: showLeadingButtonContent,
////                        handler: { self.showSlideOver() }
////                    ))
////                    trailingScreen.items[0].barVisibility = .visible(content)
////                }
////                trailingContentViewController.update(
////                    screen: trailingScreen,
////                    environment: environment
////                        .setting(\.splitScreenPosition, to: .single)
////                )
////            } else {
////                trailingContentViewController.update(
////                    description: emptyViewControllerDescription
////                )
////            }
//
//        }
//
////        //Intentional force of layout pass after updating the child view controllers
////        view.layoutIfNeeded()
//
//        view.setNeedsLayout()
//
//        if needsAnimatedLayout {
//            needsAnimatedLayout = false
//
//            UIView.animate(withDuration: 0.25) {
//                self.view.layoutIfNeeded()
//            }
//        }
//
//    }
//
//    private func showSlideOver() {
//
//    }
//
//    override internal func viewDidLoad() {
//        super.viewDidLoad()
//
//        addChild(leadingContentViewController)
//        view.addSubview(leadingContentViewController.view)
//        leadingContentViewController.didMove(toParent: self)
//
//        addChild(trailingContentViewController)
//        view.addSubview(trailingContentViewController.view)
//        trailingContentViewController.didMove(toParent: self)
//
//        view.addSubview(separatorView)
//
//        update(with: screen)
//    }
//
//    override func viewWillTransition(to size: CGSize, with coordinator: UIViewControllerTransitionCoordinator) {
//        coordinator.animate(
//            alongsideTransition: { context in
//                self.needsAnimatedLayout = false
//                self.update(with: self.screen)
//                self.view.layoutIfNeeded()
//            },
//            completion: nil)
//        super.viewWillTransition(to: size, with: coordinator)
//    }
//
//    internal override func viewDidLayoutSubviews() {
//        super.viewDidLayoutSubviews()
//
//        let distance = view.bounds.width * screen.ratio
//
//        var (firstSlice, trailingRect) = view.bounds.divided(atDistance: distance, from: .minXEdge)
//
//        var (leadingRect, separatorRect) = firstSlice.divided(atDistance: distance - screen.separatorWidth, from: .minXEdge)
//
//        switch currentLayout {
//        case .split:
//            break
//        case .stacked, .slideOver:
//            trailingRect = view.bounds
//            separatorRect.origin.x = view.bounds.minX - separatorRect.width
//            leadingRect.origin.x = separatorRect.minX - leadingRect.width
//        }
//
//        leadingContentViewController.view.frame = isLayoutDirectionRightToLeft ? trailingRect: leadingRect
//        trailingContentViewController.view.frame = isLayoutDirectionRightToLeft ? leadingRect : trailingRect
//        separatorView.frame = separatorRect
//    }
//
//}
//
//fileprivate extension UIViewController {
//    var isLayoutDirectionRightToLeft: Bool {
//        if #available(iOS 10.0, *) {
//            return traitCollection.layoutDirection == .rightToLeft
//        } else {
//            return UIView.userInterfaceLayoutDirection(for: view.semanticContentAttribute) == .rightToLeft
//        }
//    }
//}
extension UISplitViewController.DisplayMode: CustomDebugStringConvertible {

    public var debugDescription: String {
        switch self {
        case .automatic:
            return ".automatic"
        case .primaryHidden:
            return ".primaryHidden"
        case .allVisible:
            return ".allVisible"
        case .primaryOverlay:
            return ".primaryOverlay"
        @unknown default:
            return "!!UNKNOWN!!"
        }
    }

}
