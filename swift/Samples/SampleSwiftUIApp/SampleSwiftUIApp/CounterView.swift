//
//  ContentView.swift
//  SampleSwiftUIApp
//
//  Created by Dhaval Shreyas on 2/3/20.
//  Copyright © 2020 Dhaval Shreyas. All rights reserved.
//

import SwiftUI
import Workflow
import WorkflowUI

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

    func workflowDidChange(from previousWorkflow: CounterWorkflow, state: inout Int) {}

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
