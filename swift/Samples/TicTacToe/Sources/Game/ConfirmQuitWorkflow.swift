//  
//  ConfirmQuitWorkflow.swift
//  AppHost-Development-Unit-Tests
//
//  Created by Astha Trivedi on 3/26/20.
//

import Workflow
import WorkflowUI
import ReactiveSwift
import BackStackContainer


// MARK: Input and Output

struct ConfirmQuitWorkflow: Workflow {
    
    //let baseScreen: AnyScreen
    
    typealias Output = Never
//    enum Output {
//        case quit
//        case back
//    }
}


// MARK: State and Initialization

extension ConfirmQuitWorkflow {

    struct State {
//        var step: Step
//        enum Step {
//            case confirmOnce
//            case confirmTwice
//        }
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

        typealias WorkflowType = ConfirmQuitWorkflow

        func apply(toState state: inout ConfirmQuitWorkflow.State) -> ConfirmQuitWorkflow.Output? {

            switch self {
                // Update state and produce an optional output based on which action was received.
            }

        }
    }
}


// MARK: Workers

extension ConfirmQuitWorkflow {

    struct ConfirmQuitWorker: Worker {

        enum Output {

        }

        func run() -> SignalProducer<Output, Never> {
            fatalError()
        }

        func isEquivalent(to otherWorker: ConfirmQuitWorker) -> Bool {
            return true
        }

    }

}

// MARK: Rendering

extension ConfirmQuitWorkflow {
    
//    typealias Rendering = ModalContainerScreen
//
//    func render(state: ConfirmQuitWorkflow.State, context: RenderContext<ConfirmQuitWorkflow>) -> Rendering {
//
//        return ModalContainerScreen(baseScreen: , modals: <#T##[ModalContainerScreen.Modal]#>)
//    }

    typealias Rendering = ConfirmQuitScreen

    func render(state: ConfirmQuitWorkflow.State, context: RenderContext<ConfirmQuitWorkflow>) -> Rendering {

        return ConfirmQuitScreen(Question: "Are you sure??")
    }
}
