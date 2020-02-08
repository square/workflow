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
import VerticalStackContainer


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

    func workflowDidChange(from previousWorkflow: DemoWorkflow, state: inout State) {
    }

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

    typealias Rendering = VerticalStackContainerScreen<FooScreen, BarScreen, AnyScreen>
    
    private static let colors: [UIColor] = [.red, .blue, .green]

    private static let bodyText = [
        """
        Once upon a midnight dreary, while I pondered, weak and weary,
        Over many a quaint and curious volume of forgotten lore—
        \tWhile I nodded, nearly napping, suddenly there came a tapping,
        As of some one gently rapping, rapping at my chamber door.
        “’Tis some visitor,” I muttered, “tapping at my chamber door—
        \t\t\tOnly this and nothing more.”
        """,
        """
        Tyger Tyger, burning bright,
        In the forests of the night;
        What immortal hand or eye,
        Could frame thy fearful symmetry?
        """,
        """
        Lo! ’t is a gala night
        \tWithin the lonesome latter years!
        An angel throng, bewinged, bedight
        \tIn veils, and drowned in tears,
        Sit in a theatre, to see
        \tA play of hopes and fears,
        While the orchestra breathes fitfully
        \tThe music of the spheres.
        """
    ]

    func render(state: State, context: RenderContext<DemoWorkflow>) -> Rendering {
        let sink = context.makeSink(of: Action.self)
        
        return VerticalStackContainerScreen(
            topScreen: FooScreen(
                title: "Top screen",
                backgroundColor: .cyan,
                viewTapped: { sink.send(.viewTapped) }
            ),
            middleScreen: BarScreen(
                title: DemoWorkflow.bodyText[state % DemoWorkflow.bodyText.count],
                backgroundColor: .green,
                viewTapped: { sink.send(.viewTapped) }
            ),
            bottomScreen: bottomScreenFor(state: state, context: context),
            separatorColor: .black,
            separatorHeight: 1.0 * CGFloat(state)
        )

    }
    
    private func bottomScreenFor(state: State, context: RenderContext<DemoWorkflow>) -> AnyScreen {
        let sink = context.makeSink(of: Action.self)
        
        let color = DemoWorkflow.colors[state % DemoWorkflow.colors.count]
        
        if state % 2 == 0 {
            return AnyScreen(
                FooScreen(
                    title: "Top Foo screen",
                    backgroundColor: color,
                    viewTapped: { sink.send(.viewTapped) }
                )
            )
        } else {
            return AnyScreen(
                BarScreen(
                    title: "Bottom Bar screen",
                    backgroundColor: color,
                    viewTapped: { sink.send(.viewTapped) }
                )
            )
        }

    }

}
