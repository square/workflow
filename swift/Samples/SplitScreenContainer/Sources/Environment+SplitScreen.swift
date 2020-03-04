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

public enum SplitScreenPosition {
    /// Not appearing in a split screen context
    case none

    /// Appearing in the leading position in a split screen
    case leading

    /// Appearing in the trailing position in a split screen
    case trailing
}

extension ViewEnvironment {

    internal(set) public var splitScreenPosition: SplitScreenPosition {
        get { return self[SplitScreenPositionKey.self] }
        set { self[SplitScreenPositionKey.self] = newValue }
    }

}

private enum SplitScreenPositionKey: ViewEnvironmentKey {
    static var defaultValue: SplitScreenPosition = .none
}
