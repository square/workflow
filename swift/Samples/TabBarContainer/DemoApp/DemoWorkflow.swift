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
import TabBarContainer
import Workflow
import WorkflowUI

// MARK: Input and Output

struct DemoWorkflow: Workflow {

    typealias Output = Never

}

// MARK: State and Initialization

extension DemoWorkflow {

    enum State {
        case baz
        case foo
    }

    func makeInitialState() -> State {
        return .baz
    }

    func workflowDidChange(from previousWorkflow: DemoWorkflow, state: inout State) {
    }

}

// MARK: Actions

extension DemoWorkflow {
    enum Action: WorkflowAction {

        typealias WorkflowType = DemoWorkflow

        case bazTabSelected
        case fooTabSelected

        func apply(toState state: inout WorkflowType.State) -> WorkflowType.Output? {
            switch self {
            case .bazTabSelected:
                state = .baz
            case .fooTabSelected:
                state = .foo
            }

            return nil
        }
    }
}

// MARK: Rendering

extension DemoWorkflow {

    typealias Rendering = TabBarContainerScreen

    func render(state: State, context: RenderContext<DemoWorkflow>) -> Rendering {
        let sink = context.makeSink(of: Action.self)

        let bazScreen = context.render(workflow: BazWorkflow(), key: "baz") { _ in
            Action.bazTabSelected
        }

        let fooScreen = FooWorkflow().rendered(with: context)
            .tabScreen(barItem: .init(title: "Foo", image: .fooBarImage)) {
                sink.send(.fooTabSelected)
            }

        return TabBarContainerScreen(
            screens: [bazScreen, fooScreen],
            selectedIndex: (state == .baz ? 0 : 1)
        )
    }

}

extension UIImage {
    static var fooBarImage: UIImage {
        if #available(iOS 13.0, *) {
            return UIImage(systemName: "square")!
        } else {
            return UIImage()
        }
    }
}
