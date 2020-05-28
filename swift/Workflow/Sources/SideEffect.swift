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

public protocol SideEffect {
    associatedtype Output

    func run(sink: Sink<Output>) -> Lifetime

    func isEquivalent(to otherSideEffect: Self) -> Bool
}

struct MappedSideEffect<WrappedSideEffect: SideEffect, Output>: SideEffect {
    let wrapped: WrappedSideEffect
    let mapping: (WrappedSideEffect.Output) -> Output

    func run(sink: Sink<Output>) -> Lifetime {
        let mappingSink = Sink<WrappedSideEffect.Output> {
            sink.send(self.mapping($0))
        }
        return wrapped.run(sink: mappingSink)
    }

    func isEquivalent(to otherSideEffect: MappedSideEffect<WrappedSideEffect, Output>) -> Bool {
        wrapped.isEquivalent(to: otherSideEffect.wrapped)
    }
}

extension SideEffect {
    func map<MapToOutput>(_ mapping: @escaping (Self.Output) -> MapToOutput) -> MappedSideEffect<Self, MapToOutput> {
        MappedSideEffect<Self, MapToOutput>(wrapped: self, mapping: mapping)
    }
}
