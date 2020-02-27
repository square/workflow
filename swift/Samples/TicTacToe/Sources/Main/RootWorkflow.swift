//
//  RootWorkflow.swift
//  Development-SampleTicTacToe
//
//  Created by Ben Cochran on 2/27/20.
//

import Workflow
import WorkflowUI
import BackStackContainer

struct RootWorkflow: Workflow {

}

extension RootWorkflow {
    typealias Output = Never
}

extension RootWorkflow {
    func makeInitialState() -> State {
        return .list
    }

    func workflowDidChange(from previousWorkflow: RootWorkflow, state: inout State) {
    }
}

extension RootWorkflow {
    enum State {
        case list
        case showing(ReductionMode)
    }
}

extension RootWorkflow {
    typealias Rendering = BackStackScreen

    func render(state: State, context: RenderContext<RootWorkflow>) -> Rendering {
        let sink = context
                .makeSink(
                    of: ReductionMode.self,
                    onEvent: { mode, state in
                        state = .showing(mode)
                        return nil
                    }
                )
        let backSink = context
                .makeSink(
                    of: Void.self,
                    onEvent: { _, state in
                        state = .list
                        return nil
                    }
                )
//            .makeSink(of: AnyWorkflowAction.self)
//            .contraMap { (reductionMode: ReductionMode) -> AnyWorkflowAction in
//                return AnyWorkflowAction<RootWorkflow> { state in
//                    state = .showing(reductionMode)
//                    return nil
//                }
//            }
        var stack = BackStackScreen(
            items: [
                BackStackScreen.Item(
                    screen: ListScreen(
                        items: [ReductionMode.automatic, .overlay, .split]
                            .map { mode in
                                ListScreen.Item(
                                    title: mode.displayName,
                                    action: { sink.send(mode) }
                                )
                            }
                    ),
                    barContent: BackStackScreen.BarContent(
                        title: .text("Modes"),
                        leftItem: .none,
                        rightItem: .none
                    )
                )
            ]
        )


        if case .showing(let mode) = state {
            stack.items.append(BackStackScreen.Item(
                screen: TestWorkflow(reductionMode: mode)
                    .rendered(with: context),
                barContent: BackStackScreen.BarContent(
                    title: .text(mode.displayName),
                    leftItem: .button(BackStackScreen.BarContent.Button(
                        content: .text("Back"),
                        handler: { backSink.send(()) }
                    )),
                    rightItem: .none
                )
            ))
        }

        return stack
    }
}

extension ReductionMode {
    var displayName: String {
        switch self {
        case .automatic: return "Automatic"
        case .overlay: return "Overlay"
        case .split: return "Split"
        }
    }
}

struct ListScreen: Screen {
    struct Item {
        var title: String
        var action: () -> Void
    }
    var items: [Item]

    func viewControllerDescription(environment: ViewEnvironment) -> ViewControllerDescription {
        return ViewControllerDescription(
            build: { ListViewController() },
            update: { $0.items = self.items })
    }
}

fileprivate final class ListViewController: UITableViewController {

    var items: [ListScreen.Item] = [] {
        didSet {
            if isViewLoaded {
                tableView.reloadData()
            }
        }
    }

    override func viewDidLoad() {
        super.viewDidLoad()
        tableView.register(UITableViewCell.self, forCellReuseIdentifier: "cell")
    }

    override func tableView(_ tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        return items.count
    }

    override func tableView(_ tableView: UITableView, cellForRowAt indexPath: IndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCell(withIdentifier: "cell", for: indexPath)
        cell.textLabel?.text = items[indexPath.row].title
        cell.accessoryType = .disclosureIndicator
        return cell
    }

    override func tableView(_ tableView: UITableView, didSelectRowAt indexPath: IndexPath) {
        items[indexPath.row].action()
    }

}


