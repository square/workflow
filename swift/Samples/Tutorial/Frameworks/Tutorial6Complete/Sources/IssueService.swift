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
import Foundation
import ReactiveSwift
import Result


// Define the `IssueService` as a protocol so it can be mocked in tests.
protocol IssueService {
    func fetchIssues() -> SignalProducer<[GithubIssue], AnyError>
}


struct GithubIssue: Codable {
    var title: String
    var body: String
}


// The real `IssueService` that will request the issue list from github.
struct RealIssueService: IssueService {
    func fetchIssues() -> SignalProducer<[GithubIssue], AnyError> {
        let url = URL(string: "https://api.github.com/repos/square/workflow/issues")!
        let urlRequest = URLRequest(url: url)
        return URLSession.shared.reactive.data(with: urlRequest).attemptMap { arg in
            let (data, _) = arg

            let decoder = JSONDecoder()
            let issues = try decoder.decode([GithubIssue].self, from: data)
            return issues
        }
    }
}
