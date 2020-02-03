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

import SwiftUI
import Workflow
import WorkflowSwiftUI

struct CounterView: View {
    var body: some View {
        WorkflowView(workflow: CounterWorkflow(),
                     onOutput: { _ in }) { rendering in
            VStack {
                Text("The value is \(rendering.value)")
                Button(action: rendering.onIncrement) {
                    Text("+")
                }
                Button(action: rendering.onDecrement) {
                    Text("-")
                }
            }
        }
    }
}

struct CounterScreen {
    let value: Int
    var onIncrement: () -> Void
    var onDecrement: () -> Void
}

struct CounterWorkflow: Workflow {
    enum Action: WorkflowAction {
        case increment
        case decrement
        
        func apply(toState state: inout Int) -> Never? {
            switch self {
            case .increment:
                state += 1
            case .decrement:
                state -= 1
            }
            return nil
        }

        typealias WorkflowType = CounterWorkflow
    }

    func makeInitialState() -> Int {
        return 0
    }

    func workflowDidChange(from previousWorkflow: CounterWorkflow, state: inout Int) {
    }

    func render(state: Int, context: RenderContext<CounterWorkflow>) -> CounterScreen {
        let sink = context.makeSink(of: Action.self)
        return CounterScreen(value: state,
                             onIncrement: {
                                 sink.send(.increment)
                             }, onDecrement: {
                                 sink.send(.decrement)
        })
    }

    typealias Output = Never
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        CounterView()
    }
}
