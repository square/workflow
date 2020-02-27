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

public struct ViewEnvironment {

    public static let empty: ViewEnvironment = ViewEnvironment()

    private var storage: [ObjectIdentifier: Any]

    private init() {
        storage = [:]
    }

    public subscript<Key: ViewEnvironmentKey>(key: Key.Type) -> Key.Value {
        get {
            guard let value = storage[ObjectIdentifier(Key.self)] as? Key.Value else {
                return Key.defaultValue
            }
            return value
        }
        set {
            storage[ObjectIdentifier(Key.self)] = newValue
        }
    }

    public func setting<Value>(_ keyPath: WritableKeyPath<ViewEnvironment, Value>, to value: Value) -> ViewEnvironment {
        var environment = self
        environment[keyPath: keyPath] = value
        return environment
    }

}

public protocol ViewEnvironmentKey {
    associatedtype Value

    static var defaultValue: Value { get }
}
