struct Map {

    static let width = 10
    static let height = 10

    private (set) var tiles: [Tile]

    private init(tiles: [Tile]) {
        self.tiles = tiles
    }

    static var random: Map {
        var tiles: [Tile] = []

        let totalTileCount = Map.width * Map.height

        for _ in 0..<totalTileCount {
            tiles.append(Tile.weightedRandom)
        }

        tiles[0] = .empty

        return Map(tiles: tiles)
    }

    var allPositions: [Position] {
        var positions: [Position] = []
        for x in 0..<Map.width {
            for y in 0..<Map.height {
                positions.append(Position(x: x, y: y))
            }
        }
        return positions
    }

    var text: String {
        var text = ""
        for row in 0..<Map.height {
            let offset = row * Map.width
            let tileStrings = tiles[offset..<offset+Map.width].map { $0.text }
            text += tileStrings.reduce("", +)
            text += "\n"
        }
        return text
    }

    func tile(at position: Position) -> Tile {
        let offset = (position.y * Map.width) + position.x
        return tiles[offset]
    }

    mutating func clearTile(at position: Position) {
        let offset = (position.y * Map.width) + position.x
        tiles[offset] = .empty
    }

    func contains(position: Position) -> Bool {
        return (0..<Map.width).contains(position.x) && (0..<Map.height).contains(position.y)
    }

}
