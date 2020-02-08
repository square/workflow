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


/// A `VerticalStackContainerScreen` displays three screens in a vertical stack with a separator in between.
public struct VerticalStackContainerScreen<TopScreenType: Screen, MiddleScreenType: Screen, BottomScreenType: Screen>: Screen {

    /// The screen displayed in the top of the container.
    public let topScreen: TopScreenType

    /// The screen displayed in the middle of the container.
    public let middleScreen: MiddleScreenType

    /// The screen displayed in the middle of the container.
    public let bottomScreen: BottomScreenType
    
    /// The color of the separator views
    public let separatorColor: UIColor

    /// The height of the separator views
    public let separatorHeight: CGFloat

    public init(
        topScreen: TopScreenType,
        middleScreen: MiddleScreenType,
        bottomScreen: BottomScreenType,
        separatorColor: UIColor = .black,
        separatorHeight: CGFloat = 1.0
    ) {
        self.topScreen = topScreen
        self.middleScreen = middleScreen
        self.bottomScreen = bottomScreen
        self.separatorColor = separatorColor
        self.separatorHeight = separatorHeight
    }

}
