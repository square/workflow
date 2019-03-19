public struct GameState {

    var map: Map = .random
    var playerPosition: Position = .init(x: 0, y: 0)
    var playerState: PlayerState = .alive
    var playerMoney: Int = 0

    enum PlayerState {
        case alive
        case dead

        var text: String {
            switch self {
            case .alive: return "🏃🏼‍♂️"
            case .dead: return "💀"
            }
        }
    }

    func text(at position: Position) -> String {
        if position == playerPosition {
            return playerState.text
        } else {
            return map.tile(at: position).text
        }
    }

    func canMove(direction: Direction) -> Bool {
        let newPosition = playerPosition.moved(in: direction)
        guard map.contains(position: newPosition) else { return false }
        switch map.tile(at: newPosition) {
        case .empty: return true
        case .grass: return true
        case .monster: return true
        case .cash: return true
        case .rock: return false
        }
    }

}
