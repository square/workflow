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
import TabBarContainer

// MARK: Input and Output

struct BazBadgeWorkflow: Workflow {

    typealias Output = Never
}


// MARK: State and Initialization

extension BazBadgeWorkflow {

    struct State {
        var badgeValue: String?
    }

    func makeInitialState() -> BazBadgeWorkflow.State {
        return State(badgeValue: nil)
    }

    func workflowDidChange(from previousWorkflow: BazBadgeWorkflow, state: inout State) {

    }
}


// MARK: Actions

extension BazBadgeWorkflow {

    enum Action: WorkflowAction {

        typealias WorkflowType = BazBadgeWorkflow
        
        case badgeValueUpdated(String?)

        func apply(toState state: inout BazBadgeWorkflow.State) -> BazBadgeWorkflow.Output? {

            switch self {
            case .badgeValueUpdated(let badgeValue):
                state.badgeValue = badgeValue
                return nil
            }

        }
    }
}


// MARK: Workers

extension BazBadgeWorkflow {

    struct BarBadgeWorker: Worker {

        enum Output {
            case badgeValue(String?)
        }

        func run() -> SignalProducer<Output, Never> {
            return SignalProducer<Output, Never>(value: .badgeValue("1"))
        }

        func isEquivalent(to otherWorker: BarBadgeWorker) -> Bool {
            return true
        }

    }

}

// MARK: Rendering

extension BazBadgeWorkflow {
    typealias Rendering = String?

    func render(state: BazBadgeWorkflow.State, context: RenderContext<BazBadgeWorkflow>) -> Rendering {
        context.awaitResult(for: BarBadgeWorker()) { (output) -> Action in
            switch output {
            case .badgeValue(let badgeValue):
                return .badgeValueUpdated(badgeValue)
            }
        }
        
        return state.badgeValue
    }
}
