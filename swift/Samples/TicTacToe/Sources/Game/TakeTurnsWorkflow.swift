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

// MARK: Input and Output

struct TakeTurnsWorkflow: Workflow {
    var playerX: String
    var playerO: String

    typealias Output = Never
}

// MARK: State and Initialization

extension TakeTurnsWorkflow {
    struct State: Equatable {
        var board: Board
        var gameState: GameState
    }

    func makeInitialState() -> TakeTurnsWorkflow.State {
        return State(board: Board(), gameState: .ongoing(turn: .x))
    }

    func workflowDidChange(from previousWorkflow: TakeTurnsWorkflow, state: inout State) {}
}

// MARK: Actions

extension TakeTurnsWorkflow {
    enum Action: WorkflowAction {
        typealias WorkflowType = TakeTurnsWorkflow

        case selected(row: Int, col: Int)

        func apply(toState state: inout TakeTurnsWorkflow.State) -> TakeTurnsWorkflow.Output? {
            switch state.gameState {
            case let .ongoing(turn: turn):
                switch self {
                case let .selected(row: row, col: col):
                    if !state.board.isEmpty(row: row, col: col) {
                        return nil
                    }

                    state.board.takeSquare(row: row, col: col, player: turn)

                    if state.board.hasVictory() {
                        state.gameState = .win(turn)
                        return nil
                    } else if state.board.isFull() {
                        state.gameState = .tie
                        return nil
                    } else {
                        state.gameState.toggle()
                        return nil
                    }
                }

            case .tie:
                return nil
            case .win:
                return nil
            }
        }
    }
}

// MARK: Rendering

extension TakeTurnsWorkflow {
    typealias Rendering = GamePlayScreen

    func render(state: TakeTurnsWorkflow.State, context: RenderContext<TakeTurnsWorkflow>) -> Rendering {
        let sink = context.makeSink(of: Action.self)

        return GamePlayScreen(
            gameState: state.gameState,
            playerX: playerX,
            playerO: playerO,
            board: state.board.rows,
            onSelected: { row, col in
                sink.send(.selected(row: row, col: col))
            }
        )
    }
}
