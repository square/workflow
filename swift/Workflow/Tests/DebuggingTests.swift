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
