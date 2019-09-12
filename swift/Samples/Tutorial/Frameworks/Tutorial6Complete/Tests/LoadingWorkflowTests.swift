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
@testable import Tutorial6
import WorkflowTesting


class LoadingWorkflowTests: XCTestCase {

    /*
    func testLoading() {
        let todos = [TodoModel(title: "Title", note: "Note")]

        let expectedOutput = ExpectedOutput<LoadingWorkflow>(
            output: LoadingWorkflow.Output.loadCompleted(todos),
            isEquivalent: { (expected, actual) in
                switch (expected, actual) {
                case (.loadCompleted(let expectedTodos), .loadCompleted(let actualTodos)):
                    return expectedTodos == actualTodos
                case (.loadFailed, .loadFailed):
                    return true
                default:
                    return false
                }
        })
        let expectedWorker = ExpectedWorker(
            worker: LoadingWorkflow.IssueLoadingWorker.self,
            output: nil)

        LoadingWorkflow(issueService: FakeIssueService())
            .renderTester()
            .render(
                expectedOutput: expectedOutput,
                expectedWorkers: [expectedWorker],
                assertions: { _ in })
    }
 */
}
