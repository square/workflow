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

/// ViewEnvironment acts as a container for values to flow down the view-side
/// of a rendering tree (as opposed to being passed down through Workflows).
///
/// This will often be used by containers to let their children know in what
/// context they’re appearing (for example, a split screen container may set
/// the environment of its two children according to which position they’re
/// appearing in).
public struct ViewEnvironment {
    /// An empty view environment. This should only be used when setting up a
    /// root workflow into a root ContainerViewController or when writing tests.
    /// In other scenarios, containers should pass down the ViewEnvironment
    /// value they get from above.
    public static let empty: ViewEnvironment = ViewEnvironment()

    /// Storage of [K.Type: K.Value] where K: ViewEnvironmentKey
    private var storage: [ObjectIdentifier: Any]

    /// Private empty initializer to make the `empty` environment explicit.
    private init() {
        self.storage = [:]
    }

    /// Get or set for the given ViewEnvironmentKey.
    ///
    /// This will typically only be used by the module that provides the
    /// environment value. See documentation for ViewEnvironmentKey for a
    /// usage example.
    public subscript<Key>(key: Key.Type) -> Key.Value where Key: ViewEnvironmentKey {
        get {
            if let value = storage[ObjectIdentifier(key)] as? Key.Value {
                return value
            } else {
                return Key.defaultValue
            }
        }

        set {
            storage[ObjectIdentifier(key)] = newValue
        }
    }

    /// Returns a new ViewEnvironment with the given value set for the given
    /// environment key.
    ///
    /// This is provided as a convenience for modifying the environment while
    /// passing it down to children screens without the need for an intermediate
    /// mutable value. It is functionally equivalent to the subscript setter.
    public func setting<Key>(key: Key.Type, to value: Key.Value) -> ViewEnvironment where Key: ViewEnvironmentKey {
        var newEnvironment = self
        newEnvironment[key] = value
        return newEnvironment
    }

    /// Returns a new ViewEnvironment with the given value set for the given
    /// key path.
    ///
    /// This is provided as a convenience for modifying the environment while
    /// passing it down to children screens.
    ///
    /// The following are functionally equivalent:
    /// ```
    /// var newEnvironment = environment
    /// newEnvironment.someProperty = 42
    /// ```
    /// and
    /// ```
    /// let newEnvironment = environment.setting(\.someProperty, to: 42)
    /// ```
    ///
    ///
    public func setting<Value>(keyPath: WritableKeyPath<ViewEnvironment, Value>, to value: Value) -> ViewEnvironment {
        var newEnvironment = self
        newEnvironment[keyPath: keyPath] = value
        return newEnvironment
    }
}
