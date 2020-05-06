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
import SplitScreenContainer
import Workflow
import WorkflowUI

// MARK: Input and Output

struct DemoWorkflow: Workflow {
    typealias Output = Never
}

// MARK: State and Initialization

extension DemoWorkflow {
    typealias State = Int

    func makeInitialState() -> State {
        return 1
    }

    func workflowDidChange(from previousWorkflow: DemoWorkflow, state: inout State) {}
}

// MARK: Actions

extension DemoWorkflow {
    enum Action: WorkflowAction {
        typealias WorkflowType = DemoWorkflow

        case viewTapped

        func apply(toState state: inout DemoWorkflow.State) -> Never? {
            switch self {
            case .viewTapped:
                state += 1
            }

            return nil
        }
    }
}

// MARK: Rendering

extension DemoWorkflow {
    typealias Rendering = SplitScreenContainerScreen<AnyScreen, FooScreen>

    private static let sizes: [CGFloat] = [.quarter, .third, .half, 0.75]
    private static let colors: [UIColor] = [.red, .blue, .green, .yellow]
    private static let complimentaryColors: [UIColor] = [.blue, .green, .yellow, .purple]

    func render(state: State, context: RenderContext<DemoWorkflow>) -> Rendering {
        let sink = context.makeSink(of: Action.self)

        return SplitScreenContainerScreen(
            leadingScreen: leadingScreenFor(state: state, context: context),
            trailingScreen: FooScreen(title: "Trailing screen", backgroundColor: .green, viewTapped: { sink.send(.viewTapped) }),
            ratio: DemoWorkflow.sizes[state % DemoWorkflow.sizes.count],
            separatorColor: .black,
            separatorWidth: 1.0 * CGFloat(state)
        )
    }

    private func leadingScreenFor(state: State, context: RenderContext<DemoWorkflow>) -> AnyScreen {
        let sink = context.makeSink(of: Action.self)

        let color = DemoWorkflow.colors[state % DemoWorkflow.colors.count]

        if state % 2 == 0 {
            return AnyScreen(
                FooScreen(
                    title: "Leading Foo screen",
                    backgroundColor: color,
                    viewTapped: { sink.send(.viewTapped) }
                )
            )
        } else {
            let complimentaryColor = DemoWorkflow.complimentaryColors[state % DemoWorkflow.complimentaryColors.count]

            return AnyScreen(
                BarScreen(
                    title: "Leading Bar screen",
                    backgroundColors: [color, complimentaryColor],
                    viewTapped: { sink.send(.viewTapped) }
                )
            )
        }
    }
}
