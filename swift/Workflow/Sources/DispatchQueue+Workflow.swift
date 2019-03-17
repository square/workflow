import Foundation
import ReactiveSwift


extension DispatchQueue {

    static let workflowExecution: DispatchQueue = .main

}

extension QueueScheduler {

    static let workflowExecution: QueueScheduler = QueueScheduler(
        qos: .userInteractive,
        name: "com.squareup.workflow",
        targeting: DispatchQueue.workflowExecution)

}


