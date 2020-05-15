/*
 * Copyright 2020 Square Inc.
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

import ReactiveSwift
import Workflow
import WorkflowUI

// MARK: Input and Output

struct DemoWorkflow: Workflow {
    var name: String

    typealias Output = Never
}

// MARK: State and Initialization

extension DemoWorkflow {
    struct State {
        fileprivate var signal: TimerSignal
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
            signal: TimerSignal(),
            colorState: .red,
            loadingState: .idle(title: "Not Loaded"),
            subscriptionState: .not
        )
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
            case let .refreshComplete(message):
                state.loadingState = .idle(title: message)
            case let .refreshError(error):
                state.loadingState = .idle(title: error.localizedDescription)
            }
            return nil
        }
    }
}

// MARK: Workers

struct RefreshWorker: Worker {
    enum Output {
        case success(String)
        case error(Error)
    }

    func run() -> SignalProducer<RefreshWorker.Output, Never> {
        return SignalProducer(value: .success("We did it!"))
            .delay(1.0, on: QueueScheduler.main)
    }

    func isEquivalent(to otherWorker: RefreshWorker) -> Bool {
        return true
    }
}

// MARK: Rendering

extension DemoWorkflow {
    typealias Rendering = DemoScreen

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

        switch state.loadingState {
        case let .idle(title: refreshTitle):
            refreshText = refreshTitle
            refreshEnabled = true

            title = ReversingWorkflow(text: title)
                .rendered(with: context)

        case .loading:
            refreshText = "Loading..."
            refreshEnabled = false

            context.awaitResult(for: RefreshWorker()) { output -> Action in
                switch output {
                case let .success(result):
                    return .refreshComplete(result)
                case let .error(error):
                    return .refreshError(error)
                }
            }
        }

        let subscribeTitle: String

        switch state.subscriptionState {
        case .not:
            subscribeTitle = "Subscribe"
        case .subscribing:
            // Subscribe to the timer signal, simulating the title being tapped.
            context.awaitResult(for: state.signal.signal.asWorker(key: "Timer")) { _ -> Action in
                .titleButtonTapped
            }
            subscribeTitle = "Stop"
        }

        // Create a sink of our Action type so we can send actions back to the workflow.
        let sink = context.makeSink(of: Action.self)

        return DemoScreen(
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
    }
}

private class TimerSignal {
    let signal: Signal<Void, Never>
    let observer: Signal<Void, Never>.Observer
    let timer: Timer

    init() {
        let (signal, observer) = Signal<Void, Never>.pipe()

        let timer = Timer.scheduledTimer(withTimeInterval: 1.0, repeats: true) { [weak observer] _ in
            observer?.send(value: ())
        }

        self.signal = signal
        self.observer = observer
        self.timer = timer
    }
}
