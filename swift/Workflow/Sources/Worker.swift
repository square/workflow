import ReactiveSwift

/// Workers define a unit of asynchronous work.
///
/// During a render pass, a workflow can ask the context to await the result of a worker.
///
/// When this occurs, the context checks to see if there is already a running worker of the same type.
/// If there is, and if the workers are 'equivalent', the context leaves the existing worker running.
///
/// If there is not an existing worker of this type, the context will kick off the new worker (via `run`).
public protocol Worker {

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
