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

import CoreFoundation

/// Swift wrapper for `CFRunLoopObserver`
internal final class RunLoopObserver {
    private let observer: CFRunLoopObserver

    /// Creates a `RunLoopObserver` and adds it to the given run loop for the given run loop modes. See the docs for `CFRunLoopObserverCreateWithHandler` for documentation.
    init?(
        runLoop: CFRunLoop = CFRunLoopGetCurrent(),
        activityStages: CFRunLoopActivity,
        repeats: Bool = true,
        order: CFIndex = 0,
        runLoopModes: CFRunLoopMode = .defaultMode,
        callback: @escaping (_ activityStage: CFRunLoopActivity) -> Void
    ) {
        guard let observer = CFRunLoopObserverCreateWithHandler(
            kCFAllocatorDefault,
            activityStages.rawValue,
            repeats,
            order,
            { _, activityStage in callback(activityStage) }
        ) else { return nil }

        self.observer = observer
        CFRunLoopAddObserver(runLoop, observer, runLoopModes)
    }

    deinit {
        // Clean up the observer
        CFRunLoopObserverInvalidate(observer)
    }
}
