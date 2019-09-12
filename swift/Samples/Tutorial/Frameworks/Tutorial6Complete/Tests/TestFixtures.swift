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
import ReactiveSwift
import Result
@testable import Tutorial6


struct FakeIssueService: IssueService {
    func fetchIssues() -> SignalProducer<[GithubIssue], AnyError> {
        fatalError()
    }
}

struct MockIssueService: IssueService {
    func fetchIssues() -> SignalProducer<[GithubIssue], AnyError> {
        let issues = [GithubIssue(title: "Issue Title", body: "Issue Body")]

        return SignalProducer(value: issues)
    }
}
