import Foundation
import RxSwift
import ReactiveSwift

extension Signal {
    func asObservable() -> Observable<Value> {
            let subject = PublishSubject<Value>()
        
            let disposable = self
                .observe { (event) in
                    switch event {
                    case .value(let value):
                        subject.onNext(value)
                    case .failed(let error):
                        subject.onError(error)
                    case .interrupted:
                        // TODO: What do we do here?
                        subject.onCompleted()
                    case .completed:
                        subject.onCompleted()
                    }
            }
        
            return subject
                .do(
                    onNext: nil,
                    afterNext: nil,
                    onError: nil,
                    afterError: nil,
                    onCompleted: {
                        disposable?.dispose()
                    },
                    afterCompleted: nil,
                    onSubscribe: nil,
                    onSubscribed: nil, onDispose: {
                        disposable?.dispose()
                    })
    }
}

extension ObservableType {
    func asSignalProducer() -> SignalProducer<Element, Error> {
        return SignalProducer<Element, Error>({ (observer, lifetime) in
            let disposable = self
//                .subscribeOn(MainScheduler.instance)
                .subscribe(onNext: { (value) in
                    observer.send(value: value)
                }, onError: { error in
                    observer.send(error: error)
                }, onCompleted: {
                    observer.sendCompleted()
                }, onDisposed: {
                    observer.sendInterrupted()
                })
            
            lifetime
                .observeEnded {
                    disposable.dispose()
            }
        })
    }
    
    func asSignal() -> Signal<Element, Error> {
        return Signal<Element, Error>({ (observer, lifetime) in
            let disposable = self
//                .subscribeOn(MainScheduler.instance)
                .subscribe(onNext: { (value) in
                    observer.send(value: value)
                }, onError: { error in
                    observer.send(error: error)
                }, onCompleted: {
                    observer.sendCompleted()
                }, onDisposed: {
                    observer.sendInterrupted()
                })
            
            lifetime
                .observeEnded {
                    disposable.dispose()
            }
        })
    }
}
