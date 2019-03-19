import Workflow
import WorkflowUI

struct EmojiScreen: Screen {
    var gameState: GameState
    var sink: Sink<Action>
}
