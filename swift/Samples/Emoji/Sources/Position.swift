struct Position: Equatable {
    var x: Int
    var y: Int

    func moved(in direction: Direction) -> Position {
        switch direction {
        case .up:
            return Position(x: x, y: y-1)
        case .left:
            return Position(x: x-1, y: y)
        case .down:
            return Position(x: x, y: y+1)
        case .right:
            return Position(x: x+1, y: y)
        }
    }
}
