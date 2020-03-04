/*
* Copyright 2012 Square Inc.
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


/// A wrapper screen that overrides environment values
public struct EnvironmentScreen<Value, Content> {

    private let keyPath: WritableKeyPath<ViewEnvironment, Value>
    private let value: Value

    public let content: Content

    public init(keyPath: WritableKeyPath<ViewEnvironment, Value>, value: Value, content: Content) {
        self.keyPath = keyPath
        self.value = value
        self.content = content
    }

}

#if canImport(UIKit)

extension Screen {

    /// Returns a new screen that will render the contents of this screen, with
    /// the given environment key path set to the given value.
    public func withEnvironment<Value>(_ keyPath: WritableKeyPath<ViewEnvironment, Value>, value: Value) -> EnvironmentScreen<Value, Self> {
        return EnvironmentScreen(keyPath: keyPath, value: value, content: self)
    }

}

extension EnvironmentScreen: Screen where Content: Screen {

    public func viewControllerDescription(environment: ViewEnvironment) -> ViewControllerDescription {
        return content
            .viewControllerDescription(
                environment: environment
                    .setting(keyPath: keyPath, to: value))
    }

}

#endif
