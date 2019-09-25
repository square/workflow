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
@testable import Workflow
import ReactiveSwift

public class AnyWorkflowTests: XCTestCase {


    func testRendersWrappedWorkflow() {

        let workflow = AnyWorkflow(SimpleWorkflow(string: "asdf"))
        let node = WorkflowNode(workflow: PassthroughWorkflow(child: workflow))

        XCTAssertEqual(node.render(), "fdsa")
    }

    func testMapRendering() {

        let workflow = SimpleWorkflow(string: "asdf")
            .mapRendering { string -> String in
                return string + "dsa"
            }
        let node = WorkflowNode(workflow: PassthroughWorkflow(child: workflow))

        XCTAssertEqual(node.render(), "fdsadsa")
    }

}

/// Has no state or output, simply renders a reversed string
fileprivate struct PassthroughWorkflow<Rendering>: Workflow {
    var child: AnyWorkflow<Rendering, Never>
}

extension PassthroughWorkflow {

    struct State {}

    func makeInitialState() -> State {
        return State()
    }

    func workflowDidChange(from previousWorkflow: PassthroughWorkflow<Rendering>, state: inout State) {

    }

    func render(state: State, context: RenderContext<PassthroughWorkflow<Rendering>>) -> Rendering {
        return child.rendered(with: context)
    }

}




/// Has no state or output, simply renders a reversed string
fileprivate struct SimpleWorkflow: Workflow {
    var string: String
}

extension SimpleWorkflow {

    struct State {}

    func makeInitialState() -> State {
        return State()
    }

    func workflowDidChange(from previousWorkflow: SimpleWorkflow, state: inout State) {

    }

    func render(state: State, context: RenderContext<SimpleWorkflow>) -> String {
        return String(string.reversed())
    }

}
