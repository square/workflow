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
import Result


// MARK: Input and Output

struct LoadingWorkflow: Workflow {
    // The issue service dependency is passed into the workflow.
    var issueService: IssueService

    enum Output {
        // Output when the todo's have finished loading.
        case loadCompleted([TodoModel])
        // Output if the load failed.
        case loadFailed(Error)
    }
}


// MARK: State and Initialization

extension LoadingWorkflow {

    struct State {

    }

    func makeInitialState() -> LoadingWorkflow.State {
        return State()
    }

    func workflowDidChange(from previousWorkflow: LoadingWorkflow, state: inout State) {

    }
}


// MARK: Actions

extension LoadingWorkflow {

    enum Action: WorkflowAction {

        typealias WorkflowType = LoadingWorkflow

        case loadCompleted([TodoModel])
        case loadFailed(Error)

        func apply(toState state: inout LoadingWorkflow.State) -> LoadingWorkflow.Output? {

            switch self {
            case .loadCompleted(let todos):
                return .loadCompleted(todos)

            case .loadFailed(let error):
                return .loadFailed(error)
            }

        }
    }
}


// MARK: Workers

extension LoadingWorkflow {

    struct IssueLoadingWorker: Worker {

        // This worker will use the `LoadingWorkflow` `Action` as its output.
        typealias Output = Action

        // The worker needs a `IssueService` to request Github Issues.
        var issueService: IssueService

        func run() -> SignalProducer<Output, NoError> {
            // Fetch the issues, and map them from any array of `GithubIssue`s to `TodoModel`s.
            return issueService.fetchIssues().map { githubIssues in
                let todos = githubIssues.map { issue -> TodoModel in
                    return TodoModel(title: issue.title, note: issue.body)
                }

                // Return as a `.loadCompleted` action.
                return .loadCompleted(todos)
            }
            .flatMapError { error in
                // As the SignalProducer must return a `NoError`, flat map the error to our failure action.
                SignalProducer(value: .loadFailed(error))
            }
        }

        // Always consider two workers equivalent.
        func isEquivalent(to otherWorker: IssueLoadingWorker) -> Bool {
            return true
        }

    }

}

// MARK: Rendering

extension LoadingWorkflow {
    typealias Rendering = LoadingScreen

    func render(state: LoadingWorkflow.State, context: RenderContext<LoadingWorkflow>) -> Rendering {

        // Request that the `IssueLoadingWorker` be run if it has not been started. When it outputs, the action
        // returned from it will be applied.
        context.awaitResult(for: IssueLoadingWorker(issueService: issueService))

        return LoadingScreen()
    }
}
