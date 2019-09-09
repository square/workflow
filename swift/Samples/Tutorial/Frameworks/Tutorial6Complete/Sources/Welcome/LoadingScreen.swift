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


struct LoadingScreen: Screen {
    // The loading screen does not have any parameters, will just show "Loading..."
}


final class LoadingViewController: ScreenViewController<LoadingScreen> {
    let loadingLabel: UILabel

    required init(screen: LoadingScreen, viewRegistry: ViewRegistry) {
        loadingLabel = UILabel(frame: .zero)

        super.init(screen: screen, viewRegistry: viewRegistry)
        update(with: screen)
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        loadingLabel.text = "Loading..."
        loadingLabel.font = UIFont.systemFont(ofSize: 44.0)
        loadingLabel.textColor = .black
        loadingLabel.textAlignment = .center
        view.addSubview(loadingLabel)
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()

        loadingLabel.frame = view.bounds
    }

    override func screenDidChange(from previousScreen: LoadingScreen) {
        update(with: screen)
    }

    private func update(with screen: LoadingScreen) {
        /// Update UI
    }

}


extension ViewRegistry {

    public mutating func registerLoadingScreen() {
        self.register(screenViewControllerType: LoadingViewController.self)
    }

}
