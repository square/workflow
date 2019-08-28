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

public class DebuggingTests: XCTestCase {

    func test_debugTreeCoding() {
        let tree = WorkflowHierarchyDebugSnapshot(
            workflowType: "foo",
            stateDescription: "bar",
            children: [
                WorkflowHierarchyDebugSnapshot.Child(
                    key: "a",
                    snapshot: WorkflowHierarchyDebugSnapshot(
                        workflowType: "hello",
                        stateDescription: "world")),
                WorkflowHierarchyDebugSnapshot.Child(
                    key: "b",
                    snapshot: WorkflowHierarchyDebugSnapshot(
                        workflowType: "testing",
                        stateDescription: "123"))
            ])

        let encoded = try! JSONEncoder().encode(tree)
        let decoded = try! JSONDecoder().decode(WorkflowHierarchyDebugSnapshot.self, from: encoded)

        XCTAssertEqual(tree, decoded)
    }

    func test_debugUpdateInfoCoding() {

        let info = WorkflowUpdateDebugInfo(
            workflowType: "foo",
            kind: .didUpdate(
                source: .subtree(
                    WorkflowUpdateDebugInfo(
                        workflowType: "baz",
                        kind: .didUpdate(source: .external))
                )))

        let encoded = try! JSONEncoder().encode(info)
        let decoded = try! JSONDecoder().decode(WorkflowUpdateDebugInfo.self, from: encoded)

        XCTAssertEqual(info, decoded)
    }

}
