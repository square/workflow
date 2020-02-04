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
public struct SplitScreenContainerScreen: Screen {

    /// The screen displayed to the left of the separator.
    public let leftScreen: AnyScreen

    /// The screen displayed to the right of the separator.
    public let rightScreen: AnyScreen

    /// The ratio of `leftScreen`'s width relative to that of `rightScreen`. Defaults to `.third`.
    public let ratio: CGFloat
    
    /// The color of the `separatorView` displayed between `leftScreen`'s and `rightScreen`'s views.
    public let separatorColor: UIColor

    public init<LeftScreenType: Screen, RightScreenType: Screen>(
        leftScreen: LeftScreenType,
        rightScreen: RightScreenType,
        ratio: CGFloat = .third,
        separatorColor: UIColor = .black
    ) {
        self.leftScreen = AnyScreen(leftScreen)
        self.rightScreen = AnyScreen(rightScreen)
        self.ratio = ratio
        self.separatorColor = separatorColor
    }

}

public extension CGFloat {
    static let quarter: CGFloat = 1.0 / 4.0
    static let third: CGFloat = 1.0 / 3.0
    static let half: CGFloat = 1.0 / 2.0
}
