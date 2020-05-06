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

/// A key into the ViewEnvironment.
///
/// Environment keys are associated with a specific type of value (`Value`) and
/// must declare a default value.
///
/// Typically the key conforming to `ViewEnvironmentKey` will be private, and
/// you are encouraged to provide a convenience accessor on `ViewEnvironment`
/// as in the following example:
///
/// ```
/// private enum ThemeKey: ViewEnvironmentKey {
///     typealias Value = Theme
///     var defaultValue: Theme
/// }
///
/// extension ViewEnvironment {
///     public var theme: Theme {
///         get { self[ThemeKey.self] }
///         set { self[ThemeKey.self] = newValue }
///     }
/// }
/// ```
public protocol ViewEnvironmentKey {
    associatedtype Value

    static var defaultValue: Value { get }
}
