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

import Workflow
import WorkflowUI

struct LoginScreen: Screen {
    var title: String
    var email: String
    var onEmailChanged: (String) -> Void
    var password: String
    var onPasswordChanged: (String) -> Void
    var onLoginTapped: () -> Void

    func viewControllerDescription(environment: ViewEnvironment) -> ViewControllerDescription {
        return ViewControllerDescription(
            build: { LoginViewController() },
            update: { $0.update(with: self) }
        )
    }
}

private final class LoginViewController: UIViewController {
    private let welcomeLabel: UILabel = UILabel(frame: .zero)
    private let emailField: UITextField = UITextField(frame: .zero)
    private let passwordField: UITextField = UITextField(frame: .zero)
    private let button: UIButton = UIButton(frame: .zero)
    private var onEmailChanged: (String) -> Void = { _ in }
    private var onPasswordChanged: (String) -> Void = { _ in }
    private var onLoginTapped: () -> Void = {}

    override func viewDidLoad() {
        super.viewDidLoad()

        view.backgroundColor = .white

        welcomeLabel.textAlignment = .center

        emailField.placeholder = "email@address.com"
        emailField.autocapitalizationType = .none
        emailField.autocorrectionType = .no
        emailField.textContentType = .emailAddress
        emailField.backgroundColor = UIColor(white: 0.92, alpha: 1.0)
        emailField.addTarget(self, action: #selector(textDidChange(sender:)), for: .editingChanged)

        passwordField.placeholder = "password"
        passwordField.isSecureTextEntry = true
        passwordField.backgroundColor = UIColor(white: 0.92, alpha: 1.0)
        passwordField.addTarget(self, action: #selector(textDidChange(sender:)), for: .editingChanged)

        button.backgroundColor = UIColor(red: 41 / 255, green: 150 / 255, blue: 204 / 255, alpha: 1.0)
        button.setTitle("Login", for: .normal)
        button.addTarget(self, action: #selector(buttonTapped(sender:)), for: .touchUpInside)

        view.addSubview(welcomeLabel)
        view.addSubview(emailField)
        view.addSubview(passwordField)
        view.addSubview(button)
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()

        let inset: CGFloat = 12.0
        let height: CGFloat = 44.0
        var yOffset = (view.bounds.size.height - (3 * height + inset)) / 2.0

        welcomeLabel.frame = CGRect(
            x: view.bounds.origin.x,
            y: view.bounds.origin.y,
            width: view.bounds.size.width,
            height: yOffset
        )

        emailField.frame = CGRect(
            x: view.bounds.origin.x,
            y: yOffset,
            width: view.bounds.size.width,
            height: height
        )
        .insetBy(dx: inset, dy: 0.0)

        yOffset += height + inset

        passwordField.frame = CGRect(
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

    func update(with screen: LoginScreen) {
        welcomeLabel.text = screen.title
        emailField.text = screen.email
        passwordField.text = screen.password
        onEmailChanged = screen.onEmailChanged
        onPasswordChanged = screen.onPasswordChanged
        onLoginTapped = screen.onLoginTapped
    }

    @objc private func textDidChange(sender: UITextField) {
        guard let text = sender.text else {
            return
        }
        if sender == emailField {
            onEmailChanged(text)
        } else if sender == passwordField {
            onPasswordChanged(text)
        }
    }

    @objc private func buttonTapped(sender: UIButton) {
        onLoginTapped()
    }
}
