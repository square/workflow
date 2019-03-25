//  ___FILEHEADER___

import Workflow
import WorkflowUI


struct ___VARIABLE_productName___Screen: Screen {
    // This should contain all data to display in the UI

    // It should also contain callbacks for any UI events, for example:
    // var onButtonTapped: () -> Void
}


final class ___VARIABLE_productName___ViewController: ScreenViewController<___VARIABLE_productName___Screen> {

    required init(screen: ___VARIABLE_productName___Screen, viewRegistry: ViewRegistry) {
        super.init(screen: screen, viewRegistry: viewRegistry)
        update(with: screen)
    }

    override func screenDidChange(from previousScreen: ___VARIABLE_productName___Screen) {
        update(with: screen)
    }

    private func update(with screen: ___VARIABLE_productName___Screen) {
        /// Update UI
    }

}


extension ViewRegistry {

    public mutating func register___VARIABLE_productName___Screen() {
        self.register(screenViewControllerType: ___VARIABLE_productName___ViewController.self)
    }

}
