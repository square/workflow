//
//  TabScreen.swift
//  TabBarContainer
//
//  Created by Dhaval Shreyas on 4/22/20.
//

import Foundation
import WorkflowUI

public struct TabScreen {
    public let barItem: BarItem
    public let content: AnyScreen
    public let onSelect: (() -> ())

    public init<Content: Screen>(
        barItem: BarItem,
        content: Content,
        onSelect: @escaping (() -> ())
    ) {
        self.barItem = barItem
        self.content = AnyScreen(content)
        self.onSelect = onSelect
    }
}

public extension Screen {
    func tabScreen(barItem: BarItem, onSelect: @escaping (() -> ())) -> TabScreen {
        .init(barItem: barItem, content: self, onSelect: onSelect)
    }
}
