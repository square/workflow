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
import BackStackContainer


// MARK: Input and Output

struct ConfirmQuitWorkflow: Workflow {
    
   // let baseScreen: AnyScreen
    
   enum Output {
        case cancel
        case confirm
    }

}


// MARK: State and Initialization

extension ConfirmQuitWorkflow {

    struct State {

    }

    func makeInitialState() -> ConfirmQuitWorkflow.State {
        return State()
    }

    func workflowDidChange(from previousWorkflow: ConfirmQuitWorkflow, state: inout State) {

    }
}


// MARK: Actions

extension ConfirmQuitWorkflow {

    enum Action: WorkflowAction {
        
        case cancel
        case quit

        typealias WorkflowType = ConfirmQuitWorkflow

        func apply(toState state: inout ConfirmQuitWorkflow.State) -> ConfirmQuitWorkflow.Output? {

            switch self {
                case .cancel:
                    return .cancel
                
                case .quit:
                    return .confirm
            }
        }
    }
}

// MARK: Rendering

extension ConfirmQuitWorkflow {
    
    typealias Rendering = ConfirmQuitScreen

    func render(state: ConfirmQuitWorkflow.State, context: RenderContext<ConfirmQuitWorkflow>) -> Rendering {
        
        let sink = context.makeSink(of: Action.self)
        
        return ConfirmQuitScreen(
            question: "Are you sure you want to quit?",
            onQuitTapped: {
                sink.send(.quit)
            },
            onCancelTapped: {
                sink.send(.cancel)
            })
    }
}
