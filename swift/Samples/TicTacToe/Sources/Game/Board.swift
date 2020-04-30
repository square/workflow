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

import Foundation

enum Player: Equatable {
    case x
    case o
}

struct Board: Equatable {
    private(set) var rows: [[Cell]]

    enum Cell: Equatable {
        case empty
        case taken(Player)
    }

    init() {
        self.rows = [
            [.empty, .empty, .empty],
            [.empty, .empty, .empty],
            [.empty, .empty, .empty],
        ]
    }

    func isFull() -> Bool {
        for row in rows {
            for col in row {
                if col == .empty {
                    return false
                }
            }
        }
        return true
    }

    func hasVictory() -> Bool {
        var done = false

        // Across
        var row = 0
        while !done, row < 3 {
            done =
                rows[row][0] != .empty
                    && rows[row][0] == rows[row][1]
                    && rows[row][0] == rows[row][2]

            row += 1
        }

        // Down
        var col = 0
        while !done, col < 3 {
            done =
                rows[0][col] != .empty
                    && rows[0][col] == rows[1][col]
                    && rows[1][col] == rows[2][col]

            col += 1
        }

        // Diagonal
        if !done {
            done =
                rows[0][0] != .empty
                    && rows[0][0] == rows[1][1]
                    && rows[0][0] == rows[2][2]
        }

        if !done {
            done =
                rows[0][2] != .empty
                    && rows[0][2] == rows[1][1]
                    && rows[0][2] == rows[2][0]
        }

        return done
    }

    func isEmpty(row: Int, col: Int) -> Bool {
        guard row < 3 else {
            fatalError("Received an invalid row \(row)")
        }
        guard col < 3 else {
            fatalError("Received an invalid col \(col)")
        }
        if rows[row][col] == .empty {
            return true
        } else {
            return false
        }
    }

    mutating func takeSquare(row: Int, col: Int, player: Player) {
        guard row < 3 else {
            fatalError("Received an invalid row \(row)")
        }
        guard col < 3 else {
            fatalError("Received an invalid col \(col)")
        }
        guard isEmpty(row: row, col: col) else {
            return
        }

        rows[row][col] = .taken(player)
    }
}
