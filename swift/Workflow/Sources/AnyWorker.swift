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

/// A  type-erased wrapped around a Worker producing a specific given Output type.
public struct AnyWorker<Output>: Worker {

    private let storage: AnyStorage

    fileprivate init<WorkerType>(worker: WorkerType, outputTransform: @escaping (WorkerType.Output) -> Output) where WorkerType: Worker {
        storage = Storage(worker: worker, outputTransform: outputTransform)
    }

    /// Initializes a new type-erased wrapper for the given worker
    public init<WorkerType>(_ worker: WorkerType) where WorkerType: Worker, WorkerType.Output == Output {
        self.init(worker: worker, outputTransform: { $0 })
    }

    public func run() -> SignalProducer<Output, Never> {
        return storage.run()
    }

    public func isEquivalent(to otherWorker: AnyWorker<Output>) -> Bool {
        return storage.isEquivalent(to: otherWorker.storage)
    }

}

extension Worker {

    public func mapOutput<T>(_ transform: @escaping (Output) -> T) -> AnyWorker<T> {
        return AnyWorker(worker: self, outputTransform: transform)
    }

}

extension AnyWorker {

    fileprivate class AnyStorage {

        func run() -> SignalProducer<Output, Never> {
            fatalError()
        }

        func isEquivalent(to otherWorker: AnyStorage) -> Bool {
            fatalError()
        }


    }

    fileprivate final class Storage<W: Worker>: AnyStorage {

        let worker: W
        let outputTransform: (W.Output) -> Output

        init(worker: W, outputTransform: @escaping (W.Output) -> Output) {
            self.worker = worker
            self.outputTransform = outputTransform
        }

        override func run() -> SignalProducer<Output, Never> {
            return worker.run().map(outputTransform)
        }

        override func isEquivalent(to otherStorage: AnyStorage) -> Bool {
            guard let otherStorage = otherStorage as? Storage<W> else {
                return false
            }
            return worker.isEquivalent(to: otherStorage.worker)
        }

    }

}
