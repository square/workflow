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

struct NewGameScreen: Screen {
    var playerX: String
    var playerO: String
    var eventHandler: (Event) -> Void

    enum Event {
        case playerXChanged(String)
        case playerOChanged(String)
        case startGame
    }

    func viewControllerDescription(environment: ViewEnvironment) -> ViewControllerDescription {
        return NewGameViewController.description(for: self, environment: environment)
    }
}

final class NewGameViewController: ScreenViewController<NewGameScreen> {
    let playerXLabel = UILabel(frame: .zero)
    let playerXField = UITextField(frame: .zero)
    let playerOLabel = UILabel(frame: .zero)
    let playerOField = UITextField(frame: .zero)
    let startGameButton = UIButton(frame: .zero)

    override func viewDidLoad() {
        super.viewDidLoad()

        playerXLabel.text = "Player X"
        playerXField.backgroundColor = UIColor(white: 0.92, alpha: 1.0)
        playerXField.addTarget(self, action: #selector(onTextChanged(sender:)), for: .editingChanged)

        playerOLabel.text = "Player O"
        playerOField.backgroundColor = UIColor(white: 0.92, alpha: 1.0)
        playerOField.addTarget(self, action: #selector(onTextChanged(sender:)), for: .editingChanged)

        startGameButton.backgroundColor = UIColor(red: 41 / 255, green: 150 / 255, blue: 204 / 255, alpha: 1.0)
        startGameButton.setTitle("Let's Play!", for: .normal)
        startGameButton.addTarget(self, action: #selector(startPressed(sender:)), for: .touchUpInside)

        view.addSubview(playerXLabel)
        view.addSubview(playerXField)
        view.addSubview(playerOLabel)
        view.addSubview(playerOField)
        view.addSubview(startGameButton)
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()

        let inset: CGFloat = 12.0
        let height: CGFloat = 44.0
        var yOffset = (view.bounds.size.height - (3 * height + inset)) / 2.0

        let xSize = playerXLabel.sizeThatFits(CGSize(
            width: view.bounds.size.width,
            height: height
        ))

        playerXLabel.frame = CGRect(
            x: view.bounds.origin.x,
            y: yOffset,
            width: xSize.width,
            height: height
        )

        playerXField.frame = CGRect(
            x: view.bounds.origin.x + xSize.width,
            y: yOffset,
            width: view.bounds.size.width,
            height: height
        )
        .insetBy(dx: inset, dy: 0.0)

        yOffset += height + inset

        let oSize = playerOLabel.sizeThatFits(CGSize(
            width: view.bounds.size.width,
            height: height
        ))

        playerOLabel.frame = CGRect(
            x: view.bounds.origin.x,
            y: yOffset,
            width: oSize.width,
            height: height
        )

        playerOField.frame = CGRect(
            x: view.bounds.origin.x + oSize.width,
            y: yOffset,
            width: view.bounds.size.width,
            height: height
        )
        .insetBy(dx: inset, dy: 0.0)

        yOffset += height + inset

        startGameButton.frame = CGRect(
            x: view.bounds.origin.x,
            y: yOffset,
            width: view.bounds.size.width,
            height: height
        )
        .insetBy(dx: inset, dy: 0.0)

        yOffset += height + inset
    }

    override func screenDidChange(from previousScreen: NewGameScreen, previousEnvironment: ViewEnvironment) {
        super.screenDidChange(from: previousScreen, previousEnvironment: previousEnvironment)

        playerXField.text = screen.playerX
        playerOField.text = screen.playerO
    }

    @objc private func onTextChanged(sender: UITextField) {
        guard let name = sender.text else {
            return
        }

        if sender == playerXField {
            screen.eventHandler(.playerXChanged(name))
        } else if sender == playerOField {
            screen.eventHandler(.playerOChanged(name))
        }
    }

    @objc private func startPressed(sender: UIButton) {
        screen.eventHandler(.startGame)
    }
}
