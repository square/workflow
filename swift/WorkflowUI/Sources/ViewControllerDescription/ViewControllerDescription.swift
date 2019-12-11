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

public struct ViewControllerDescription {
    #if canImport(UIKit)
    public typealias ViewController = UIViewController
    #elseif canImport(AppKit)
    public typealias ViewController = NSViewController
    #endif

    private let _build: () -> ViewController
    private let _update: (ViewController) -> Void

    public init<VC: ViewController>(builder: @escaping () -> VC = { VC() }, updater: @escaping (VC) -> Void) {
        _build = {
            builder()
        }
        _update = { vc in
            guard let vc = vc as? VC else {
                fatalError("Unexpected type while updating screen for \(VC.self)")
            }
            updater(vc)
        }
        viewControllerType = VC.self
    }

    internal let viewControllerType: ViewController.Type

    internal func canUpdate(viewController: ViewController) -> Bool {
        return type(of: viewController) == viewControllerType
    }

    internal func build() -> ViewController {
        return _build()
    }

    internal func update(viewController: ViewController) {
        assert(type(of: viewController) == viewControllerType)
        _update(viewController)
    }
}
