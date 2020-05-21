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

import AlertContainer
import BackStackContainer
import ModalContainer
import Workflow
import WorkflowUI

// MARK: Input and Output

struct RunGameWorkflow: Workflow {
    typealias Output = Never
}

// MARK: State and Initialization

extension RunGameWorkflow {
    struct State {
        var playerX: String
        var playerO: String
        var step: Step

        enum Step {
            case newGame
            case playing
            case maybeQuit
        }
    }

    func makeInitialState() -> RunGameWorkflow.State {
        return State(playerX: "X", playerO: "O", step: .newGame)
    }
}

// MARK: Actions

extension RunGameWorkflow {
    enum Action: WorkflowAction {
        typealias WorkflowType = RunGameWorkflow

        case updatePlayerX(String)
        case updatePlayerO(String)
        case startGame
        case back
        case confirmQuit

        func apply(toState state: inout RunGameWorkflow.State) -> RunGameWorkflow.Output? {
            switch self {
            case let .updatePlayerX(name):
                state.playerX = name

            case let .updatePlayerO(name):
                state.playerO = name

            case .startGame:
                state.step = .playing

            case .back:
                state.step = .newGame

            case .confirmQuit:
                state.step = .maybeQuit
            }

            return nil
        }
    }
}

// MARK: Rendering

extension RunGameWorkflow {
    typealias Rendering = AlertContainerScreen<ModalContainerScreen<BackStackScreen<AnyScreen>>>

    func render(state: RunGameWorkflow.State, context: RenderContext<RunGameWorkflow>) -> Rendering {
        let sink = context.makeSink(of: Action.self)
        var modals: [ModalContainerScreenModal] = []
        var alert: Alert?

        var backStackItems: [BackStackScreen<AnyScreen>.Item] = [BackStackScreen.Item(
            screen: newGameScreen(
                sink: sink,
                playerX: state.playerX,
                playerO: state.playerO
            ).asAnyScreen(),
            barVisibility: .hidden
        )]

        switch state.step {
        case .newGame:
            break

        case .playing:
            let takeTurnsScreen = TakeTurnsWorkflow(
                playerX: state.playerX,
                playerO: state.playerO
            )
            .rendered(with: context)
            backStackItems.append(BackStackScreen.Item(
                screen: takeTurnsScreen.asAnyScreen(),
                barVisibility: .visible(BackStackScreen.BarContent(
                    leftItem: BackStackScreen.BarContent.BarButtonItem.button(BackStackScreen.BarContent.Button(
                        content: .text("Quit"),
                        handler: {
                            sink.send(.confirmQuit)
                        }
                    ))
                ))
            ))

        case .maybeQuit:

            let takeTurnsScreen = TakeTurnsWorkflow(
                playerX: state.playerX,
                playerO: state.playerO
            )
            .rendered(with: context)
            backStackItems.append(BackStackScreen.Item(
                screen: takeTurnsScreen.asAnyScreen(),
                barVisibility: .visible(BackStackScreen.BarContent(
                    leftItem: BackStackScreen.BarContent.BarButtonItem.button(BackStackScreen.BarContent.Button(
                        content: .text("Quit"),
                        handler: {
                            sink.send(.confirmQuit)
                        }
                    ))
                ))
            ))

            let (confirmQuitScreen, confirmQuitAlert) = ConfirmQuitWorkflow()
                .mapOutput { output -> Action in
                    switch output {
                    case .cancel:
                        return .startGame
                    case .quit:
                        return .back
                    }
                }
                .rendered(with: context)
            alert = confirmQuitAlert
            modals.append(ModalContainerScreenModal(screen: AnyScreen(confirmQuitScreen), style: .fullScreen, key: "0", animated: true))
        }

        let modalContainerScreen = ModalContainerScreen(baseScreen: BackStackScreen(items: backStackItems), modals: modals)

        return AlertContainerScreen(baseScreen: modalContainerScreen, alert: alert)
    }

    private func newGameScreen(sink: Sink<Action>, playerX: String, playerO: String) -> NewGameScreen {
        return NewGameScreen(
            playerX: playerX,
            playerO: playerO,
            eventHandler: { event in
                switch event {
                case .startGame:
                    sink.send(.startGame)

                case let .playerXChanged(name):
                    sink.send(.updatePlayerX(name))

                case let .playerOChanged(name):
                    sink.send(.updatePlayerO(name))
                }
            }
        )
    }
}
