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

/// Sink is a type that receives incoming values (commonly events or `WorkflowAction`)
///
/// Use `RenderContext.makeSink` to create instances.
public struct Sink<Value> {
    private let onValue: (Value) -> Void

    /// Initializes a new sink with the given closure.
    public init(_ onValue: @escaping (Value) -> Void) {
        self.onValue = onValue
    }

    /// Sends a new event into the sink.
    ///
    /// - Parameter event: The value to send into the sink.
    public func send(_ value: Value) {
        onValue(value)
    }

    /// Generates a new sink of type NewValue.
    ///
    /// Given a `transform` closure, the following code is functionally equivalent:
    ///
    /// ```
    /// sink.send(transform(value))
    /// ```
    /// ```
    /// sink.contraMap(transform).send(value)
    /// ```
    ///
    /// **Trivia**: Why is this called `contraMap`?
    /// - `map` turns `Type<T>` into `Type<U>` via `(T)->U`.
    /// - `contraMap` turns `Type<T>` into `Type<U>` via `(U)->T`
    ///
    /// Another way to think about this is: `map` transforms a type by changing the
    /// output types of its API, while `contraMap` transforms a type by changing the
    /// *input* types of its API.
    ///
    /// - Parameter transform: An escaping closure that transforms `T` into `Event`.
    public func contraMap<NewValue>(_ transform: @escaping (NewValue) -> Value) -> Sink<NewValue> {
        return Sink<NewValue> { value in
            self.send(transform(value))
        }
    }
}
