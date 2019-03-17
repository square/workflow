import Workflow


struct GameWorkflow: Workflow {

    typealias State = GameState

    typealias Output = Never

    typealias Rendering = GameViewModel

    func makeInitialState() -> State {
        return GameState()
    }

    func workflowDidChange(from previousWorkflow: GameWorkflow, state: inout State) {

    }

    func compose(state: State, context: WorkflowContext<GameWorkflow>) -> GameViewModel {

        let sink = context.makeSink(of: GameAction.self)

        return GameViewModel(
            gameState: state,
            sink: sink)
    }
}

