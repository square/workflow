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
public struct SignalProducerWorker<Value>: Worker {
    /// The type of output events returned by this worker.
    let valueType: Value.Type

    /// The `SignalProducer<Value, Never>` wrapped by this worker.
    let signalProducer: SignalProducer<Value, Never>

    init(signalProducer: SignalProducer<Value, Never>) {
        self.signalProducer = signalProducer
        self.valueType = Value.self
    }

    /// Returns a signal producer to execute the work represented by this worker.
    public func run() -> SignalProducer<Value, Never> {
        return signalProducer
    }

    /// Returns `true` if the other worker should be considered equivalent to `self`. Equivalence should take into
    /// account whatever data is meaninful to the task. For example, a worker that loads a user account from a server
    /// would not be equivalent to another worker with a different user ID.
    public func isEquivalent(to otherWorker: SignalProducerWorker<Value>) -> Bool {
        return valueType == otherWorker.valueType
    }
}

public extension SignalProducer {
    func asWorker(errorTransform: @escaping (Error) -> SignalProducer<Value, Never>) -> SignalProducerWorker<Value> {
        return self
            .flatMapError { errorTransform($0) }
            .asWorker()
    }

    func asWorker(errorTransform: @escaping (Error) -> Value) -> SignalProducerWorker<Value> {
        return self
            .flatMapError({
                return SignalProducer<Value, Never>(value: errorTransform($0))
            })
            .asWorker()
    }
}

public extension SignalProducer where Error == Never {
    func asWorker() -> SignalProducerWorker<Value> {
        return SignalProducerWorker(signalProducer: self)
    }
}
