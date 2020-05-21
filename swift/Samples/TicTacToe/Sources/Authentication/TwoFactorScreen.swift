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

struct TwoFactorScreen: Screen {
    var title: String
    var onLoginTapped: (String) -> Void

    func viewControllerDescription(environment: ViewEnvironment) -> ViewControllerDescription {
        return TwoFactorViewController.description(for: self, environment: environment)
    }
}

private final class TwoFactorViewController: ScreenViewController<TwoFactorScreen> {
    let titleLabel = UILabel(frame: .zero)
    let twoFactorField = UITextField(frame: .zero)
    let button = UIButton(frame: .zero)

    override func viewDidLoad() {
        super.viewDidLoad()

        titleLabel.textAlignment = .center

        twoFactorField.placeholder = "one time token"
        twoFactorField.backgroundColor = UIColor(white: 0.92, alpha: 1.0)

        button.backgroundColor = UIColor(red: 41 / 255, green: 150 / 255, blue: 204 / 255, alpha: 1.0)
        button.setTitle("Login", for: .normal)
        button.addTarget(self, action: #selector(buttonTapped(sender:)), for: .touchUpInside)

        view.addSubview(titleLabel)
        view.addSubview(twoFactorField)
        view.addSubview(button)
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()

        let inset: CGFloat = 12.0
        let height: CGFloat = 44.0

        var yOffset = (view.bounds.size.height - (2 * height + inset)) / 2.0

        titleLabel.frame = CGRect(
            x: view.bounds.origin.x,
            y: view.bounds.origin.y,
            width: view.bounds.size.width,
            height: yOffset
        )

        twoFactorField.frame = CGRect(
            x: view.bounds.origin.x,
            y: yOffset,
            width: view.bounds.size.width,
            height: height
        )
        .insetBy(dx: inset, dy: 0.0)

        yOffset += height + inset

        button.frame = CGRect(
            x: view.bounds.origin.x,
            y: yOffset,
            width: view.bounds.size.width,
            height: height
        )
        .insetBy(dx: inset, dy: 0.0)
    }

    override func screenDidChange(from previousScreen: TwoFactorScreen, previousEnvironment: ViewEnvironment) {
        super.screenDidChange(from: previousScreen, previousEnvironment: previousEnvironment)
        titleLabel.text = screen.title
    }

    @objc private func buttonTapped(sender: UIButton) {
        guard let twoFactorCode = twoFactorField.text else {
            return
        }
        screen.onLoginTapped(twoFactorCode)
    }
}
