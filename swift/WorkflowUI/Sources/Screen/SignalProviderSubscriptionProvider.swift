import Foundation
import ReactiveSwift

public protocol ResubscribeStrategy: Equatable {}

public struct SubscribeOnlyOnFirstRenderStrategy: ResubscribeStrategy {
    public init() {}
}

public struct AlwaysResubscribeOnRenderStrategy: ResubscribeStrategy {
    public init() {}
    
    public static func == (lhs: Self, rhs: Self) -> Bool {
        return false
    }
}

public struct ResubscribeIfNotEqualStrategy<Key: Equatable>: ResubscribeStrategy {
    let key: Key

    public init(key: Key) {
        self.key = key
    }
}

public struct SignalProviderSubscriptionProvider<Output, Strategy: ResubscribeStrategy>: SubscriptionProvider {
    public let producer: SignalProducer<Output, Never>
    let strategy: Strategy

    public init(producer: SignalProducer<Output, Never>, resubscribeStrategy: Strategy) {
        self.producer = producer
        self.strategy = resubscribeStrategy
    }

    public static func == (lhs: Self, rhs: Self) -> Bool {
        lhs.strategy == rhs.strategy
    }
}

public extension SignalProducer where Error == Never {
    func subscriptionProvider<Strategy: ResubscribeStrategy>(using strategy: Strategy)
        -> SignalProviderSubscriptionProvider<Self.Value, Strategy>
    {
        SignalProviderSubscriptionProvider(producer: self, resubscribeStrategy: strategy)
    }
}
