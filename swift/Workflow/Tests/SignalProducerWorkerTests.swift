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
import XCTest
import ReactiveSwift
@testable import Workflow

import ReactiveSwift


final class SignalProducerWorkerTests: XCTestCase {
    fileprivate struct MyError: Error {}
    
    func test_signalProducersOfSameValueTypeAndKeyAreEquivalent() {
        let signalProducer1 = SignalProducer(value: "Hello")
        let worker1 = signalProducer1.asWorker(key: "hello-worker")
        
        let signalProducer2 = SignalProducer(value: "World")
        let worker2 = signalProducer2.asWorker(key: "hello-worker")
        
        XCTAssertTrue(worker1.isEquivalent(to: worker2))
    }
    
    func test_signalProducersOfSameValueTypeDifferentKeyAreNotEquivalent() {
        let signalProducer1 = SignalProducer(value: "Hello")
        let worker1 = signalProducer1.asWorker(key: "hello-worker")
        
        let signalProducer2 = SignalProducer(value: "World")
        let worker2 = signalProducer2.asWorker(key: "world-worker")
        
        XCTAssertFalse(worker1.isEquivalent(to: worker2))
    }
    
    func test_singalProducerAsWorkerNeverError() {
        let signalProducer = SignalProducer(value: "Hello")
        let worker = signalProducer.asWorker(key: "hello-worker")
        
        XCTAssertNotNil(worker)
        XCTAssertEqual(worker.run().first()?.value, "Hello")
    }
    
    func test_signalProducerAsWorkerErrorTransformersValueEmission() {
        let signalProducer1 = SignalProducer<String, Error>(value: "Hello")
        let worker1 = signalProducer1.asWorker(key: "hello-worker") { _ in "Ooops 1" }
        
        XCTAssertNotNil(worker1)
        XCTAssertEqual(worker1.run().first()?.value, "Hello")
        
        let signalProducer2 = SignalProducer<String, Error>(value: "World")
        let worker2 = signalProducer2.asWorker(key: "world-worker") { _ in SignalProducer(value: "Ooops 2") }
        
        XCTAssertNotNil(worker2)
        XCTAssertEqual(worker2.run().first()?.value, "World")
        
        let signalProducer3 = SignalProducer<String, Error>(value: "Foo")
        let worker3 = signalProducer3.asWorker(key: "foo-worker")
        
        XCTAssertNotNil(worker3)
        XCTAssertEqual(worker3.run().first()?.value?.value, "Foo")
        XCTAssertNil(worker3.run().first()?.value?.error)
    }
    
    func test_signalProducerAsWorkerErrorTransformersErrorEmission() {
        let signalProducer1 = SignalProducer<String, Error>(error: MyError())
        let worker1 = signalProducer1.asWorker(key: "error-worker-1") { _ in "Ooops 1" }
        
        XCTAssertNotNil(worker1)
        XCTAssertEqual(worker1.run().first()?.value, "Ooops 1")
        
        let signalProducer2 = SignalProducer<String, Error>(error: MyError())
        let worker2 = signalProducer2.asWorker(key: "error-worker-2") { _ in SignalProducer(value: "Ooops 2") }

        XCTAssertNotNil(worker2)
        XCTAssertEqual(worker2.run().first()?.value, "Ooops 2")
        
        let signalProducer3 = SignalProducer<String, Error>(error: MyError())
        let worker3 = signalProducer3.asWorker(key: "error-worker-3")
        
        XCTAssertNotNil(worker3)
        XCTAssertNil(worker3.run().first()?.value?.value)
        XCTAssertNotNil(worker3.run().first()?.value?.error)
    }
}
