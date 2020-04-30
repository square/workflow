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
import WorkflowTesting
import XCTest

@testable import Development_SampleTicTacToe

class TakeTurnsWorkflowTests: XCTestCase {
    // MARK: Action Tests

    /*

     _|_|_       X|_|_
     _|_|_   =>  _|_|_
     _|_|_       _|_|_

     */
    func test_action_selected_initialMove() {
        let emptyBoardState = TakeTurnsWorkflow.State(
            board: Board(),
            gameState: .ongoing(turn: .x)
        )

        TakeTurnsWorkflow
            .Action
            .tester(withState: emptyBoardState)
            .send(
                action: .selected(row: 0, col: 0),
                outputAssertions: { output in
                    // This workflow has no outputs.
                    XCTAssertNil(output)
                }
            )
            // After x takes 0, 0 we expect the following state:
            .assertState { state in
                // Board is not full.
                XCTAssertFalse(state.board.isFull())

                // Cell at 0, 0 is not empty.
                XCTAssertFalse(state.board.isEmpty(row: 0, col: 0))

                // Cell at 0, 0 is taken by player x.
                XCTAssertEqual(state.board.rows[0][0], Board.Cell.taken(.x))

                // We do not have a victory.
                XCTAssertFalse(state.board.hasVictory())

                // Game state is now in ongoing but for player o.
                if case GameState.ongoing(turn: .o) = state.gameState {
                    XCTAssertTrue(true)
                } else {
                    XCTFail("after x takes 0, 0. It should be o's turn")
                }
            }
            /*

             X|_|_       X|_|_
             _|_|_   =>  _|O|_
             _|_|_       _|_|_

             */
            .send(
                action: .selected(row: 0, col: 1)
                // After o takes 0, 1 we expect the following state:
            ).assertState { state in
                // Board is not full.
                XCTAssertFalse(state.board.isFull())

                // Cell at 0, 0 is not empty.
                XCTAssertFalse(state.board.isEmpty(row: 0, col: 0))
                // Cell at 0, 1 is not empty.
                XCTAssertFalse(state.board.isEmpty(row: 0, col: 1))

                // Cell at 0, 0 is taken by player x.
                XCTAssertEqual(state.board.rows[0][0], Board.Cell.taken(.x))

                // Cell at 0, 1 is taken by player x.
                XCTAssertEqual(state.board.rows[0][1], Board.Cell.taken(.o))

                // We do not have a victory.
                XCTAssertFalse(state.board.hasVictory())

                // Game state is now in ongoing but for player 0.
                if case GameState.ongoing(turn: .x) = state.gameState {
                    XCTAssertTrue(true)
                } else {
                    XCTFail("after o takes 0, 1. It should be x's turn")
                }
            }
    }

    /*

     X|O|X       X|O|X
     O|X|O   =>  O|X|O
     O|X|_       O|X|O

     */
    func test_action_selected_tieGame() {
        var board = Board()
        board.takeSquare(row: 0, col: 0, player: .x)
        board.takeSquare(row: 0, col: 1, player: .o)
        board.takeSquare(row: 0, col: 2, player: .x)

        board.takeSquare(row: 1, col: 0, player: .o)
        board.takeSquare(row: 1, col: 1, player: .x)
        board.takeSquare(row: 1, col: 2, player: .o)

        board.takeSquare(row: 2, col: 0, player: .o)
        board.takeSquare(row: 2, col: 1, player: .x)

        let boardState = TakeTurnsWorkflow.State(
            board: board,
            gameState: .ongoing(turn: .o)
        )

        TakeTurnsWorkflow
            .Action
            .tester(withState: boardState)
            .send(
                action: .selected(row: 2, col: 2),
                outputAssertions: { output in
                    // This workflow has no outputs.
                    XCTAssertNil(output)
                }
            )
            // After o takes 2, 2 we expect the following state:
            .assertState { state in
                // Board is full
                XCTAssertTrue(state.board.isFull())

                // We do not have a victory.
                XCTAssertFalse(state.board.hasVictory())

                // Game state is now a tie.
                if case GameState.tie = state.gameState {
                    XCTAssertTrue(true)
                } else {
                    XCTFail("when o takes spot 2, 2 we should end up in a tie")
                }
            }
    }

    /*

     X|X|O       X|X|O
     _|O|O   =>  X|O|O
     X|O|X       X|O|X

     */
    func test_action_selected_victory() {
        var board = Board()
        board.takeSquare(row: 0, col: 0, player: .x)
        board.takeSquare(row: 0, col: 1, player: .x)
        board.takeSquare(row: 0, col: 2, player: .o)

        board.takeSquare(row: 1, col: 1, player: .o)
        board.takeSquare(row: 1, col: 2, player: .o)

        board.takeSquare(row: 2, col: 0, player: .x)
        board.takeSquare(row: 2, col: 1, player: .o)
        board.takeSquare(row: 2, col: 2, player: .x)

        let boardState = TakeTurnsWorkflow.State(
            board: board,
            gameState: .ongoing(turn: .x)
        )

        TakeTurnsWorkflow
            .Action
            .tester(withState: boardState)
            .send(
                action: .selected(row: 1, col: 0),
                outputAssertions: { output in
                    // This workflow has no outputs.
                    XCTAssertNil(output)
                }
            )
            // After o takes 2, 2 we expect the following state:
            .assertState { state in
                // Board is full.
                XCTAssertTrue(state.board.isFull())

                // We do have a victory.
                XCTAssertTrue(state.board.hasVictory())

                // Game state is now in a win for player x.
                if case GameState.win(.x) = state.gameState {
                    XCTAssertTrue(true)
                } else {
                    XCTFail("when x takes spot 1, 0 we should end up in a victory for player x")
                }
            }
    }

    // MARK: Render Tests

    // Empty board with X making the first move.
    func test_render_initialBoard() {
        let emptyBoardState = TakeTurnsWorkflow.State(
            board: Board(),
            gameState: .ongoing(turn: .x)
        )

        let expectedState = ExpectedState<TakeTurnsWorkflow>(state: emptyBoardState)

        let renderExpectation = RenderExpectations<TakeTurnsWorkflow>(
            expectedState: expectedState,
            expectedOutput: nil,
            expectedWorkers: [],
            expectedWorkflows: []
        )

        let workflow = TakeTurnsWorkflow(
            playerX: "X",
            playerO: "O"
        )

        workflow
            .renderTester()
            .render(
                with: renderExpectation
            ) { screen in

                // The display value for player X should match what was passed to the workflow.
                XCTAssertEqual(screen.playerX, "X")

                // The display value for player O should match what was passed to the workflow.
                XCTAssertEqual(screen.playerO, "O")

                // The screen state should match with player x going next.
                if case GameState.ongoing(turn: .x) = screen.gameState {
                    XCTAssertTrue(true)
                } else {
                    XCTFail("x should start the game since the board is setup with ongoing(turn: .x)")
                }
                XCTAssertEqual(screen.board, Board().rows)
            }
    }

    func test_render_winningBoard() {
        var board = Board()
        board.takeSquare(row: 0, col: 0, player: .x)
        board.takeSquare(row: 0, col: 1, player: .x)
        board.takeSquare(row: 0, col: 2, player: .o)

        board.takeSquare(row: 1, col: 0, player: .o)
        board.takeSquare(row: 1, col: 1, player: .o)
        board.takeSquare(row: 1, col: 2, player: .o)

        board.takeSquare(row: 2, col: 0, player: .x)
        board.takeSquare(row: 2, col: 1, player: .o)
        board.takeSquare(row: 2, col: 2, player: .x)

        let boardState = TakeTurnsWorkflow.State(
            board: board,
            gameState: .win(.o)
        )

        let expectedState = ExpectedState<TakeTurnsWorkflow>(state: boardState)

        let renderExpectation = RenderExpectations<TakeTurnsWorkflow>(
            expectedState: expectedState,
            expectedOutput: nil,
            expectedWorkers: [],
            expectedWorkflows: []
        )

        let workflow = TakeTurnsWorkflow(
            playerX: "X",
            playerO: "O"
        )

        workflow
            .renderTester(initialState: boardState)
            .render(
                with: renderExpectation
            ) { screen in

                // The display value for player X should match what was passed to the workflow.
                XCTAssertEqual(screen.playerX, "X")

                // The display value for player O should match what was passed to the workflow.
                XCTAssertEqual(screen.playerO, "O")

                // The screen state should match with player o winning.
                if case GameState.win(.o) = screen.gameState {
                    XCTAssertTrue(true)
                } else {
                    XCTFail("o should win the game")
                }
            }
    }
}
