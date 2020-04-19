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
import Workflow
import WorkflowUI
import ReactiveSwift
import TabBarContainer


// MARK: Input and Output

struct FooWorkflow: Workflow {

    typealias Output = Never

}


// MARK: State and Initialization

extension FooWorkflow {

    typealias State = Void

}


// MARK: Actions

extension FooWorkflow {

    enum Action: WorkflowAction {

        typealias WorkflowType = FooWorkflow

        func apply(toState state: inout FooWorkflow.State) -> FooWorkflow.Output? {
            return nil
        }
    }
}

// MARK: Rendering

extension FooWorkflow {
    typealias Rendering = FooScreen
    
    func render(state: FooWorkflow.State, context: RenderContext<FooWorkflow>) -> Rendering {
        let fooScreen = FooScreen(
            title: "Foo Screen",
            backgroundColor: .red
        )
        
        return fooScreen
    }
}
