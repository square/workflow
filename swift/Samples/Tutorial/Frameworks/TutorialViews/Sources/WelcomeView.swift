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


public final class WelcomeView: UIView {
    public var name: String {
        didSet {
            nameField.text = name
        }
    }

    public var onNameChanged: (String) -> Void
    public var onLoginTapped: () -> Void

    let welcomeLabel: UILabel
    let nameField: UITextField
    let button: UIButton

    public override init(frame: CGRect) {

        self.name = ""
        self.onNameChanged = { _ in }
        self.onLoginTapped = {}

        welcomeLabel = UILabel(frame: .zero)
        nameField = UITextField(frame: .zero)
        button = UIButton(frame: .zero)

        super.init(frame: frame)

        welcomeLabel.text = "Welcome! Please Enter Your Name"
        welcomeLabel.textAlignment = .center

        nameField.backgroundColor = UIColor(white: 0.92, alpha: 1.0)
        nameField.addTarget(self, action: #selector(textDidChange(sender:)), for: .editingChanged)

        button.backgroundColor = UIColor(red: 41 / 255, green: 150 / 255, blue: 204 / 255, alpha: 1.0)
        button.setTitle("Login", for: .normal)
        button.addTarget(self, action: #selector(buttonTapped(sender:)), for: .touchUpInside)

        addSubview(welcomeLabel)
        addSubview(nameField)
        addSubview(button)
    }

    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    public override func layoutSubviews() {
        super.layoutSubviews()

        let inset: CGFloat = 12.0
        let height: CGFloat = 44.0
        var yOffset = (bounds.size.height - (2 * height + inset)) / 2.0

        welcomeLabel.frame = CGRect(
            x: bounds.origin.x,
            y: bounds.origin.y,
            width: bounds.size.width,
            height: yOffset)

        nameField.frame = CGRect(
            x: bounds.origin.x,
            y: yOffset,
            width: bounds.size.width,
            height: height)
            .insetBy(dx: inset, dy: 0.0)

        yOffset += height + inset
        button.frame = CGRect(
            x: bounds.origin.x,
            y: yOffset,
            width: bounds.size.width,
            height: height)
            .insetBy(dx: inset, dy: 0.0)
    }

    @objc private func textDidChange(sender: UITextField) {
        guard let text = sender.text else {
            return
        }
        onNameChanged(text)
    }

    @objc private func buttonTapped(sender: UIButton) {
        onLoginTapped()
    }
}
