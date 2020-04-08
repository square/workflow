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
                // Update state and produce an optional output based on which action was received.
                case .cancel:
                    return .cancel
                
                case .quit:
                    return .confirm
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
//        return ModalContainerScreen(baseScreen: , modals: )
//    }

    typealias Rendering = ConfirmQuitScreen

    func render(state: ConfirmQuitWorkflow.State, context: RenderContext<ConfirmQuitWorkflow>) -> Rendering {
        
        let sink = context.makeSink(of: Action.self)
        
        return ConfirmQuitScreen(question: "Are you sure you want to quit?",
                              onQuitTapped: {
                                sink.send(.quit)
                              },
                              onCancelTapped: {
                                sink.send(.cancel)
                              })
    }
}
