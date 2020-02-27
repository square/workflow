//
//  TestWorkflow.swift
//  Development-SampleTicTacToe
//
//  Created by Ben Cochran on 2/27/20.
//

import Workflow
import WorkflowUI
import BackStackContainer

struct TestWorkflow: Workflow {
    var reductionMode: ReductionMode
    var primaryOptions: [(UIColor, String)] = [
        (.red, "red"),
        (.orange, "orange"),
        (.yellow, "yellow")
    ]
    var secondaryOptions: [(UIColor, String)] = [
        (.green, "green"),
        (.cyan, "cyan"),
        (.blue, "blue"),
        (.purple, "purple")
    ]
}

extension TestWorkflow {
    typealias Output = Never
}

extension TestWorkflow {

    func makeInitialState() -> State {
        return State(
            primary: [(.red, "Red")],
            secondary: []
        )
    }

    func workflowDidChange(from previousWorkflow: TestWorkflow, state: inout State) {
    }

}

extension TestWorkflow {
    struct State {
        var primary: [(UIColor, String)]
        var secondary: [(UIColor, String)]
    }
}

extension TestWorkflow {

    typealias Rendering = SplitScreenContainerScreen<BackStackScreen, BackStackScreen, BackStackScreen>

    func render(state: State, context: RenderContext<TestWorkflow>) -> Rendering {
        let addPrimarySink = context
            .makeSink(of: AnyWorkflowAction.self)
            .contraMap { [primaryOptions] () in
                AnyWorkflowAction<TestWorkflow> { state in
                    if let color = primaryOptions.dropFirst(state.primary.count).first {
                        state.primary.append(color)
                    }
                    return nil
                }
            }
        let removePrimarySink = context
            .makeSink(of: AnyWorkflowAction.self)
            .contraMap { () in
                AnyWorkflowAction<TestWorkflow> { state in
                    state.primary.removeLast()
                    return nil
                }
            }

        let addSecondarySink = context
            .makeSink(of: AnyWorkflowAction.self)
            .contraMap { [secondaryOptions] () in
                AnyWorkflowAction<TestWorkflow> { state in
                    if let color = secondaryOptions.dropFirst(state.secondary.count).first {
                        state.secondary.append(color)
                    }
                    return nil
                }
            }

        let removeSecondarySink = context
            .makeSink(of: AnyWorkflowAction.self)
            .contraMap {  () in
                AnyWorkflowAction<TestWorkflow> { state in
                    state.secondary.removeLast()
                    return nil
                }
            }

        let secondary: BackStackScreen?
        if state.secondary.count > 0 {
            secondary = BackStackScreen(
                items: state.secondary.enumerated().map { arg -> BackStackScreen.Item in
                    let (index, (color, name)) = arg
                    return BackStackScreen.Item(
                        key: "secondary-\(index)-\(name)",
                        screen: SomeScreen(color: color, name: name),
                        barContent: BackStackScreen.BarContent(
                            title: "Secondary \(index) \(name)",
                            leftItem: index > 0
                                ? .button(BackStackScreen.BarContent.Button(
                                    content: .text("Pop"),
                                    handler: { removeSecondarySink.send(()) }
                                ))
                                : .none,
                            rightItem: .button(BackStackScreen.BarContent.Button(
                                content: .text("Push Next"),
                                handler: { addSecondarySink.send(()) }
                            ))
                        )
                    )
                }
            )
        } else {
            secondary = nil
        }

        return SplitScreenContainerScreen(
            primary: BackStackScreen(
                items: state.primary.enumerated().map { arg -> BackStackScreen.Item in
                    let (index, (color, name)) = arg
                    return BackStackScreen.Item(
                        key: "primary-\(index)-\(name)",
                        screen: SomeScreen(color: color, name: name),
                        barContent: BackStackScreen.BarContent(
                            title: "Primary \(index) \(name)",
                            leftItem: index > 0
                                ? .button(BackStackScreen.BarContent.Button(
                                    content: .text("Pop"),
                                    handler: { removePrimarySink.send(()) }
                                ))
                                : .none,
                            rightItem: state.primary.count == primaryOptions.count
                                ? .button(BackStackScreen.BarContent.Button(
                                    content: .text("Push Secondary"),
                                    handler: { addSecondarySink.send(()) }
                                ))
                                : .button(BackStackScreen.BarContent.Button(
                                    content: .text("Push Primary"),
                                    handler: { addPrimarySink.send(()) }
                                ))
                        )
                    )
                }
            ),
            secondary: secondary,
            backFromSecondaryBarButtonItem: .button(BackStackScreen.BarContent.Button(
                content: .text("Collapsed Pop"),
                handler: { removeSecondarySink.send(()) }
            )),
            showOverlayButtonContent: .text("Overlay"),
            preferredReductionMode: reductionMode,
            onNeedSecondary: {
                addSecondarySink.send(())
            },
            ratio: .specific(0.5)
        )
    }

}


struct SomeScreen: Screen {
    var color: UIColor
    var name: String

    func viewControllerDescription(environment: ViewEnvironment) -> ViewControllerDescription {
        return ViewControllerDescription(
            build: { () -> UIViewController in
                let vc = UIViewController()
                let label = UILabel()
                vc.view.addSubview(label)
                label.translatesAutoresizingMaskIntoConstraints = false
                NSLayoutConstraint.activate([
                    label.centerYAnchor.constraint(equalTo: vc.view.centerYAnchor),
                    label.centerXAnchor.constraint(equalTo: vc.view.centerXAnchor),
                ])
                return vc
            },
            update: {
                ($0.view.subviews[0] as! UILabel).text = self.name
                $0.view.backgroundColor = self.color
            }
        )
    }
}
