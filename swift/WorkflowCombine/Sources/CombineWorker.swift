/*
 * Copyright 2020 Square Inc.
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

import Combine
import Workflow

/// Workers define a unit of asynchronous work.
///
/// During a render pass, a workflow can ask the context to await the result of a worker.
///
/// When this occurs, the context checks to see if there is already a running worker of the same type.
/// If there is, and if the workers are 'equivalent', the context leaves the existing worker running.
///
/// If there is not an existing worker of this type, the context will kick off the new worker (via `run`).
public protocol CombineWorker: AnyWorkflowConvertible {

    associatedtype PublisherType: Publisher where PublisherType.Failure == Never

    /// Returns a signal producer to execute the work represented by this worker.
    func run() -> PublisherType

    /// Returns `true` if the other worker should be considered equivalent to `self`. Equivalence should take into
    /// account whatever data is meaninful to the task. For example, a worker that loads a user account from a server
    /// would not be equivalent to another worker with a different user ID.
    func isEquivalent(to otherWorker: Self) -> Bool
}

extension CombineWorker where Self: Equatable {

    public func isEquivalent(to otherWorker: Self) -> Bool {
        return self == otherWorker
    }

}

extension CombineWorker {

    public func asAnyWorkflow() -> AnyWorkflow<Void, PublisherType.Output> {
        return CombineWorkerWorkflow(worker: self).asAnyWorkflow()
    }

}


import Foundation


fileprivate struct CombineWorkerWorkflow<WorkerType: CombineWorker>: Workflow {

    var worker: WorkerType

    typealias Output = WorkerType.PublisherType.Output

    typealias State = UUID

    func makeInitialState() -> State {
        return UUID()
    }

    func workflowDidChange(from previousWorkflow: CombineWorkerWorkflow<WorkerType>, state: inout UUID) {
        if !worker.isEquivalent(to: previousWorkflow.worker) {
            state = UUID()
        }
    }

    typealias Rendering = Void

    func render(state: State, context: RenderContext<CombineWorkerWorkflow>) -> Rendering {
        worker
            .run()
            .map { AnyWorkflowAction(sendingOutput: $0) }
            .running(with: context, key: state.uuidString)
        return ()
    }

}
