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
import Workflow
import WorkflowUI
import ReactiveSwift


// MARK: Input and Output

struct DemoWorkflow: Workflow {
    var name: String

    typealias Output = Never
}


// MARK: State and Initialization

extension DemoWorkflow {

    struct State {
        var colorState: ColorState
        var loadingState: LoadingState
        var subscriptionState: SubscriptionState

        enum ColorState {
            case red
            case green
            case blue
        }

        enum LoadingState {
            case idle(title: String)
            case loading
        }

        enum SubscriptionState {
            case not
            case subscribing
        }
    }

    func makeInitialState() -> DemoWorkflow.State {
        return State(
            colorState: .red,
            loadingState: .idle(title: "Not Loaded"),
            subscriptionState: .not)
    }

    func workflowDidChange(from previousWorkflow: DemoWorkflow, state: inout State) {

    }
}


// MARK: Actions

extension DemoWorkflow {

    enum Action: WorkflowAction {

        typealias WorkflowType = DemoWorkflow

        case titleButtonTapped
        case subscribeTapped
        case refreshButtonTapped
        case refreshComplete(String)
        case refreshError(Error)

        func apply(toState state: inout DemoWorkflow.State) -> DemoWorkflow.Output? {

            switch self {
            case .titleButtonTapped:
                switch state.colorState {
                case .red:
                    state.colorState = .green
                case .green:
                    state.colorState = .blue
                case .blue:
                    state.colorState = .red
                }

            case .subscribeTapped:
                switch state.subscriptionState {
                case .not:
                    state.subscriptionState = .subscribing
                case .subscribing:
                    state.subscriptionState = .not
                }

            case .refreshButtonTapped:
                state.loadingState = .loading
            case .refreshComplete(let message):
                state.loadingState = .idle(title: message)
            case .refreshError(let error):
                state.loadingState = .idle(title: error.localizedDescription)
            }
            return nil
        }
    }
}

extension SignalProducer {
    static var refresher: SignalProducer<String, Never> {
        return SignalProducer<String, Never>(value: "We did it!")
            .delay(1.0, on: QueueScheduler.main)
    }
}

// MARK: Rendering

extension DemoWorkflow {
    typealias Rendering = AnyScreen

    func render(state: DemoWorkflow.State, context: RenderContext<DemoWorkflow>) -> Rendering {
        let color: UIColor
        switch state.colorState {
        case .red:
            color = .red
        case .green:
            color = .green
        case .blue:
            color = .blue
        }

        var title = "Hello, \(name)!"
        let refreshText: String
        let refreshEnabled: Bool
        var refreshSubscription: Subscription<String>?
        // Create a sink of our Action type so we can send actions back to the workflow.
        let sink = context.makeSink(of: Action.self)

        switch state.loadingState {
        case .idle(title: let refreshTitle):
            refreshText = refreshTitle
            refreshEnabled = true

            title = ReversingWorkflow(text: title)
                .rendered(with: context)

        case .loading:
            refreshText = "Loading..."
            refreshEnabled = false

            refreshSubscription = Subscription(producer: SignalProducer<String, Never>.refresher) { val in
                sink.send(.refreshComplete(val))
            }
        }

        let subscribeTitle: String
        var subscription: Subscription<Void>?
        
        switch state.subscriptionState {
        case .not:
            subscribeTitle = "Subscribe"
        case .subscribing:
            let producer = SignalProducer
                .timer(interval: .seconds(1), on: QueueScheduler.main)
                .map { _ in () }
            
            subscription = Subscription(producer: producer) {
                sink.send(.titleButtonTapped)
            }

            subscribeTitle = "Stop"
        }


        let demoScreen = DemoScreen(
            title: title,
            color: color,
            onTitleTap: {
                sink.send(.titleButtonTapped)
            },
            subscribeTitle: subscribeTitle,
            onSubscribeTapped: {
                sink.send(.subscribeTapped)
            },
            refreshText: refreshText,
            isRefreshEnabled: refreshEnabled,
            onRefreshTap: {
                sink.send(.refreshButtonTapped)
            }
        )

        return demoScreen
            .subscribed(to: subscription)
            .subscribed(to: refreshSubscription)
            .asAny()
    }
}
