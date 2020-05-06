//  ___FILEHEADER___

import Workflow
import WorkflowUI

struct ___VARIABLE_productName___Screen: Screen {
    // This should contain all data to display in the UI

    // It should also contain callbacks for any UI events, for example:
    // var onButtonTapped: () -> Void

    func viewControllerDescription(environment: ViewEnvironment) -> ViewControllerDescription {
        return ___VARIABLE_productName___ViewController.description(for: self, environment: environment)
    }
}

final class ___VARIABLE_productName___ViewController: ScreenViewController<___VARIABLE_productName___Screen> {
    required init(screen: ___VARIABLE_productName___Screen, environment: ViewEnvironment) {
        super.init(screen: screen, environment: environment)
        update(with: screen, environment: environment)
    }

    override func screenDidChange(from previousScreen: ___VARIABLE_productName___Screen, previousEnvironment: ViewEnvironment) {
        update(with: screen, environment: environment)
    }

    private func update(with screen: ___VARIABLE_productName___Screen, environment: ViewEnvironment) {
        /// Update UI
    }
}
