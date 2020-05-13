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

import UIKit
import Workflow
import WorkflowUI

private struct AlertStyleConstants {
    static let viewWidth: CGFloat = 343.0
    static let buttonTitleColor = UIColor(red: 41 / 255, green: 150 / 255, blue: 204 / 255, alpha: 1.0)
    static let titleFont = UIFont.systemFont(ofSize: 18, weight: .medium)
}

internal final class AlertContainerViewController<AlertScreen: Screen>: ScreenViewController<AlertContainerScreen<AlertScreen>> {
    private var baseScreenViewController: DescribedViewController

    private let dimmingView = UIView()

    private var alertView: AlertView?

    required init(screen: AlertContainerScreen<AlertScreen>, environment: ViewEnvironment) {
        self.baseScreenViewController = DescribedViewController(screen: screen.baseScreen, environment: environment)
        super.init(screen: screen, environment: environment)
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        addChild(baseScreenViewController)
        view.addSubview(baseScreenViewController.view)
        baseScreenViewController.didMove(toParent: self)

        dimmingView.backgroundColor = UIColor(white: 0, alpha: 0.5)
        view.addSubview(dimmingView)
    }

    override func viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()

        baseScreenViewController.view.frame = view.bounds

        dimmingView.frame = view.bounds
        dimmingView.isUserInteractionEnabled = (alertView != nil)
        dimmingView.alpha = (alertView != nil) ? 1 : 0
    }

    override func screenDidChange(from previousScreen: AlertContainerScreen<AlertScreen>, previousEnvironment: ViewEnvironment) {
        super.screenDidChange(from: previousScreen, previousEnvironment: previousEnvironment)

        baseScreenViewController.update(screen: screen.baseScreen, environment: environment)

        if let alert = screen.alert {
            if let alertView = alertView {
                alertView.alert = alert
            } else {
                let inAlertView = AlertView(alert: alert)
                inAlertView.backgroundColor = .init(white: 0.95, alpha: 1)
                inAlertView.layer.cornerRadius = 10
                inAlertView.translatesAutoresizingMaskIntoConstraints = false
                alertView = inAlertView
                inAlertView.accessibilityViewIsModal = true
                view.insertSubview(inAlertView, aboveSubview: dimmingView)

                NSLayoutConstraint.activate([
                    inAlertView.centerXAnchor.constraint(equalTo: view.centerXAnchor),
                    inAlertView.centerYAnchor.constraint(equalTo: view.centerYAnchor),
                    inAlertView.heightAnchor.constraint(greaterThanOrEqualToConstant: 0),
                    inAlertView.widthAnchor.constraint(greaterThanOrEqualToConstant: AlertStyleConstants.viewWidth),
                ])

                view.setNeedsLayout()
                view.layoutIfNeeded()

                dimmingView.alpha = 0

                UIView.animate(
                    withDuration: 0.1,
                    delay: 0,
                    options: [
                        .curveEaseInOut,
                        .allowUserInteraction,
                    ],
                    animations: {
                        self.dimmingView.alpha = 1
                        inAlertView.transform = .identity
                        inAlertView.alpha = 1
                    },
                    completion: { _ in
                        UIAccessibility.post(notification: .screenChanged, argument: nil)
                    }
                )
            }
        } else {
            if let alertView = alertView {
                UIView.animate(
                    withDuration: 0.1,
                    delay: 0,
                    options: .curveEaseInOut,
                    animations: {
                        alertView.transform = CGAffineTransform(scaleX: 0.85, y: 0.85)
                        alertView.alpha = 0
                        self.dimmingView.alpha = 0
                    },
                    completion: { _ in
                        alertView.removeFromSuperview()
                        self.view.setNeedsLayout()
                        UIAccessibility.post(notification: .screenChanged, argument: nil)
                    }
                )
                self.alertView = nil
            }
        }
    }

    override var childForStatusBarStyle: UIViewController? {
        return baseScreenViewController
    }

    override var childForStatusBarHidden: UIViewController? {
        return baseScreenViewController
    }

    override var childForHomeIndicatorAutoHidden: UIViewController? {
        return baseScreenViewController
    }

    override var childForScreenEdgesDeferringSystemGestures: UIViewController? {
        return baseScreenViewController
    }

    override public var supportedInterfaceOrientations: UIInterfaceOrientationMask {
        return baseScreenViewController.supportedInterfaceOrientations
    }
}

private final class AlertView: UIView {
    public var alert: Alert?
    private lazy var title: UILabel = {
        let title = UILabel()
        title.font = AlertStyleConstants.titleFont
        title.textAlignment = .center
        title.translatesAutoresizingMaskIntoConstraints = false
        return title
    }()

    private lazy var message: UILabel = {
        let message = UILabel()
        message.font = AlertStyleConstants.titleFont
        message.textAlignment = .center
        message.numberOfLines = 0
        message.lineBreakMode = .byWordWrapping
        message.translatesAutoresizingMaskIntoConstraints = false
        return message
    }()

    public required init(alert: Alert?) {
        self.alert = alert
        super.init(frame: .zero)
        commonInit()
    }

    private func commonInit() {
        guard let alert = alert else {
            return
        }
        title.text = alert.title
        addSubview(title)

        message.text = alert.message
        addSubview(message)

        let buttonStackView = setupButtons(actions: alert.actions)
        addSubview(buttonStackView)

        var constraints: [NSLayoutConstraint] = []

        constraints.append(title.topAnchor.constraint(equalTo: topAnchor, constant: 10))
        constraints.append(title.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -10))
        constraints.append(title.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 10))
        constraints.append(title.heightAnchor.constraint(equalToConstant: 25))

        constraints.append(message.topAnchor.constraint(equalTo: title.bottomAnchor, constant: 10))
        constraints.append(message.trailingAnchor.constraint(equalTo: trailingAnchor, constant: -10))
        constraints.append(message.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 10))

        constraints.append(buttonStackView.topAnchor.constraint(equalTo: message.bottomAnchor, constant: 15))
        constraints.append(buttonStackView.bottomAnchor.constraint(equalTo: bottomAnchor, constant: 0))
        constraints.append(buttonStackView.trailingAnchor.constraint(equalTo: trailingAnchor, constant: 0))
        constraints.append(buttonStackView.leadingAnchor.constraint(equalTo: leadingAnchor, constant: 0))
        constraints.append(buttonStackView.heightAnchor.constraint(greaterThanOrEqualToConstant: 50))

        addConstraints(constraints)
    }

    private func setupButtons(actions: [AlertAction]) -> UIStackView {
        let buttonStackView = UIStackView()
        buttonStackView.axis = actions.count == 2 ? .horizontal : .vertical
        buttonStackView.distribution = .fillEqually
        buttonStackView.alignment = .fill
        buttonStackView.translatesAutoresizingMaskIntoConstraints = false

        for action in actions {
            let alertButton = AlertButton(action: action)
            alertButton.backgroundColor = backgroundColor
            alertButton.layer.borderColor = UIColor.gray.cgColor
            alertButton.layer.borderWidth = 0.2
            alertButton.translatesAutoresizingMaskIntoConstraints = false

            buttonStackView.addArrangedSubview(alertButton)
        }

        return buttonStackView
    }

    @available(*, unavailable)
    public required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }
}

private final class AlertButton: UIButton {
    private var action: AlertAction

    required init(action: AlertAction) {
        self.action = action
        super.init(frame: .zero)
        commonInit()
    }

    private func commonInit() {
        setTitle(action.title, for: .normal)

        switch action.style {
        case .default, .cancel:
            setTitleColor(AlertStyleConstants.buttonTitleColor, for: .normal)
        case .destructive:
            setTitleColor(.systemRed, for: .normal)
        }

        addTarget(self, action: #selector(triggerActionHandler), for: .touchUpInside)
    }

    required init?(coder aDecoder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    @objc func triggerActionHandler() {
        action.handler()
    }
}
