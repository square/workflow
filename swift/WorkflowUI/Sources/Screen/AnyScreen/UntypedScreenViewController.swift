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

#if os(iOS)

// Internal API for working with a screen view controller when the specific screen type is unknown.
protocol UntypedScreenViewController {
    var screenType: Screen.Type { get }
    func update(untypedScreen: Screen)
}

extension ScreenViewController: UntypedScreenViewController {

    // `var screenType: Screen.Type` is already present in ScreenViewController

    func update(untypedScreen: Screen) {
        guard let typedScreen = untypedScreen as? ScreenType else {
            fatalError("Screen type mismatch: \(self) expected to receive a screen of type \(ScreenType.self), but instead received a screen of type \(type(of: screen))")
        }
        update(screen: typedScreen)
    }

}

#endif
