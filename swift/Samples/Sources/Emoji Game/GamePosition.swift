struct GamePosition: Equatable {
    var x: Int
    var y: Int

    func moved(in direction: GameDirection) -> GamePosition {
        switch direction {
        case .up:
            return GamePosition(x: x, y: y-1)
        case .left:
            return GamePosition(x: x-1, y: y)
        case .down:
            return GamePosition(x: x, y: y+1)
        case .right:
            return GamePosition(x: x+1, y: y)
        }
    }
}
