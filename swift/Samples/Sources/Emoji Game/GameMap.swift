struct GameMap {

    static let width = 10
    static let height = 10

    private (set) var tiles: [GameTile]

    private init(tiles: [GameTile]) {
        self.tiles = tiles
    }

    static var random: GameMap {
        var tiles: [GameTile] = []

        let totalTileCount = GameMap.width * GameMap.height

        for _ in 0..<totalTileCount {
            tiles.append(GameTile.weightedRandom)
        }

        return GameMap(tiles: tiles)
    }

    var allPositions: [GamePosition] {
        var positions: [GamePosition] = []
        for x in 0..<GameMap.width {
            for y in 0..<GameMap.height {
                positions.append(GamePosition(x: x, y: y))
            }
        }
        return positions
    }

    var text: String {
        var text = ""
        for row in 0..<GameMap.height {
            let offset = row * GameMap.width
            let tileStrings = tiles[offset..<offset+GameMap.width].map { $0.text }
            text += tileStrings.reduce("", +)
            text += "\n"
        }
        return text
    }

    func tile(at position: GamePosition) -> GameTile {
        let offset = (position.y * GameMap.width) + position.x
        return tiles[offset]
    }

    mutating func clearTile(at position: GamePosition) {
        let offset = (position.y * GameMap.width) + position.x
        tiles[offset] = .empty
    }

    func contains(position: GamePosition) -> Bool {
        return (0..<GameMap.width).contains(position.x) && (0..<GameMap.height).contains(position.y)
    }

}
