//  
//  ConfirmQuitScreen.swift
//  Development - SampleTicTacToe
//
//  Created by Astha Trivedi on 3/26/20.
//

import Workflow
import WorkflowUI


struct ConfirmQuitScreen: Screen {
    
    let question: String
    var onQuitTapped: () -> Void = {}
    var onCancelTapped: () -> Void = {}

    func viewControllerDescription(environment: ViewEnvironment) -> ViewControllerDescription {
        return ConfirmQuitViewController.description(for: self, environment: environment)
    }
}


final class ConfirmQuitViewController: ScreenViewController<ConfirmQuitScreen> {
    
    private let questionLabel: UILabel = UILabel(frame: .zero)
    private let confirmButton: UIButton = UIButton(frame: .zero)
    private let cancelButton: UIButton = UIButton(frame: .zero)
    private var onQuitTapped: () -> Void = {}
    private var onCancelTapped: () -> Void = {}

    required init(screen: ConfirmQuitScreen, environment: ViewEnvironment) {
        super.init(screen: screen, environment: environment)
        update(with: screen, environment: environment)
    }

    override func screenDidChange(from previousScreen: ConfirmQuitScreen, previousEnvironment: ViewEnvironment) {
        update(with: screen, environment: environment)
    }

    private func update(with screen: ConfirmQuitScreen, environment: ViewEnvironment) {
        /// Update UI
        questionLabel.text = screen.question
        onQuitTapped = screen.onQuitTapped
        onCancelTapped = screen.onCancelTapped
    }
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
        view.backgroundColor = .white

        questionLabel.textAlignment = .center
        
        confirmButton.backgroundColor = UIColor(red: 41/255, green: 150/255, blue: 204/255, alpha: 1.0)
        confirmButton.setTitle("Yes, quit the game", for: .normal)
        confirmButton.addTarget(self, action: #selector(quitButtonTapped(sender:)), for: .touchUpInside)
        
        cancelButton.backgroundColor = UIColor(red: 41/255, green: 150/255, blue: 204/255, alpha: 1.0)
        cancelButton.setTitle("Go back", for: .normal)
        cancelButton.addTarget(self, action: #selector(cancelButtonTapped(sender:)), for: .touchUpInside)
        
        view.addSubview(questionLabel)
        view.addSubview(confirmButton)
        view.addSubview(cancelButton)
    }
    
    override func viewDidLayoutSubviews() {
    super.viewDidLayoutSubviews()

        let inset: CGFloat = 12.0
        let height: CGFloat = 44.0
        let buttonHeight: CGFloat = 50.0
        var yOffset = view.bounds.origin.y + view.bounds.size.height/4

        questionLabel.frame = CGRect(
        x: view.bounds.origin.x,
        y: yOffset,
        width: view.bounds.size.width,
        height: height)
        
        yOffset += height + inset*2
        
        confirmButton.frame = CGRect(
        x: view.bounds.origin.x,
        y: yOffset,
        width: view.bounds.size.width,
        height: buttonHeight)
            .insetBy(dx: inset, dy: 0.0)
        
        yOffset += height + inset*2
        
        cancelButton.frame = CGRect(
        x: view.bounds.origin.x,
        y: yOffset,
        width: view.bounds.size.width,
        height: buttonHeight)
            .insetBy(dx: inset, dy: 0.0)
    }
    
    @objc private func cancelButtonTapped(sender: UIButton) {
        onCancelTapped()
    }
    
    @objc private func quitButtonTapped(sender: UIButton) {
        onQuitTapped()
    }

}
