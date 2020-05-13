/*
 * Copyright 2020 Square Inc.
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

import WorkflowUI

struct LoadingScreen: Screen {
    func viewControllerDescription(environment: ViewEnvironment) -> ViewControllerDescription {
        return LoadingScreenViewController.description(for: self, environment: environment)
    }
}

private final class LoadingScreenViewController: ScreenViewController<LoadingScreen> {
    let loadingLabel = UILabel(frame: .zero)

    override func viewDidLoad() {
        super.viewDidLoad()

        loadingLabel.font = UIFont.boldSystemFont(ofSize: 44.0)
        loadingLabel.textColor = .black
        loadingLabel.textAlignment = .center
        loadingLabel.text = "Loading..."

        view.backgroundColor = .white

        view.addSubview(loadingLabel)
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()

        loadingLabel.frame = view.bounds
    }
}
