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
import WorkflowUI


/// A `SplitScreenContainerScreen` displays two screens side by side with a separator in between.
public struct SplitScreenContainerScreen<LeftScreenType: Screen, RightScreenType: Screen>: Screen {

    /// The screen displayed to the left of the separator.
    public let leftScreen: LeftScreenType

    /// The screen displayed to the right of the separator.
    public let rightScreen: RightScreenType

    /// The ratio of `leftScreen`'s width relative to that of `rightScreen`. Defaults to `.third`.
    public let ratio: CGFloat
    
    /// The color of the `separatorView` displayed between `leftScreen`'s and `rightScreen`'s views.
    public let separatorColor: UIColor

    /// The width of the `separatorView` displayed between `leftScreen`'s and `rightScreen`'s views.
    public let separatorWidth: CGFloat

    public init(
        leftScreen: LeftScreenType,
        rightScreen: RightScreenType,
        ratio: CGFloat = .third,
        separatorColor: UIColor = .black,
        separatorWidth: CGFloat = 1.0
    ) {
        self.leftScreen = leftScreen
        self.rightScreen = rightScreen
        self.ratio = ratio
        self.separatorColor = separatorColor
        self.separatorWidth = separatorWidth
    }

}

public extension CGFloat {
    static let quarter: CGFloat = 1.0 / 4.0
    static let third: CGFloat = 1.0 / 3.0
    static let half: CGFloat = 1.0 / 2.0
}
