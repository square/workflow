 enum GameTile {
    case empty
    case grass
    case rock
    case monster
    case cash

    private static let randomDistribution: [GameTile] = [
            Array(repeating: GameTile.empty, count: 20),
            Array(repeating: GameTile.grass, count: 4),
            Array(repeating: GameTile.rock, count: 4),
            Array(repeating: GameTile.monster, count: 1),
            Array(repeating: GameTile.cash, count: 1)
        ].flatMap { $0 }

    static var weightedRandom: GameTile {
        return randomDistribution.randomElement() ?? .empty
    }

    var text: String {
        switch self {
        case .empty: return " "
        case .grass: return "🌱"
        case .monster: return "🦖"
        case .rock: return "🏔"
        case .cash: return "💰"
        }
    }
 }
