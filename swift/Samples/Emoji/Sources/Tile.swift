 enum Tile {
    case empty
    case grass
    case rock
    case monster
    case cash

    private static let randomDistribution: [Tile] = [
            Array(repeating: Tile.empty, count: 20),
            Array(repeating: Tile.grass, count: 4),
            Array(repeating: Tile.rock, count: 4),
            Array(repeating: Tile.monster, count: 1),
            Array(repeating: Tile.cash, count: 1)
        ].flatMap { $0 }

    static var weightedRandom: Tile {
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
