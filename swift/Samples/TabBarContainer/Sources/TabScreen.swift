/*
* Copyright 2019 Square Inc.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/
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
