//
//  File.swift
//  TicTacToe
//
//  Created by Ben Cochran on 1/30/20.
//  Copyright © 2020 Square. All rights reserved.
//

import WorkflowUI

private enum PaddingKey: ContainerHintKey {
    static let defaultValue: CGFloat = 16
}

extension ViewEnvironment {

    var padding: CGFloat {
        get { self[PaddingKey.self] }
        set { self[PaddingKey.self] = newValue }
    }

}


struct PaddingScreen<Content> {

    var content: Content

    init(_ content: Content) {
        self.content = content
    }

}

extension PaddingScreen: Screen where Content: Screen {

    var viewControllerDescription: ViewControllerDescription {
        return ViewControllerDescription(
            build: { PaddingViewController(content: self.content, environment: $0) },
            update: { $0.update(content: self.content, environment: $1) })
    }

}

private final class PaddingViewController<Content: Screen>: UIViewController {

    private var environment: ViewEnvironment
    private var content: Content
    private var padding: CGFloat = 16
    private var slider: UISlider = UISlider()
    private var contentViewController: DescribedViewController

    init(content: Content, environment: ViewEnvironment) {
        self.environment = environment
        self.environment.padding = padding
        self.content = content
        contentViewController = DescribedViewController(
            screen: content,
            environment: self.environment)
        super.init(nibName: nil, bundle: nil)
    }

    required init?(coder: NSCoder) {
        fatalError("init(coder:) has not been implemented")
    }

    func update(content: Content, environment: ViewEnvironment) {
        self.environment = environment
        self.environment.padding = padding
        self.content = content
        contentViewController.update(
            screen: content,
            environment: self.environment)
    }

    @objc func sliderDidChange() {
        padding = CGFloat(slider.value)
        update(content: content, environment: environment)
    }

    override func viewDidLoad() {
        super.viewDidLoad()

        view.backgroundColor = .white

        addChild(contentViewController)
        view.addSubview(contentViewController.view)
        contentViewController.didMove(toParent: self)

        slider.minimumValue = 0
        slider.maximumValue = 64
        slider.value = Float(padding)
        slider.addTarget(self, action: #selector(sliderDidChange), for: .valueChanged)
        view.addSubview(slider)

        contentViewController.view.translatesAutoresizingMaskIntoConstraints = false
        slider.translatesAutoresizingMaskIntoConstraints = false
        view.addConstraints([
            contentViewController.view.topAnchor.constraint(equalTo: view.topAnchor),
            contentViewController.view.leftAnchor.constraint(equalTo: view.leftAnchor),
            contentViewController.view.rightAnchor.constraint(equalTo: view.rightAnchor),
            slider.topAnchor.constraint(equalTo: contentViewController.view.bottomAnchor, constant: 18),
            slider.leftAnchor.constraint(equalTo: view.layoutMarginsGuide.leftAnchor),
            slider.rightAnchor.constraint(equalTo: view.layoutMarginsGuide.rightAnchor),
            slider.bottomAnchor.constraint(equalTo: view.layoutMarginsGuide.bottomAnchor),
        ])
    }

}
