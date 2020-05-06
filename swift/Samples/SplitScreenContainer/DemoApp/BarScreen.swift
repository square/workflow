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
import Workflow
import WorkflowUI

struct BarScreen: Screen {
    let title: String
    let backgroundColors: [UIColor]
    let viewTapped: () -> Void

    func viewControllerDescription(environment: ViewEnvironment) -> ViewControllerDescription {
        return BarScreenViewController.description(for: self, environment: environment)
    }
}

private final class BarScreenViewController: ScreenViewController<BarScreen> {
    private lazy var titleLabel: UILabel = .init()
    private lazy var tapGestureRecognizer: UITapGestureRecognizer = .init()
    private var gradientLayer: CAGradientLayer?

    required init(screen: BarScreen, environment: ViewEnvironment) {
        super.init(screen: screen, environment: environment)

        update(with: screen)
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        tapGestureRecognizer.addTarget(self, action: #selector(viewTapped))
        view.addGestureRecognizer(tapGestureRecognizer)

        titleLabel.translatesAutoresizingMaskIntoConstraints = false
        titleLabel.textAlignment = .center
        titleLabel.textColor = .white
        view.addSubview(titleLabel)

        NSLayoutConstraint.activate([
            titleLabel.centerXAnchor.constraint(equalTo: view.centerXAnchor),
            titleLabel.centerYAnchor.constraint(equalTo: view.centerYAnchor),
        ])

        updateGradient(for: view, colors: screen.backgroundColors)
    }

    override func screenDidChange(from previousScreen: BarScreen, previousEnvironment: ViewEnvironment) {
        update(with: screen)
    }

    private func update(with screen: BarScreen) {
        titleLabel.text = screen.title

        updateGradient(for: view, colors: screen.backgroundColors)
    }

    private func updateGradient(for targetView: UIView, colors: [UIColor]) {
        let newGradientLayer = CAGradientLayer()

        newGradientLayer.frame = targetView.bounds
        newGradientLayer.colors = colors.map { $0.cgColor }

        targetView.layer.insertSublayer(newGradientLayer, at: 0)

        gradientLayer?.removeFromSuperlayer()

        gradientLayer = newGradientLayer
    }

    @objc
    private func viewTapped() {
        screen.viewTapped()
    }
}
