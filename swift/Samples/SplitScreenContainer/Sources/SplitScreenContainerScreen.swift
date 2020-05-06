/*
 * Copyright 2020 Square Inc.
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
public struct SplitScreenContainerScreen<LeadingScreenType: Screen, TrailingScreenType: Screen>: Screen {
    /// The screen displayed leading the separator.
    public let leadingScreen: LeadingScreenType

    /// The screen displayed trailing the separator.
    public let trailingScreen: TrailingScreenType

    /// The ratio of `leadingScreen`'s width relative to that of `trailingScreen`. Defaults to `.third`.
    public let ratio: CGFloat

    /// The color of the `separatorView` displayed between `leadingScreen`'s and `trailingScreen`'s views.
    public let separatorColor: UIColor

    /// The width of the `separatorView` displayed between `leadingScreen`'s and `trailingScreen`'s views.
    public let separatorWidth: CGFloat

    public init(
        leadingScreen: LeadingScreenType,
        trailingScreen: TrailingScreenType,
        ratio: CGFloat = .third,
        separatorColor: UIColor = .black,
        separatorWidth: CGFloat = 1.0
    ) {
        self.leadingScreen = leadingScreen
        self.trailingScreen = trailingScreen
        self.ratio = ratio
        self.separatorColor = separatorColor
        self.separatorWidth = separatorWidth
    }

    public func viewControllerDescription(environment: ViewEnvironment) -> ViewControllerDescription {
        return SplitScreenContainerViewController.description(for: self, environment: environment)
    }
}

public extension CGFloat {
    static let quarter: CGFloat = 1.0 / 4.0
    static let third: CGFloat = 1.0 / 3.0
    static let half: CGFloat = 1.0 / 2.0
}
