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
import UIKit
import Workflow
import WorkflowUI
import BackStackContainer


public final class TutorialContainerViewController: UIViewController {
    let containerViewController: UIViewController

    public init() {
        // Create a view registry. This will allow the infrastructure to map `Screen` types to their respective view controller type.
        var viewRegistry = ViewRegistry()
        // Register the `WelcomeScreen` and view controller with the convenience method the template provided.
        viewRegistry.registerWelcomeScreen()
        // Register the `TodoListScreen` and view controller with the convenience method the template provided.
        viewRegistry.registerTodoListScreen()
        // Register the `BackStackContainer`, which provides a container for the `BackStackScreen`.
        viewRegistry.registerBackStackContainer()
        // Register the `TodoEditScreen` and view controller with the convenience method the template provided.
        viewRegistry.registerTodoEditScreen()

        // Create a `ContainerViewController` with the `RootWorkflow` as the root workflow, with the view registry we just created.
        containerViewController = ContainerViewController(
            workflow: RootWorkflow(),
            viewRegistry: viewRegistry)

        super.init(nibName: nil, bundle: nil)
    }

    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    public override func viewDidLoad() {
        super.viewDidLoad()

        view.backgroundColor = .white

        addChild(containerViewController)
        view.addSubview(containerViewController.view)
        containerViewController.didMove(toParent: self)

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

        let decoder = JSONDecoder()
        guard let issues = try? decoder.decode([GithubIssue].self, from: data) else {
            return
        }
        print("Issues: \(issues)")
    }

    public override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()

        containerViewController.view.frame = view.bounds
    }
}
