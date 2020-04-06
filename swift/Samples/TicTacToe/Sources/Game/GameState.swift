//
//  GameState.swift
//  Development-SampleTicTacToe
//
//  Created by Astha Trivedi on 3/26/20.
//

enum GameState {
    case ongoing(turn: Player)
    case win(Player)
    case tie

    mutating func toggle() {
        switch self {
        case .ongoing(turn: let player):
            switch player {
            case .x:
                self = .ongoing(turn: .o)
            case .o:
                self = .ongoing(turn: .x)
            }
        default:
            break
        }
    }
}
