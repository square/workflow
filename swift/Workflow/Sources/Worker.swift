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

/// Workers define a unit of asynchronous work.
///
/// During a render pass, a workflow can ask the context to await the result of a worker.
///
/// When this occurs, the context checks to see if there is already a running worker of the same type.
/// If there is, and if the workers are 'equivalent', the context leaves the existing worker running.
///
/// If there is not an existing worker of this type, the context will kick off the new worker (via `run`).
public protocol Worker: AnyWorkflowConvertible {

    /// The type of output events returned by this worker.
    associatedtype Output
    
    /// Returns a signal producer to execute the work represented by this worker.
    func run() -> SignalProducer<Output, Never>

    /// Returns `true` if the other worker should be considered equivalent to `self`. Equivalence should take into
    /// account whatever data is meaninful to the task. For example, a worker that loads a user account from a server
    /// would not be equivalent to another worker with a different user ID.
    func isEquivalent(to otherWorker: Self) -> Bool
}

extension Worker where Self: Equatable {

    public func isEquivalent(to otherWorker: Self) -> Bool {
        return self == otherWorker
    }

}

extension Worker {

    public func asAnyWorkflow() -> AnyWorkflow<Void, Output> {
        return WorkerWorkflow(worker: self).asAnyWorkflow()
    }

}


import Foundation


fileprivate struct WorkerWorkflow<WorkerType: Worker>: Workflow {

    var worker: WorkerType

    typealias Output = WorkerType.Output

    typealias State = UUID

    func makeInitialState() -> State {
        return UUID()
    }

    func workflowDidChange(from previousWorkflow: WorkerWorkflow<WorkerType>, state: inout UUID) {
        if !worker.isEquivalent(to: previousWorkflow.worker) {
            state = UUID()
        }
    }

    typealias Rendering = Void

    func render(state: State, context: RenderContext<WorkerWorkflow>) -> Rendering {
        let sink = context.makeSink(of: AnyWorkflowAction.self)
        context.runSideEffect(key: state) { lifetime in
            worker
                .run()
                .take(during: lifetime)
                .map { AnyWorkflowAction(sendingOutput: $0) }
                .observe(on: QueueScheduler.workflowExecution)
                .startWithValues(sink.send)
        }
    }

}
