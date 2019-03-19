import Workflow

enum Action: Equatable, WorkflowAction {

    case move(Direction)
    case reset

    typealias WorkflowType = EmojiWorkflow

    func apply(toState state: inout GameState) -> Never? {

        switch self {
        case .move(let directon):
            guard state.playerState == .alive else { return nil }
            if state.canMove(direction: directon) {
                state.playerPosition = state.playerPosition.moved(in: directon)
                switch state.map.tile(at: state.playerPosition) {
                case .empty, .grass, .rock: break
                case .monster:
                    state.playerState = .dead
                case .cash:
                    state.playerMoney += 1
                    state.map.clearTile(at: state.playerPosition)
                }
            }
        case .reset:
            state = GameState()
        }
        return nil
    }
}
