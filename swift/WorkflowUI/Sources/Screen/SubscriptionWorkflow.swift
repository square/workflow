import Foundation
import ReactiveSwift

public struct SubscribedScreen<S: Screen, O>: Screen {
    let screen: S
    let subscription: Subscription<O>?

    public func viewControllerDescription(environment: ViewEnvironment) -> ViewControllerDescription {
        return ViewController.description(for: self, environment: environment)
    }

    private class ViewController: ScreenViewController<SubscribedScreen<S, O>> {
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
            from previousScreen: SubscribedScreen<S, O>,
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

            disposable = subscription.producer.start { ev in
                guard let value = ev.value else {
                    return
                }

                subscription.action(value)
            }
        }
    }
}

public extension Screen {
    func subscribed<O>(to subscription: Subscription<O>?) -> SubscribedScreen<Self, O> {
        return SubscribedScreen(screen: self, subscription: subscription)
    }
}

public struct Subscription<O> {
    let producer: SignalProducer<O, Never>
    let action: ((O) -> Void)
    
    public init(producer: SignalProducer<O, Never>, action: @escaping ((O) -> Void)) {
        self.producer = producer
        self.action = action
    }
}
