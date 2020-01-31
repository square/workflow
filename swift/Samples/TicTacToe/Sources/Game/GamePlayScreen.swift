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
import WorkflowUI


struct GamePlayScreen: Screen {
    var gameState: GameState
    var playerX: String
    var playerO: String
    var board: [[Board.Cell]]
    var onSelected: (Int, Int) -> Void

    var viewControllerDescription: ViewControllerDescription {
        return GamePlayViewController.description(for: self)
    }
}


final class GamePlayViewController: ScreenViewController<GamePlayScreen> {
    let titleLabel: UILabel
    let cells: [[UIButton]]

    required init(screen: GamePlayScreen) {
        self.titleLabel = UILabel(frame: .zero)
        var cells: [[UIButton]] = []

        for _ in 0..<3 {
            var row: [UIButton] = []
            for _ in 0..<3 {
                row.append(UIButton(frame: .zero))
            }
            cells.append(row)
        }

        self.cells = cells
        super.init(screen: screen)
        update(with: screen)
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        titleLabel.textAlignment = .center
        titleLabel.font = UIFont.systemFont(ofSize: 32.0)
        view.addSubview(titleLabel)

        var toggle = true
        for row in cells {
            for cell in row {
                let backgroundColor: UIColor
                if toggle {
                    backgroundColor = UIColor(white: 0.92, alpha: 1.0)
                } else {
                    backgroundColor = UIColor(white: 0.82, alpha: 1.0)
                }
                cell.backgroundColor = backgroundColor
                toggle = !toggle

                cell.titleLabel?.font = UIFont.boldSystemFont(ofSize: 66.0)
                cell.addTarget(self, action: #selector(buttonPressed(sender:)), for: .touchUpInside)
                view.addSubview(cell)
            }
        }
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()

        let inset: CGFloat = 8.0
        let boardLength = min(view.bounds.width, view.bounds.height) - inset * 2
        let cellLength = boardLength / 3.0

        let bounds = view.bounds.inset(by: view.safeAreaInsets)
        titleLabel.frame = CGRect(
            x: bounds.origin.x,
            y: bounds.origin.y,
            width: bounds.size.width,
            height: 44.0)

        var yOffset = (view.bounds.height - boardLength) / 2.0
        for row in cells {
            var xOffset = inset
            for cell in row {
                cell.frame = CGRect(
                    x: xOffset,
                    y: yOffset,
                    width: cellLength,
                    height: cellLength)

                xOffset += inset + cellLength
            }
            yOffset += inset + cellLength
        }
    }

    override func screenDidChange(from previousScreen: GamePlayScreen) {
        update(with: screen)
    }

    private func update(with screen: GamePlayScreen) {
        let title: String
        switch screen.gameState {

        case .ongoing(turn: let turn):
            switch turn {
            case .x:
                title = "\(screen.playerX), place your 🙅"
            case .o:
                title = "\(screen.playerO), place your 🙆"
            }

        case .tie:
            title = "It's a Tie!"

        case .win(let player):
            switch player {
            case .x:
                title = "The 🙅's have it, \(screen.playerX) wins!"
            case .o:
                title = "The 🙆's have it, \(screen.playerO) wins!"
            }
        }
        titleLabel.text = title

        for row in 0..<3 {
            let cols = screen.board[row]
            for col in 0..<3 {
                switch cols[col] {
                case .empty:
                    cells[row][col].setTitle("", for: .normal)
                case .taken(let player):
                    switch player {
                    case .x:
                        cells[row][col].setTitle("🙅", for: .normal)
                    case .o:
                        cells[row][col].setTitle("🙆", for: .normal)
                    }
                }
            }
        }
    }

    @objc private func buttonPressed(sender: UIButton) {
        for row in 0..<3 {
            let cols = cells[row]
            for col in 0..<3 {
                if cols[col] == sender {
                    screen.onSelected(row, col)
                    return
                }
            }
        }
    }
}
