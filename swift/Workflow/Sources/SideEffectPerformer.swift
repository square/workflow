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

import Foundation

public struct SideEffectPerformer<WorkflowType, Action: WorkflowAction>: SideEffect where Action.WorkflowType == WorkflowType {
    let key: AnyHashable
    let action: (Sink<Action>, Lifetime) -> Void

    public init(key: AnyHashable, action: @escaping (Sink<Action>, Lifetime) -> Void) {
        self.key = key
        self.action = action
    }

    public func run(sink: Sink<Action>) -> Lifetime {
        let lifetime = Lifetime {}
        action(sink, lifetime)
        return lifetime
    }

    public static func == (lhs: SideEffectPerformer, rhs: SideEffectPerformer) -> Bool {
        lhs.key == rhs.key
    }

    public func hash(into hasher: inout Hasher) {
        hasher.combine(key)
    }
}
