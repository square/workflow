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

import ReactiveSwift
import Workflow

extension Property: AnyWorkflowConvertible {

    public func asAnyWorkflow() -> AnyWorkflow<Value, Never> {
        return PropertyWorkflow(property: self).asAnyWorkflow()
    }


}

private struct PropertyWorkflow<Value>: Workflow {
    var property: Property<Value>
    typealias Output = Never
    typealias State = Value
    func makeInitialState() -> State {
        return property.value
    }
    func workflowDidChange(from previousWorkflow: PropertyWorkflow, state: inout State) {
    }
    typealias Rendering = Value
    func render(state: State, context: RenderContext<PropertyWorkflow>) -> Rendering {
        let sink = context.makeSink(of: AnyWorkflowAction.self)
        // Use the object identifier of the property as the key (so we
        // resubscribe if the property instance is different)
        context.runSideEffect(key: ObjectIdentifier(property)) { [property] lifetime in
            property
                .signal
                .take(during: lifetime)
                .map { value in
                    AnyWorkflowAction { state in
                        state = value
                        return nil
                    }
                }
                .observe(on: QueueScheduler.workflowExecution)
                .observeValues(sink.send)
        }
        return property.value
    }
}
