//  ___FILEHEADER___

import Workflow
import WorkflowUI
import ReactiveSwift


// MARK: Input and Output

struct ___VARIABLE_productName___Workflow: Workflow {

    enum Output {

    }
}


// MARK: State and Initialization

extension ___VARIABLE_productName___Workflow {

    struct State {

    }

    func makeInitialState() -> ___VARIABLE_productName___Workflow.State {
        return State()
    }

    func workflowDidChange(from previousWorkflow: ___VARIABLE_productName___Workflow, state: inout State) {

    }
}


// MARK: Actions

extension ___VARIABLE_productName___Workflow {

    enum Action: WorkflowAction {

        typealias WorkflowType = ___VARIABLE_productName___Workflow

        func apply(toState state: inout ___VARIABLE_productName___Workflow.State) -> ___VARIABLE_productName___Workflow.Output? {

            switch self {
                // Update state and produce an optional output based on which action was received.
            }

        }
    }
}


// MARK: Workers

extension ___VARIABLE_productName___Workflow {

    struct ___VARIABLE_productName___Worker: Worker {

        enum Output {

        }

        func run() -> SignalProducer<Output, Never> {
            fatalError()
        }

        func isEquivalent(to otherWorker: ___VARIABLE_productName___Worker) -> Bool {
            return true
        }

    }

}

// MARK: Rendering

extension ___VARIABLE_productName___Workflow {

    func render(state: ___VARIABLE_productName___Workflow.State, context: RenderContext<___VARIABLE_productName___Workflow>) -> String {
        #warning("Don't forget your compose implementation and to return the correct rendering type!")
        return "This is likely not the rendering that you want to return"
    }
}
