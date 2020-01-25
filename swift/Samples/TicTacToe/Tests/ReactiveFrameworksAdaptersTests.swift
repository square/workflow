import XCTest
import RxSwift
import RxTest
import ReactiveSwift

class ReactiveFrameworksAdapters: XCTestCase {
    func testElementsEmitted() {
        let scheduler = TestScheduler(initialClock: 0)
        
        let xs = scheduler.createHotObservable([
            .next(210, "RxSwift"),
            .next(220, "is"),
            .next(230, "pretty"),
            .next(240, "awesome")
            ])
        
        let res = scheduler.start { xs.asObservable() }
        
        XCTAssertRecordedElements(res.events, ["RxSwift", "is", "pretty", "awesome"])
    }
    
    func testElementsEmittedAsSignalProducer() {
        let myObservable = Observable.from(["RxSwift", "is", "pretty", "awesome"])
        
        let mySignalProducer = myObservable.asSignalProducer()
        
        mySignalProducer
            .observe(on: QueueScheduler.main)
            .flatMapError({ _ in fatalError() })
            .collect()
            .startWithValues({ output in
                XCTAssertEqual(output, ["RxSwift", "is", "pretty", "awesome"])
            })
    }

    struct MyTestError: Error {}
    
    func testErrorsEmittedAsSignalProducer() {
        
        let myObservable = Observable<String>.error(MyTestError())
        
        let mySignalProducer = myObservable.asSignalProducer()
        
        mySignalProducer
            .observe(on: QueueScheduler.main)
            .flatMapError({ error in
                XCTAssertTrue(type(of: error) == MyTestError.self)
                return SignalProducer<String, Never>(value: "error")
            })
            .collect()
            .startWithValues({ output in
                XCTAssertEqual(output, ["error"])
            })
    }
    
}
