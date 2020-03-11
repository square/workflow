import Foundation
import ReactiveSwift
import Workflow

public struct SubscribedScreen<S: Screen, SubPro: SubscriptionProvider>: Screen {
    let screen: S
    let subscription: Subscription<SubPro>?

    public func viewControllerDescription(environment: ViewEnvironment) -> ViewControllerDescription {
        return ViewController.description(for: self, environment: environment)
    }

    private class ViewController: ScreenViewController<SubscribedScreen<S, SubPro>> {
        private var disposable: Disposable?
        private let screenViewController: DescribedViewController

        required init(screen: SubscribedScreen, environment: ViewEnvironment) {
            screenViewController = DescribedViewController(screen: screen.screen, environment: environment)

            super.init(screen: screen, environment: environment)
        }

        override func viewDidLoad() {
            super.viewDidLoad()

            self.view.addSubview(screenViewController.view)
        }

        override func viewDidLayoutSubviews() {
            super.viewDidLayoutSubviews()

            screenViewController.view.frame = self.view.bounds
        }

        override func screenDidChange(
            from previousScreen: SubscribedScreen<S, SubPro>,
            previousEnvironment: ViewEnvironment
        ) {
            updateSubscription()
            screenViewController.update(screen: screen.screen, environment: environment)
        }

        private func updateSubscription() {
            guard let subscription = screen.subscription else {
                disposable = nil
                return
            }

            guard disposable == nil else {
                return
            }

            disposable = subscription.provider.producer.start { ev in
                guard let value = ev.value else {
                    return
                }

                subscription.action(value)
            }
        }

        private func shouldUpdateSubscription(from previousScreen: SubscribedScreen<S, SubPro>) -> Bool {
            screen.subscription?.provider == previousScreen.subscription?.provider
        }
    }
}

public extension Screen {
    func subscribed<Provider: SubscriptionProvider>(to subscription: Subscription<Provider>?) -> SubscribedScreen<Self, Provider> {
        return SubscribedScreen(screen: self, subscription: subscription)
    }
}

public protocol SubscriptionProvider: Equatable {
    associatedtype Output

    var producer: SignalProducer<Output, Never> { get }
}

public struct MappedSubscriptionProvider<Sub: SubscriptionProvider, Output>: SubscriptionProvider {
    let wrapped: Sub
    let mapper: ((Sub.Output) -> Output)

    public var producer: SignalProducer<Output, Never> {
        wrapped
            .producer
            .map { self.mapper($0) }
    }

    public static func == (
        lhs: MappedSubscriptionProvider<Sub, Output>,
        rhs: MappedSubscriptionProvider<Sub, Output>
    ) -> Bool {
        lhs.wrapped == rhs.wrapped
    }
}

public extension SubscriptionProvider {
    func map<NewOutput>(_ mapper: @escaping (Output) -> NewOutput) -> MappedSubscriptionProvider<Self, NewOutput> {
        MappedSubscriptionProvider(wrapped: self, mapper: mapper)
    }
}

public struct Subscription<Provider: SubscriptionProvider> {
    let provider: Provider
    let action: ((Provider.Output) -> Void)
}

public extension SubscriptionProvider {
    func subscribe<WorkflowType>(context: RenderContext<WorkflowType>) -> Subscription<Self>
        where
        Output: WorkflowAction,
        Output.WorkflowType == WorkflowType
    {
        let sink = context.makeSink(of: Output.self)
        return Subscription(provider: self) { o in
            sink.send(o)
        }
    }
}
