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
import BackStackContainer
import ModalContainer


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

    func workflowDidChange(from previousWorkflow: RunGameWorkflow, state: inout State) {

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
            case .updatePlayerX(let name):
                state.playerX = name

            case .updatePlayerO(let name):
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

    typealias Rendering = ModalContainerScreen<AnyScreen>

    func render(state: RunGameWorkflow.State, context: RenderContext<RunGameWorkflow>) -> Rendering {
        let sink = context.makeSink(of: Action.self)
        var modals:[ModalContainerScreen<AnyScreen>.Modal] = []
        
        var backStackItems: [BackStackScreen.Item] = [BackStackScreen.Item(
            screen: newGameScreen(
                sink: sink,
                playerX: state.playerX,
                playerO: state.playerO),
            barVisibility: .hidden)]

        switch state.step {
        case .newGame:
            break

        case .playing:
            let takeTurnsScreen = TakeTurnsWorkflow(
                playerX: state.playerX,
                playerO: state.playerO)
                .rendered(with: context)
            backStackItems.append(BackStackScreen.Item(
                screen: takeTurnsScreen,
                barVisibility: .visible(BackStackScreen.BarContent(
                    leftItem: BackStackScreen.BarContent.BarButtonItem.button(BackStackScreen.BarContent.Button(
                        content: .text("Quit"),
                        handler: {
                            sink.send(.confirmQuit)
                        }))))))
            
        case .maybeQuit:
            
            let takeTurnsScreen = TakeTurnsWorkflow(
                playerX: state.playerX,
                playerO: state.playerO)
                .rendered(with: context)
            backStackItems.append(BackStackScreen.Item(
                screen: takeTurnsScreen,
                barVisibility: .visible(BackStackScreen.BarContent(
                    leftItem: BackStackScreen.BarContent.BarButtonItem.button(BackStackScreen.BarContent.Button(
                        content: .text("Quit"),
                        handler: {
                            sink.send(.confirmQuit)
                        }))))))
            
            let confirmQuitScreen = ConfirmQuitWorkflow()
                .mapOutput( { output -> Action in
                    switch output {
                    case .cancel:
                        return .startGame
                    case .confirm:
                        return .back
                    }
                })
                .rendered(with: context)
            modals.append(ModalContainerScreen.Modal(screen: AnyScreen(confirmQuitScreen), style: .sheet, key: "0", animated: true))
        }
        
        let modalContainerScreen = ModalContainerScreen(baseScreen: AnyScreen(BackStackScreen(items: backStackItems)), modals: modals)

        return modalContainerScreen // this is the base screen. Render all of the pieces of the backstack.
    }

    private func newGameScreen(sink: Sink<Action>, playerX: String, playerO: String) -> NewGameScreen {
        return NewGameScreen(
            playerX: playerX,
            playerO: playerO,
            eventHandler: { event in
                switch event {
                case .startGame:
                    sink.send(.startGame)

                case .playerXChanged(let name):
                    sink.send(.updatePlayerX(name))

                case .playerOChanged(let name):
                    sink.send(.updatePlayerO(name))
                }
            })
    }
}
