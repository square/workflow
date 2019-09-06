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


// HAX test JSON decoding
let data = """
        [
          {
            "url": "https://api.github.com/repos/square/workflow/issues/605",
            "title": "Remove key from TypedWorker helpers",
            "body": "_Everybody_ is confused why this exists along with the key passed to `runningWorker`, I don't think I've seen anyone actually use this, and if you _do_ need this functionality it's just better to write a custom worker anyway."
          },
          {
            "url": "https://api.github.com/repos/square/workflow/issues/604",
            "title": "RenderTester should allow test to inspect which workers were all rendered",
            "body": ""
          }
        ]
        """.data(using: .utf8)!

struct GithubIssue: Codable {
    var title: String
    var body: String
}

struct IssueService {
    func fetchIssues() -> SignalProducer<[GithubIssue], AnyError> {
        let url = URL(string: "https://api.github.com/repos/square/workflow/issues")!
        let urlRequest = URLRequest(url: url)
        return URLSession.shared.reactive.data(with: urlRequest).attemptMap { arg in
            // TODO: Inspect the response before trying to decode.
            let (data, _) = arg

            let decoder = JSONDecoder()
            let issues = try decoder.decode([GithubIssue].self, from: data)
            return issues
        }
    }
}
