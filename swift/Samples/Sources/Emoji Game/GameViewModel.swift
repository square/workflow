import Workflow

struct GameViewModel {
    var gameState: GameState
    var sink: Sink<GameAction>
}
