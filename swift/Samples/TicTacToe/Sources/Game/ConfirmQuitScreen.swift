//  
//  ConfirmQuitScreen.swift
//  Development - SampleTicTacToe
//
//  Created by Astha Trivedi on 3/26/20.
//

import Workflow
import WorkflowUI


struct ConfirmQuitScreen: Screen {
    
    let Question: String
    // This should contain all data to display in the UI

    // It should also contain callbacks for any UI events, for example:
    // var onButtonTapped: () -> Void

    func viewControllerDescription(environment: ViewEnvironment) -> ViewControllerDescription {
        return ConfirmQuitViewController.description(for: self, environment: environment)
    }
}


final class ConfirmQuitViewController: ScreenViewController<ConfirmQuitScreen> {

    required init(screen: ConfirmQuitScreen, environment: ViewEnvironment) {
        super.init(screen: screen, environment: environment)
        update(with: screen, environment: environment)
    }

    override func screenDidChange(from previousScreen: ConfirmQuitScreen, previousEnvironment: ViewEnvironment) {
        update(with: screen, environment: environment)
    }

    private func update(with screen: ConfirmQuitScreen, environment: ViewEnvironment) {
        /// Update UI
    }

}
