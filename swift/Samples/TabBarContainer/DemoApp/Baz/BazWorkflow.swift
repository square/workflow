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
import ReactiveSwift
import TabBarContainer
import Workflow
import WorkflowUI

// MARK: Input and Output

struct BazWorkflow: Workflow {

}

// MARK: State and Initialization

extension BazWorkflow {

    typealias State = Void
    enum Output {
        case tabSelected
    }

}

// MARK: Actions

extension BazWorkflow {

    enum Action: WorkflowAction {
        case tabSelected

        typealias WorkflowType = BazWorkflow

        func apply(toState state: inout BazWorkflow.State) -> BazWorkflow.Output? {
            return .tabSelected
        }
    }
}

// MARK: Rendering

extension BazWorkflow {
    typealias Rendering = TabScreen

    func render(state: BazWorkflow.State, context: RenderContext<BazWorkflow>) -> Rendering {
        let sink = context.makeSink(of: Action.self)

        let bazBadgeValue = BazBadgeWorkflow()
            .mapRendering({ (rendering) -> BarItem.Badge in
                guard let badgeValue = rendering else {
                    return .none
                }

                return .text(badgeValue)
            })
            .rendered(with: context)

        return BazScreen(
            title: "Baz Screen",
            backgroundColor: .green
        ).tabScreen(barItem: .init(
            title: "Baz",
            image: bazImage,
            badge: bazBadgeValue
        )) {
            sink.send(.tabSelected)
        }
    }

    private var bazImage: UIImage {
        if #available(iOS 13.0, *) {
            return UIImage(systemName: "triangle") ?? UIImage()
        } else {
            return UIImage()
        }
    }
}
