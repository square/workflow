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

import Foundation
import RxSwift
import Workflow

extension Observable: AnyWorkflowConvertible {

    public func asAnyWorkflow() -> AnyWorkflow<Void, Element> {
        return ObservableWorkflow(observable: self).asAnyWorkflow()
    }

}


private struct ObservableWorkflow<Element>: Workflow {
    var observable: Observable<Element>
    typealias Output = Element
    typealias State = UUID
    func makeInitialState() -> State {
        return UUID()
    }
    func workflowDidChange(from previousWorkflow: ObservableWorkflow, state: inout State) {
    }
    typealias Rendering = Void
    func render(state: State, context: RenderContext<ObservableWorkflow>) -> Void {
        let sink = context.makeSink(of: AnyWorkflowAction.self)
        context.runSideEffect(key: state) { [observable] lifetime in
            let disposable = observable
                .map { AnyWorkflowAction(sendingOutput: $0) }
                .observeOn(SerialDispatchQueueScheduler.workflowExecution)
                .subscribe(onNext: sink.send)
            lifetime.onEnded {
                disposable.dispose()
            }
        }
    }
}
